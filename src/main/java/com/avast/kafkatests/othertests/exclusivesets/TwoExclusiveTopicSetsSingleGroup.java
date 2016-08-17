package com.avast.kafkatests.othertests.exclusivesets;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/**
 * NullPointerException in RoundRobinAssignor in case of two exclusive sets of topics consumed by two sets of consumers
 * sharing same group ID. Unfortunately not repeatable using this much smaller code in short time, something must be different.
 * Simple workaround is to use two different group IDs.
 * <p>
 * java.lang.NullPointerException: null
 * at org.apache.kafka.clients.consumer.RoundRobinAssignor.allPartitionsSorted(RoundRobinAssignor.java:69) ~[kafka-clients-0.9.0.1-SNAPSHOT-f232f53.jar:na]
 * at org.apache.kafka.clients.consumer.RoundRobinAssignor.assign(RoundRobinAssignor.java:50) ~[kafka-clients-0.9.0.1-SNAPSHOT-f232f53.jar:na]
 * at org.apache.kafka.clients.consumer.internals.AbstractPartitionAssignor.assign(AbstractPartitionAssignor.java:67) ~[kafka-clients-0.9.0.1-SNAPSHOT-f232f53.jar:na]
 * at org.apache.kafka.clients.consumer.internals.ConsumerCoordinator.performAssignment(ConsumerCoordinator.java:227) ~[kafka-clients-0.9.0.1-SNAPSHOT-f232f53.jar:na]
 * at org.apache.kafka.clients.consumer.internals.AbstractCoordinator.onJoinLeader(AbstractCoordinator.java:393) ~[kafka-clients-0.9.0.1-SNAPSHOT-f232f53.jar:na]
 * at org.apache.kafka.clients.consumer.internals.AbstractCoordinator.access$700(AbstractCoordinator.java:81) ~[kafka-clients-0.9.0.1-SNAPSHOT-f232f53.jar:na]
 * at org.apache.kafka.clients.consumer.internals.AbstractCoordinator$JoinGroupResponseHandler.handle(AbstractCoordinator.java:343) ~[kafka-clients-0.9.0.1-SNAPSHOT-f232f53.jar:na]
 * at org.apache.kafka.clients.consumer.internals.AbstractCoordinator$JoinGroupResponseHandler.handle(AbstractCoordinator.java:324) ~[kafka-clients-0.9.0.1-SNAPSHOT-f232f53.jar:na]
 * at org.apache.kafka.clients.consumer.internals.AbstractCoordinator$CoordinatorResponseHandler.onSuccess(AbstractCoordinator.java:665) ~[kafka-clients-0.9.0.1-SNAPSHOT-f232f53.jar:na]
 * at org.apache.kafka.clients.consumer.internals.AbstractCoordinator$CoordinatorResponseHandler.onSuccess(AbstractCoordinator.java:644) ~[kafka-clients-0.9.0.1-SNAPSHOT-f232f53.jar:na]
 * at org.apache.kafka.clients.consumer.internals.RequestFuture$1.onSuccess(RequestFuture.java:167) ~[kafka-clients-0.9.0.1-SNAPSHOT-f232f53.jar:na]
 * at org.apache.kafka.clients.consumer.internals.RequestFuture.fireSuccess(RequestFuture.java:133) ~[kafka-clients-0.9.0.1-SNAPSHOT-f232f53.jar:na]
 * at org.apache.kafka.clients.consumer.internals.RequestFuture.complete(RequestFuture.java:107) ~[kafka-clients-0.9.0.1-SNAPSHOT-f232f53.jar:na]
 * at org.apache.kafka.clients.consumer.internals.ConsumerNetworkClient$RequestFutureCompletionHandler.onComplete(ConsumerNetworkClient.java:380) ~[kafka-clients-0.9.0.1-SNAPSHOT-f232f53.jar:na]
 * at org.apache.kafka.clients.NetworkClient.poll(NetworkClient.java:274) ~[kafka-clients-0.9.0.1-SNAPSHOT-f232f53.jar:na]
 * at org.apache.kafka.clients.consumer.internals.ConsumerNetworkClient.clientPoll(ConsumerNetworkClient.java:320) ~[kafka-clients-0.9.0.1-SNAPSHOT-f232f53.jar:na]
 * at org.apache.kafka.clients.consumer.internals.ConsumerNetworkClient.poll(ConsumerNetworkClient.java:213) ~[kafka-clients-0.9.0.1-SNAPSHOT-f232f53.jar:na]
 * at org.apache.kafka.clients.consumer.internals.ConsumerNetworkClient.poll(ConsumerNetworkClient.java:193) ~[kafka-clients-0.9.0.1-SNAPSHOT-f232f53.jar:na]
 * at org.apache.kafka.clients.consumer.internals.ConsumerNetworkClient.poll(ConsumerNetworkClient.java:163) ~[kafka-clients-0.9.0.1-SNAPSHOT-f232f53.jar:na]
 * at org.apache.kafka.clients.consumer.internals.AbstractCoordinator.ensureActiveGroup(AbstractCoordinator.java:222) ~[kafka-clients-0.9.0.1-SNAPSHOT-f232f53.jar:na]
 * at org.apache.kafka.clients.consumer.internals.ConsumerCoordinator.ensurePartitionAssignment(ConsumerCoordinator.java:311) ~[kafka-clients-0.9.0.1-SNAPSHOT-f232f53.jar:na]
 * at org.apache.kafka.clients.consumer.KafkaConsumer.pollOnce(KafkaConsumer.java:890) ~[kafka-clients-0.9.0.1-SNAPSHOT-f232f53.jar:na]
 * at org.apache.kafka.clients.consumer.KafkaConsumer.poll(KafkaConsumer.java:853) ~[kafka-clients-0.9.0.1-SNAPSHOT-f232f53.jar:na]
 * at com.avast.utils2.kafka.consumer.ConsumerThread.pollAndProcessMessagesFromKafka(ConsumerThread.java:180) ~[utils-kafka-3.2.32.jar:na]
 * at com.avast.utils2.kafka.consumer.ConsumerThread.run(ConsumerThread.java:149) ~[utils-kafka-3.2.32.jar:na]
 * at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:511) [na:1.8.0_66]
 * at java.util.concurrent.FutureTask.run(FutureTask.java:266) [na:1.8.0_66]
 * at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1142) [na:1.8.0_66]
 * at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617) [na:1.8.0_66]
 * at java.lang.Thread.run(Thread.java:745) [na:1.8.0_66]
 *
 * public List<TopicPartition> allPartitionsSorted(Map<String, Integer> partitionsPerTopic,
 *     Map<String, List<String>> subscriptions) {
 *     SortedSet<String> topics = new TreeSet<>();
 *     for (List<String> subscription : subscriptions.values())
 *         topics.addAll(subscription);
 *
 *         List<TopicPartition> allPartitions = new ArrayList<>();
 *         for (String topic : topics) {
 *             Integer partitions = partitionsPerTopic.get(topic);
 *             for (int partition = 0; partition < partitions; partition++) {    // <<<<<<< partitions is null, code operates on all topics from both sets instead of on the subscribed ones
 *                 allPartitions.add(new TopicPartition(topic, partition));
 *             }
 *         }
 *     return allPartitions;
 * }
 */
