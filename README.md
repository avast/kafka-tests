Kafka 0.9 Tests
===============

Group of tools to verify Kafka 0.9 is reliable enough and ready for production.


High-Level Description
----------------------

- **Producers** generate groups of N messages and send them to Kafka.
    - Message key is string with ID of messages group.
    - Message value is integer with order in the current group of messages.
    - Sending of each message is marked in database.
    - Confirmation of each send is marked in database.
- **Consumers** read messages from Kafka.
    - Each consumed message is marked in database.
    - There may be multiple consumer types (different Kafka's group ID).
- **Results updater** is executed periodically.
    - Verification that all produced messages are consumed by all consumers on level of message groups.
    - Removal of data required for confirmations.
    - Increase of various counters.
    - Print current state of counters.


Preconditions and Requirements
------------------------------

### Kafka and ZooKeeper

Install Kafka and ZooKeeper standard way, standalone or as a cluster.

- The topic below is expected to be present in the tests.
- Update replication factor and number of partitions according to your needs.

````sh
ZOOKEEPER=localhost:2181 && cd ~/kafka && bin/kafka-topics.sh --zookeeper $ZOOKEEPER --create --replication-factor 1 --partitions 9 --topic kafka-test
````


### Redis

It is used as a database for tracking flow of messages.

- Confirmation that each produced message is also consumed.
- Storage of results.

Installation (in Debian)

````sh
apt-get install redis-server redis-tools
````


Execution
---------

- Make sure all data are consumed from Kafka by all consumers.
    - Offsets should be at the latest positions.
    - Start and stop consumers after a while if you are unsure.
- Reset state stored in database.

````sh
redis-cli

# Verification that no key is present
KEYS *

# Remove all data in database (be careful!), execute before run of each test 
FLUSHALL
````

- Update `Configuration` according to your needs.
- Start `ResultsUpdater` always in one instance.
- Start one or more instances of `Kafka09AutoCommitConsumer` and `Kafka09SeekingConsumer`.
    - Note there may be multiple consumers/threads inside based on `Configuration`.
- Start one or more instances of `Kafka09Producer`

- Look at state in logs of `ResultsUpdater`.

- Start and stop producers to have higher/lower load of messages.
- Start and stop consumers to test behavior of consumer during rebalancing.
    - Always at least one consumer instance in each group should be running.

````sh
# There is a graceful shutdown on SIGTERM
ps aux | egrep 'Consumer|Producer|Updater'
kill PID
````

- Stop of test:
    - Shutdown all producers first.

````sh
kill `pgrep --full 'Kafka09Producer'`
````

    - Let all consumers to consume all messages from Kafka.
    
````sh
kill `pgrep --full 'Kafka09.*Consumer'`
````

    - Let results updater to process all data in database.

````sh
kill `pgrep --full 'ResultsUpdater'`
````
