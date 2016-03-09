Kafka 0.9 Tests
===============

Group of tools to verify Kafka 0.9 is reliable enough and ready for production.


High-Level Description
----------------------

Basic idea is to verify that every produced message is also consumed by all consumers in a reasonable amount of time.

- `GeneratingProducer`
    - Generate groups per N messages and send them to Kafka.
    - Message key is string with ID of messages group.
    - Message value is integer with order in the current group of messages.
    - Sending of each message is marked in database.
    - Confirmation of each send from Kafka broker is marked in database.
- `AutoCommitConsumer`
    - Consume messages from Kafka and mark them in database.
    - Let consumer client to commit offsets automatically.
- `SeekingConsumer`
    - Consume messages from Kafka and mark them in database.
    - Commit offsets manually after predefined number of messages, handle rebalancing notifications.
    - Skip marking of messages occasionally to simulate e.g. HDFS or Cassandra error and seek back in the queue to the last committed offset.
- `ResultsUpdater`
    - Periodically recompute current state and garbage collect processed stuff in database.
    - Verify that all produced messages were really consumed by all consumers on level of message groups.
    - Increase of counters and print their values.
    - There must be always exactly one instance running.
- `ChaoticManager`
    - Periodically and randomly change previous decisions, start and stop producers and consumers.
    - There are bounds for min. and max. number of running components, frequency of updates and number of decisions per update.


Preconditions and Requirements
------------------------------

### Kafka and ZooKeeper

Install Kafka and ZooKeeper standard way, standalone or as a cluster.

- The topic below is expected to be present in the tests.
- Update replication factor and number of partitions according to your needs.

````sh
ZOOKEEPER=localhost:2181 && cd ~/kafka && bin/kafka-topics.sh --zookeeper $ZOOKEEPER --create --replication-factor 2 --partitions 9 --topic kafka-test
````


### Redis

It is used as a database for tracking flow of messages.

- Confirmation that each produced message is also consumed.
- Storage of results.

Installation (in Debian)

````sh
apt-get install redis-server redis-tools
````


Compilation
-----------

````sh
mvn package
````


Execution
---------

Prefer to use shell scripts present in the top level project directory or go deeper to understand how the tools exactly work.

### Start

- Make sure all data are consumed from Kafka by all consumers.
    - Committed offsets should be at the latest positions.
    - Start consumers and stop them after a while if you are unsure.
- Reset state stored in database.
- Update configuration according to your needs.
- Start `ResultsUpdater` always in one instance.
- Start one or more instances of `AutoCommitConsumer` and `SeekingConsumer`.
    - Note there may be multiple consumers/threads inside based on `Configuration`.
- Start one or more instances of `GeneratorProducer`


#### Long term stability test

The following test uses periodical pseudo random starting and stopping of producers and consumers with consumers rebalancing.

````sh
# On all nodes
./erase_all_data.sh
````

````sh
# Single instance
./run_ResultsUpdater.sh &
````

````sh
# On all nodes, possibly multiple instances
./run_AutoCommitConsumer_ChaoticManager.sh &
./run_SeekingConsumer_ChaoticManager.sh &
./run_GeneratorProducer_ChaoticManager.sh &
````

#### Add partitions test

Occasionally increase number of partitions and check logs of producers and consumer to verify they notice the change.

````sh
# On all nodes
./erase_all_data.sh
````

````sh
# Single instance
./run_ResultsUpdater.sh &
````

````sh
# On all nodes, possibly multiple instances
./run_AutoCommitConsumer.sh &
./run_AutoCommitConsumer.sh &
./run_SeekingConsumer.sh &
./run_SeekingConsumer.sh &
./run_GeneratorProducer.sh &
./run_GeneratorProducer.sh &
````

````sh
# Sometimes
date ; /opt/kafka/bin/kafka-topics.sh --zookeeper localhost --topic kafka-test --alter --partitions 42
````


#### Shutdown broker test

- Replication factor configured for a topic must be at least 2.
- Stop and start one of the Kafka brokers.
- Verify data loss of confirmed messages is in expected range (see `acks` parameter of producer)
- Verify assignment of leaders to Kafka brokers.

````sh
# On all nodes
./erase_all_data.sh
````

````sh
# Single instance
./run_ResultsUpdater.sh &
````

````sh
# On all nodes, possibly multiple instances
./run_AutoCommitConsumer.sh &
./run_AutoCommitConsumer.sh &
./run_SeekingConsumer.sh &
./run_SeekingConsumer.sh &
./run_GeneratorProducer.sh &
./run_GeneratorProducer.sh &
````

````sh
# Show assigned leaders and replicas
/opt/kafka/bin/kafka-topics.sh --zookeeper localhost --topic kafka-test --describe
````

````sh
# Stop or crash Kafka broker (multiple choices)
date ; stop kafka
date ; /opt/kafka/bin/kafka-shutdown-broker.sh
date ; /opt/kafka/bin/kafka-server-stop.sh
date ; pkill -9 -f kafka.Kafka
````

````sh
# Rebalance Kafka leaders, http://kafka.apache.org/documentation.html#basic_ops_leader_balancing
# Or auto.leader.rebalance.enable is enabled by default so waiting for a while should be enough
/opt/kafka/bin/kafka-preferred-replica-election.sh --zookeeper localhost
````


### More instances, rebalancing

- Look at state in logs of `ResultsUpdater`.
- Start and stop producers to have higher/lower load of messages.
- Start and stop consumers to test behavior of consumer during rebalancing.
    - Always at least one consumer instance in each group should be running.
- Or use `ChaoticManager` to start and stop them periodically and pseudo randomly.


### Stop

- Shutdown all producers first.
- Let all consumers to consume all messages from Kafka.
- Let `ResultsUpdater` to process all data in database.


### Issues found using this tool

- Too slow processing in consumer times out its session
    - This issue was only rediscovered, it's a direct consequence of new consumer single-thread API.
    - [https://issues.apache.org/jira/browse/KAFKA-2985?focusedCommentId=15105539&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-15105539](https://issues.apache.org/jira/browse/KAFKA-2985?focusedCommentId=15105539&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-15105539)
    - [https://issues.apache.org/jira/browse/KAFKA-2986](https://issues.apache.org/jira/browse/KAFKA-2986)
    - [https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=61333789](https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=61333789)
- [Topic partition is not sometimes consumed after rebalancing of consumer group](https://github.com/avast/kafka-tests/tree/issue1/issues/1_rebalancing)
    - [https://issues.apache.org/jira/browse/KAFKA-2978](https://issues.apache.org/jira/browse/KAFKA-2978)
    - [https://github.com/apache/kafka/pull/666](https://github.com/apache/kafka/pull/666)