/*
bin/zookeeper-server-start.sh config/zookeeper.properties
bin/kafka-server-start.sh config/server.properties

TOPICS="`echo {1..29}`"
for i in ${TOPICS}
do
    bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 3 --topic `printf "test-%03d" ${i}`
done

bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 3 --topic test-x
bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 3 --topic test-y

bin/kafka-console-producer.sh --broker-list localhost:9092 --topic test-001
bin/kafka-console-producer.sh --broker-list localhost:9092 --topic test-002
bin/kafka-console-producer.sh --broker-list localhost:9092 --topic test-003

bin/kafka-console-producer.sh --broker-list localhost:9092 --topic test-x
bin/kafka-console-producer.sh --broker-list localhost:9092 --topic test-y
 */
public class TwoExclusiveTopicSetsSingleGroup implements Runnable, ConsumerRebalanceListener, AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(TwoExclusiveTopicSetsSingleGroup.class);

    private final Logger logger;
    private final Pattern topics;
    private final String groupId;
    private final String clientId;
    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    public TwoExclusiveTopicSetsSingleGroup(Pattern topics, String groupId, String clientId) {
        logger = LoggerFactory.getLogger(getClass().getName() + "." + groupId + "." + clientId);
        this.topics = topics;
        this.groupId = groupId;
        this.clientId = clientId;
    }

    @Override
    public void close() {
        shutdown.set(true);
    }

    @Override
    public void run() {
        Thread.currentThread().setName(groupId + "-" + clientId);
        logger.info("Thread started");

        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.setProperty(ConsumerConfig.CLIENT_ID_CONFIG, "localhost" + groupId + "-" + clientId);
        properties.setProperty(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, "org.apache.kafka.clients.consumer.RoundRobinAssignor");
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        properties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        try (Consumer<String, String> consumer = new KafkaConsumer<>(properties, new StringDeserializer(), new StringDeserializer())) {
            consumer.subscribe(topics, this);

            while (!shutdown.get()) {
                try {
                    ConsumerRecords<String, String> messages = consumer.poll(2000);

                    if (!messages.isEmpty()) {
                        messages.forEach(m -> logger.debug("Incoming messages: {}", m));
                    }
                } catch (Exception e) {
                    logger.error("Unexpected exception while processing messages: {}", e.toString(), e);
                }
            }
        }

        logger.info("Thread finished");
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        logger.info("Partitions revoked: {}", partitions);
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        logger.info("Partitions assigned: {}", partitions);
    }

    public static void main(String args[]) throws InterruptedException {
        LOGGER.info("================= Application started =================");
        ExecutorService executor = Executors.newFixedThreadPool(6);

        List<TwoExclusiveTopicSetsSingleGroup> consumers = new ArrayList<>();

        Pattern setA = Pattern.compile("test-[0-9]{3}");
        Pattern setB = Pattern.compile("test-x|test-y");

        consumers.add(new TwoExclusiveTopicSetsSingleGroup(setA, "test-group-a", "1"));
        consumers.add(new TwoExclusiveTopicSetsSingleGroup(setA, "test-group-a", "2"));
        consumers.add(new TwoExclusiveTopicSetsSingleGroup(setA, "test-group-a", "3"));
        consumers.add(new TwoExclusiveTopicSetsSingleGroup(setA, "test-group-a", "4"));

        consumers.add(new TwoExclusiveTopicSetsSingleGroup(setB, "test-group-b", "1"));
        consumers.add(new TwoExclusiveTopicSetsSingleGroup(setB, "test-group-b", "2"));

        consumers.forEach(executor::submit);

        Thread threadToWait = Thread.currentThread();
        CountDownLatch latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                LOGGER.info("================= Shutdown hook started =================");
                latch.countDown();

                try {
                    threadToWait.join(10000);
                } catch (InterruptedException e) {
                    LOGGER.error("Interrupted: {}", e);
                }
                LOGGER.info("================= Shutdown hook finished =================");
            }
        });
        latch.await();

        consumers.forEach(TwoExclusiveTopicSetsSingleGroup::close);

        executor.shutdown();
        executor.awaitTermination(10000, TimeUnit.MILLISECONDS);

        LOGGER.info("================= Application finished =================");
    }
}
