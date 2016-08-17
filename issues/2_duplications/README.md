Too slow processing in consumer times out its session
=====================================================

Short Description
-----------------

https://issues.apache.org/jira/browse/KAFKA-2985?focusedCommentId=15105539&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-15105539


Scenario and Initial Analysis
-----------------------------

### Prepare environment

- Check out and compile Kafka.

```sh
git pull https://github.com/apache/kafka.git
cd kafka
git checkout 0.9.0

date
# Tue Jan 19 09:29:40 CET 2016
git rev-parse HEAD
# c2b73258454778731a8a27bca2836d25eaf04d06

./gradlew clean
./gradlew -PscalaVersion=2.11 releaseTarGz -x signArchives
# ./core/build/distributions

# Install the JARs locally to ~/.m2/repository/...
mvn install:install-file -Dfile=clients/build/libs/kafka-clients-0.9.0.1-SNAPSHOT.jar -DgroupId=org.apache.kafka -DartifactId=kafka-clients -Dversion=0.9.0.1-SNAPSHOT-`git rev-parse --short HEAD` -Dpackaging=jar
mvn install:install-file -Dfile=clients/build/libs/kafka-clients-0.9.0.1-SNAPSHOT-sources.jar -DgroupId=org.apache.kafka -DartifactId=kafka-clients -Dversion=0.9.0.1-SNAPSHOT-`git rev-parse --short HEAD` -Dclassifier=sources -Dpackaging=jar
mvn install:install-file -Dfile=clients/build/libs/kafka-clients-0.9.0.1-SNAPSHOT-javadoc.jar -DgroupId=org.apache.kafka -DartifactId=kafka-clients -Dversion=0.9.0.1-SNAPSHOT-`git rev-parse --short HEAD` -Dclassifier=javadoc -Dpackaging=jar
mvn install:install-file -Dfile=clients/build/libs/kafka-clients-0.9.0.1-SNAPSHOT-test.jar -DgroupId=org.apache.kafka -DartifactId=kafka-clients -Dversion=0.9.0.1-SNAPSHOT-`git rev-parse --short HEAD` -Dclassifier=test -Dpackaging=jar
```
    
- Run single node with default configuration, just extract the archive and run.

```sh
bin/zookeeper-server-start.sh config/zookeeper.properties
bin/kafka-server-start.sh config/server.properties
```

- Setup environment according to README in the top level directory (create topic, install Redis).

```sh
ZOOKEEPER=localhost:2181 && bin/kafka-topics.sh --zookeeper $ZOOKEEPER --create --replication-factor 1 --partitions 30 --topic kafka-test
apt-get install redis-server redis-tools
```

- Clone 'kafka-tests' project from GitHub and build it.
- Configuration in `config/application.conf`

```sh
kafka-tests {
  redisServer = "localhost"
  kafkaBrokers = "localhost:9092"
  kafkaTopic = "kafka-test"
  shutdownTimeout = "40 seconds"
  messagesPerGroup = 100
  producerInstances = 10
  producerSlowDown = "100 milliseconds"
  consumerInstancesAutoCommit = 1
  consumerInstancesSeeking = 1
  consumerPollTimeout = "1 seconds"
  updateStatePeriod = "5 seconds"
  checksBeforeFailure = 15
  messagesToChangeState = 100
  percentFailureProbability = 40

  chaoticManagerMinComponents = 3
  chaoticManagerMaxComponents = 10
  chaoticManagerDecisionsPerUpdate = 2
  chaoticManagerUpdatePeriod = "3 minutes"
}
```

- Run all parts of the tests.

````sh
./erase_all_data.sh
./run_ResultsUpdater.sh &
./run_AutoCommitConsumer_ChaoticManager.sh &
./run_SeekingConsumer_ChaoticManager.sh &
./run_GeneratorProducer_ChaoticManager.sh &
````

- Periodically check all logs for message about duplication.

````sh
watch -d 'grep -r Duplication logs | head'
````


System, versions
----------------

...if anyone is interested.

- 8 CPUs.
- 16 GB RAM.
- GNU/Linux, Debian Jessie (current stable with few backports).
- Oracle Java 8.
- Kafka 0.9.0, branch 0.9.0, git c2b73258454778731a8a27bca2836d25eaf04d06, Scala 2.11.

````
uname -a
Linux traktor 4.2.0-0.bpo.1-amd64 #1 SMP Debian 4.2.5-1~bpo8+1 (2015-11-02) x86_64 GNU/Linux

java -version
java version "1.8.0_66"
Java(TM) SE Runtime Environment (build 1.8.0_66-b17)
Java HotSpot(TM) 64-Bit Server VM (build 25.66-b17, mixed mode)
````


Conclusion
----------

This issue was only rediscovered, it's a direct consequence of new consumer single-thread API.

https://issues.apache.org/jira/browse/KAFKA-2985?focusedCommentId=15105539&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-15105539
https://issues.apache.org/jira/browse/KAFKA-2986
https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=61333789
