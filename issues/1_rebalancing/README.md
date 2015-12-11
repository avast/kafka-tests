Unsuccessful Rebalancing
========================

Short Description
-----------------

Adding more and more consumers to a consumer group may result in unsuccessful rebalancing. Data from one or more partitions
are not consumed and are not effectively available to the client application (e.g. for 15 minutes). Situation can be resolved
externally by touching the consumer group again (add or remove a consumer) which forces another rebalancing that may or may not be successful.

Significantly higher CPU utilization was observed in such cases (from about 3% to 17%). The CPU utilization takes place in both the affected consumer
thread and Kafka broker according to htop and profiling using jvisualvm.

The issue is not deterministic but it can be easily reproduced after a few minutes just by executing more and more consumers.
More parallelism with multiple CPUs probably gives the issue more chances to appear.


Scenario and Initial Analysis
-----------------------------

### Prepare environment

- Install Kafka and run single node with default configuration.
    - [http://kafka.apache.org/downloads.html](http://kafka.apache.org/downloads.html), kafka_2.11-0.9.0.0.tgz
    - Just extract the archive, no changes in configuration.
    - `bin/zookeeper-server-start.sh config/zookeeper.properties`
    - `bin/kafka-server-start.sh config/server.properties`
- Setup environment according to README in the top level directory (create topic, install Redis).
    - `ZOOKEEPER=localhost:2181 && bin/kafka-topics.sh --zookeeper $ZOOKEEPER --create --replication-factor 1 --partitions 9 --topic kafka-test`
    - `apt-get install redis-server redis-tools`
- Execute all subsystems locally.

- Clone 'kafka-tests' project from GitHub and build it. Run the following scripts:

````sh
./erase_all_data.sh
./run_Kafka09AutoCommitConsumer.sh &
./run_Kafka09SeekingConsumer.sh &
./run_ResultsUpdater.sh &
./run_Kafka09Producer.sh &
````

- Tail logs of ResultsUpdater to detect the issue.

````sh
tail -f logs/ResultsUpdater_*.log
````


### Try to break Kafka or consumer

- Add more and more consumers with about 10 seconds delay until the issue occurs (typically 5 - 10 instances).
    - The issue was reproduced with both consumer types (Kafka09AutoCommitConsumer, Kafka09SeekingConsumer) on a multiprocessor system

````sh
./run_Kafka09AutoCommitConsumer.sh &
````

- Search the following pattern in logs.
    - Number of 'checks' on a `Not yet:` line goes higher than 1 or 2 and continuously increases during each update (when it reaches 15, the counter is abandoned and failure counter is increased)
    - All columns on a `Not yet:` line represent `messagesPerGroup` values (100 in the example) except the one of the affected consumer group.
    - See two examples below for output when everything is OK and output when the issue is present.

````
# OK
2015-12-08 16:51:51.269 DEBUG com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: ====================== Updating state... ====================== (ResultsUpdater.java:52)
2015-12-08 16:51:51.276 DEBUG com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: Not yet: 4618f1b7-a5eb-4d59-aec7-4e2a37dc9e11, 77 send, 77 confirm, 76 consume auto commit, 76 consume seeking, 1 checks (ResultsUpdater.java:75)
2015-12-08 16:51:51.277 DEBUG com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: Not yet: d0af68f1-86a5-42cb-9de5-5bb63080fc1d, 77 send, 77 confirm, 75 consume auto commit, 76 consume seeking, 1 checks (ResultsUpdater.java:75)
2015-12-08 16:51:51.278 DEBUG com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: Not yet: 3b40454d-9bd0-4d9e-9883-ecf034eaf385, 77 send, 77 confirm, 75 consume auto commit, 76 consume seeking, 1 checks (ResultsUpdater.java:75)
2015-12-08 16:51:51.278 DEBUG com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: Not yet: ded1870d-ab05-4169-8346-f4b930ae3652, 77 send, 77 confirm, 72 consume auto commit, 77 consume seeking, 1 checks (ResultsUpdater.java:75)
2015-12-08 16:51:51.278 DEBUG com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: Not yet: cabda754-2477-4a5f-a2c1-662f7b8477f4, 77 send, 77 confirm, 76 consume auto commit, 76 consume seeking, 1 checks (ResultsUpdater.java:75)
2015-12-08 16:51:51.279 DEBUG com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: Not yet: d069df3e-21fb-4723-9283-bf8cd33dc1a4, 77 send, 77 confirm, 75 consume auto commit, 77 consume seeking, 1 checks (ResultsUpdater.java:75)
2015-12-08 16:51:51.279 DEBUG com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: Not yet: f2d0ed6d-cd7e-48f5-84a7-28107daf6ea8, 77 send, 77 confirm, 72 consume auto commit, 77 consume seeking, 1 checks (ResultsUpdater.java:75)
2015-12-08 16:51:51.280 DEBUG com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: Not yet: 61455c0a-ba57-4230-8159-681ebe3837d0, 77 send, 77 confirm, 75 consume auto commit, 77 consume seeking, 1 checks (ResultsUpdater.java:75)
2015-12-08 16:51:51.280 DEBUG com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: Not yet: 3d561677-8074-4f92-bfe3-94f444f6aba7, 77 send, 77 confirm, 75 consume auto commit, 76 consume seeking, 1 checks (ResultsUpdater.java:75)
2015-12-08 16:51:51.281 DEBUG com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: Not yet: 0296c19c-f8ba-4ad1-83bd-5795ccbf4d4c, 77 send, 77 confirm, 75 consume auto commit, 77 consume seeking, 1 checks (ResultsUpdater.java:75)
2015-12-08 16:51:51.282 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 5770                  send requests (ResultsUpdater.java:84)
2015-12-08 16:51:51.283 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 0             send duplications (ResultsUpdater.java:85)
2015-12-08 16:51:51.283 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 0             send bits fail (ResultsUpdater.java:86)
2015-12-08 16:51:51.283 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 5770                  send confirm success (ResultsUpdater.java:88)
2015-12-08 16:51:51.283 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 0             send confirm fail (ResultsUpdater.java:89)
2015-12-08 16:51:51.283 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 0             send confirm duplications (ResultsUpdater.java:90)
2015-12-08 16:51:51.283 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 0             send confirm bits fail (ResultsUpdater.java:91)
2015-12-08 16:51:51.284 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 5746                  consume auto commit (ResultsUpdater.java:93)
2015-12-08 16:51:51.284 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 0             consume auto commit duplications (ResultsUpdater.java:94)
2015-12-08 16:51:51.284 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 0             consume auto commit bits fail (ResultsUpdater.java:95)
2015-12-08 16:51:51.284 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 5765                  consume seeking (ResultsUpdater.java:97)
2015-12-08 16:51:51.284 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 2368                  consume seeking skip (consumed multiple times due to simulated error) (ResultsUpdater.java:98)
2015-12-08 16:51:51.284 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 0             consume seeking duplications (ResultsUpdater.java:99)
2015-12-08 16:51:51.285 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 0             consume seeking bits fail (ResultsUpdater.java:100)
2015-12-08 16:51:51.285 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 5000                  success bits (ResultsUpdater.java:102)
2015-12-08 16:51:51.285 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 0             total not confirmed (ResultsUpdater.java:104)
2015-12-08 16:51:51.285 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 24            total not consumed auto commit (ResultsUpdater.java:105)
2015-12-08 16:51:51.285 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 770           total not success (ResultsUpdater.java:106)
2015-12-08 16:51:51.285 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 5             total not consumed seeking (ResultsUpdater.java:107)
...
# Issue
2015-12-08 16:52:46.421 DEBUG com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: ====================== Updating state... ====================== (ResultsUpdater.java:52)
2015-12-08 16:52:46.425 DEBUG com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: Not yet: dfb5a892-421c-4fed-a7b3-ead3fbeb84c6, 100 send, 100 confirm, 44 consume auto commit, 100 consume seeking, 8 checks (ResultsUpdater.java:75)
2015-12-08 16:52:46.425 DEBUG com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: Not yet: 42f33b63-d6ed-40b7-825f-2ea2e7a3cc67, 100 send, 100 confirm, 0 consume auto commit, 100 consume seeking, 6 checks (ResultsUpdater.java:75)
2015-12-08 16:52:46.425 DEBUG com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: Not yet: ed995474-3b25-47cf-b207-90bc308b6ad3, 100 send, 100 confirm, 0 consume auto commit, 100 consume seeking, 4 checks (ResultsUpdater.java:75)
2015-12-08 16:52:46.425 DEBUG com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: Not yet: 5b99bd7e-5d65-4ca2-b76c-25c8567537ff, 100 send, 100 confirm, 45 consume auto commit, 100 consume seeking, 8 checks (ResultsUpdater.java:75)
2015-12-08 16:52:46.425 DEBUG com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: Not yet: d4711f64-13b3-4d1d-aad8-736ca660dae1, 100 send, 100 confirm, 45 consume auto commit, 100 consume seeking, 8 checks (ResultsUpdater.java:75)
2015-12-08 16:52:46.426 DEBUG com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: Not yet: 7ccdfbd0-b843-401f-9ad1-e5be4849dc62, 100 send, 100 confirm, 45 consume auto commit, 100 consume seeking, 8 checks (ResultsUpdater.java:75)
2015-12-08 16:52:46.426 DEBUG com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: Not yet: d1e26cbf-6eef-4743-ab36-8898a8bfb53b, 100 send, 100 confirm, 0 consume auto commit, 100 consume seeking, 4 checks (ResultsUpdater.java:75)
2015-12-08 16:52:46.426 DEBUG com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: Not yet: 85017713-eb93-478b-8288-7a29a3c72064, 100 send, 100 confirm, 0 consume auto commit, 100 consume seeking, 4 checks (ResultsUpdater.java:75)
2015-12-08 16:52:46.426 DEBUG com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: Not yet: eb26f45b-77ab-4b48-bea9-a9f66630cdc2, 100 send, 100 confirm, 0 consume auto commit, 100 consume seeking, 4 checks (ResultsUpdater.java:75)
2015-12-08 16:52:46.426 DEBUG com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: Not yet: 965986a4-74a2-4179-a40a-5b9901d64c10, 100 send, 100 confirm, 45 consume auto commit, 100 consume seeking, 8 checks (ResultsUpdater.java:75)
2015-12-08 16:52:46.426 DEBUG com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: Not yet: f4fb9f5e-2a5e-407c-9484-711be52aac24, 100 send, 100 confirm, 0 consume auto commit, 100 consume seeking, 4 checks (ResultsUpdater.java:75)
2015-12-08 16:52:46.426 DEBUG com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: Not yet: 9e38acd6-f211-49c5-8b1d-0f0a6113e70e, 100 send, 100 confirm, 45 consume auto commit, 100 consume seeking, 8 checks (ResultsUpdater.java:75)
2015-12-08 16:52:46.426 DEBUG com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: Not yet: 83ec96e4-0c89-485d-b42d-551a8ac25ea6, 100 send, 100 confirm, 0 consume auto commit, 100 consume seeking, 6 checks (ResultsUpdater.java:75)
2015-12-08 16:52:46.427 DEBUG com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: Not yet: e3585c37-ae09-4a3e-a5ec-3f40d23779e5, 100 send, 100 confirm, 44 consume auto commit, 100 consume seeking, 8 checks (ResultsUpdater.java:75)
2015-12-08 16:52:46.427 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 10000                 send requests (ResultsUpdater.java:84)
2015-12-08 16:52:46.428 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 0             send duplications (ResultsUpdater.java:85)
2015-12-08 16:52:46.428 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 0             send bits fail (ResultsUpdater.java:86)
2015-12-08 16:52:46.428 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 10000                 send confirm success (ResultsUpdater.java:88)
2015-12-08 16:52:46.428 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 0             send confirm fail (ResultsUpdater.java:89)
2015-12-08 16:52:46.428 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 0             send confirm duplications (ResultsUpdater.java:90)
2015-12-08 16:52:46.428 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 0             send confirm bits fail (ResultsUpdater.java:91)
2015-12-08 16:52:46.428 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 8913                  consume auto commit (ResultsUpdater.java:93)
2015-12-08 16:52:46.428 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 0             consume auto commit duplications (ResultsUpdater.java:94)
2015-12-08 16:52:46.428 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 0             consume auto commit bits fail (ResultsUpdater.java:95)
2015-12-08 16:52:46.428 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 10000                 consume seeking (ResultsUpdater.java:97)
2015-12-08 16:52:46.428 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 3811                  consume seeking skip (consumed multiple times due to simulated error) (ResultsUpdater.java:98)
2015-12-08 16:52:46.428 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 0             consume seeking duplications (ResultsUpdater.java:99)
2015-12-08 16:52:46.429 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 0             consume seeking bits fail (ResultsUpdater.java:100)
2015-12-08 16:52:46.429 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 8600                  success bits (ResultsUpdater.java:102)
2015-12-08 16:52:46.429 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 0             total not confirmed (ResultsUpdater.java:104)
2015-12-08 16:52:46.429 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 1087                  total not consumed auto commit (ResultsUpdater.java:105)
2015-12-08 16:52:46.429 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 1400                  total not success (ResultsUpdater.java:106)
2015-12-08 16:52:46.429 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 0             total not consumed seeking (ResultsUpdater.java:107)
````


### Analysis while the issue is present

- CPU utilization goes significantly higher (in my case from about 3% to 17% during the issue).
- Stop all producers to reliably verify some of the data were not read by consumers.

````sh
./stop_all_producers.sh
````

- All consumers promptly finish reading and wait for more data, but some messages are still missing (`Not yet:` lines are still present until timeout).
- See screenshots of htop and jvisualvm, they were taken while the issue was present.


### Work around using external touch

- Touch Kafka's consumer group by adding another consumer which forces another rebalancing.

````sh
./run_Kafka09AutoCommitConsumer.sh &
````

- The missing data surprisingly appears in Kafka and are consumed (it was 15 minutes after stop of producers in my case).

````
2015-12-09 09:27:49.206 DEBUG com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: ====================== Updating state... ====================== (ResultsUpdater.java:52)
2015-12-09 09:27:49.207 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 47000                 send requests (ResultsUpdater.java:84)
2015-12-09 09:27:49.207 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 0             send duplications (ResultsUpdater.java:85)
2015-12-09 09:27:49.207 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 0             send bits fail (ResultsUpdater.java:86)
2015-12-09 09:27:49.207 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 47000                 send confirm success (ResultsUpdater.java:88)
2015-12-09 09:27:49.207 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 0             send confirm fail (ResultsUpdater.java:89)
2015-12-09 09:27:49.207 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 0             send confirm duplications (ResultsUpdater.java:90)
2015-12-09 09:27:49.207 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 0             send confirm bits fail (ResultsUpdater.java:91)
2015-12-09 09:27:49.207 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 45892                 consume auto commit (ResultsUpdater.java:93)
2015-12-09 09:27:49.207 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 0             consume auto commit duplications (ResultsUpdater.java:94)
2015-12-09 09:27:49.207 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 1108                  consume auto commit bits fail (ResultsUpdater.java:95)
2015-12-09 09:27:49.207 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 47000                 consume seeking (ResultsUpdater.java:97)
2015-12-09 09:27:49.207 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 17164                 consume seeking skip (consumed multiple times due to simulated error) (ResultsUpdater.java:98)
2015-12-09 09:27:49.207 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 0             consume seeking duplications (ResultsUpdater.java:99)
2015-12-09 09:27:49.207 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 1108                  consume seeking bits fail (ResultsUpdater.java:100)
2015-12-09 09:27:49.207 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 45800                 success bits (ResultsUpdater.java:102)
2015-12-09 09:27:49.208 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 0             total not confirmed (ResultsUpdater.java:104)
2015-12-09 09:27:49.208 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 1108                  total not consumed auto commit (ResultsUpdater.java:105)
2015-12-09 09:27:49.208 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 1200                  total not success (ResultsUpdater.java:106)
2015-12-09 09:27:49.208 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 0             total not consumed seeking (ResultsUpdater.java:107)
2015-12-09 09:27:54.208 DEBUG com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: ====================== Updating state... ====================== (ResultsUpdater.java:52)
2015-12-09 09:27:54.214 DEBUG com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: Not yet: 1fedd86b-76d8-4160-91ad-116a43dd7b40, 0 send, 0 confirm, 100 consume auto commit, 0 consume seeking, 0 checks (ResultsUpdater.java:75)
2015-12-09 09:27:54.214 DEBUG com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: Not yet: 29aa07c5-4aa7-4d32-a1bd-2d17744ffedb, 0 send, 0 confirm, 54 consume auto commit, 0 consume seeking, 0 checks (ResultsUpdater.java:75)
2015-12-09 09:27:54.214 DEBUG com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: Not yet: 1f5ffe94-be1c-4ae9-8905-465013e89ce3, 0 send, 0 confirm, 100 consume auto commit, 0 consume seeking, 0 checks (ResultsUpdater.java:75)
2015-12-09 09:27:54.215 DEBUG com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: Not yet: 1608ee41-ee89-49d4-a60a-53d94912a5e0, 0 send, 0 confirm, 100 consume auto commit, 0 consume seeking, 0 checks (ResultsUpdater.java:75)
2015-12-09 09:27:54.215 DEBUG com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: Not yet: 240d6507-cf58-4f19-9b6f-35a4c5cca2a0, 0 send, 0 confirm, 100 consume auto commit, 0 consume seeking, 0 checks (ResultsUpdater.java:75)
2015-12-09 09:27:54.215 DEBUG com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: Not yet: ffd010c4-92bf-4cec-8d2b-0983effc337c, 0 send, 0 confirm, 100 consume auto commit, 0 consume seeking, 0 checks (ResultsUpdater.java:75)
2015-12-09 09:27:54.215 DEBUG com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: Not yet: 97ee1208-2d42-4a87-9ba5-222c1442098d, 0 send, 0 confirm, 100 consume auto commit, 0 consume seeking, 0 checks (ResultsUpdater.java:75)
2015-12-09 09:27:54.215 DEBUG com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: Not yet: 5a400052-d012-4126-8ceb-4556ff9e0d86, 0 send, 0 confirm, 100 consume auto commit, 0 consume seeking, 0 checks (ResultsUpdater.java:75)
2015-12-09 09:27:54.215 DEBUG com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: Not yet: 9deaefd1-b9f4-42df-8156-4edc1c2e1cd8, 0 send, 0 confirm, 100 consume auto commit, 0 consume seeking, 0 checks (ResultsUpdater.java:75)
2015-12-09 09:27:54.216 DEBUG com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: Not yet: fb1e2354-8c5d-4faf-a2c3-2605c7eeb29f, 0 send, 0 confirm, 54 consume auto commit, 0 consume seeking, 0 checks (ResultsUpdater.java:75)
2015-12-09 09:27:54.216 DEBUG com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: Not yet: 6acdccaa-76dc-455e-ac92-af890e0db280, 0 send, 0 confirm, 100 consume auto commit, 0 consume seeking, 0 checks (ResultsUpdater.java:75)
2015-12-09 09:27:54.216 DEBUG com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: Not yet: 153c6234-afed-4678-bb42-7e0931e8e542, 0 send, 0 confirm, 100 consume auto commit, 0 consume seeking, 0 checks (ResultsUpdater.java:75)
2015-12-09 09:27:54.217 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 47000                 send requests (ResultsUpdater.java:84)
2015-12-09 09:27:54.217 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 0             send duplications (ResultsUpdater.java:85)
2015-12-09 09:27:54.217 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 0             send bits fail (ResultsUpdater.java:86)
2015-12-09 09:27:54.217 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 47000                 send confirm success (ResultsUpdater.java:88)
2015-12-09 09:27:54.217 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 0             send confirm fail (ResultsUpdater.java:89)
2015-12-09 09:27:54.217 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 0             send confirm duplications (ResultsUpdater.java:90)
2015-12-09 09:27:54.218 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 0             send confirm bits fail (ResultsUpdater.java:91)
2015-12-09 09:27:54.218 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 47000                 consume auto commit (ResultsUpdater.java:93)
2015-12-09 09:27:54.218 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 0             consume auto commit duplications (ResultsUpdater.java:94)
2015-12-09 09:27:54.218 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 1108                  consume auto commit bits fail (ResultsUpdater.java:95)
2015-12-09 09:27:54.218 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 47000                 consume seeking (ResultsUpdater.java:97)
2015-12-09 09:27:54.218 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 17164                 consume seeking skip (consumed multiple times due to simulated error) (ResultsUpdater.java:98)
2015-12-09 09:27:54.218 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 0             consume seeking duplications (ResultsUpdater.java:99)
2015-12-09 09:27:54.218 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 1108                  consume seeking bits fail (ResultsUpdater.java:100)
2015-12-09 09:27:54.218 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 45800                 success bits (ResultsUpdater.java:102)
2015-12-09 09:27:54.218 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 0             total not confirmed (ResultsUpdater.java:104)
2015-12-09 09:27:54.218 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 0             total not consumed auto commit (ResultsUpdater.java:105)
2015-12-09 09:27:54.218 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 1200                  total not success (ResultsUpdater.java:106)
2015-12-09 09:27:54.218 INFO  com.avast.kafkatests.ResultsUpdater [ResultsUpdater-timer-0]: State: 0             total not consumed seeking (ResultsUpdater.java:107)
````

- Stop all tools and finish the test.

````sh
./stop_all_consumers.sh
./run_ResultsUpdater.sh
````


### Analysis of captured logs

- You can find details in consumer logs using logged IDs from `Not yet:` line (`grep -rI '1fedd86b-76d8-4160-91ad-116a43dd7b40' .`).
- Search big delay in timestamps of log messages.
- logs/Kafka09AutoCommitConsumer_2015-12-09_09:06:43.log
    - Partition kafka-test/6 wasn't consumed for about 15 minutes.

````
2015-12-09 09:12:06.907 TRACE c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Message consumed: 1faae32a-6bd6-47c1-92c7-bb65933f548b, 98, kafka-test/5/5598 (Kafka09AutoCommitConsumer.java:76)
2015-12-09 09:12:06.908 TRACE c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Message consumed: 1faae32a-6bd6-47c1-92c7-bb65933f548b, 99, kafka-test/5/5599 (Kafka09AutoCommitConsumer.java:76)
2015-12-09 09:27:48.217 INFO  o.a.k.c.c.i.AbstractCoordinator     [Kafka09AutoCommitConsumer-worker-0]: Attempt to heart beat failed since the group is rebalancing, try to re-join group. (AbstractCoordinator.java:633)
2015-12-09 09:27:48.218 INFO  c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Rebalance callback, revoked: [kafka-test-5] (Kafka09AutoCommitConsumer.java:90)
2015-12-09 09:27:51.130 INFO  c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Rebalance callback, assigned: [kafka-test-6] (Kafka09AutoCommitConsumer.java:95)
2015-12-09 09:27:51.132 TRACE c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Message consumed: 29aa07c5-4aa7-4d32-a1bd-2d17744ffedb, 46, kafka-test/6/3092 (Kafka09AutoCommitConsumer.java:76)
2015-12-09 09:27:51.132 TRACE c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Message consumed: fb1e2354-8c5d-4faf-a2c3-2605c7eeb29f, 46, kafka-test/6/3093 (Kafka09AutoCommitConsumer.java:76)
2015-12-09 09:27:51.132 TRACE c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Message consumed: 29aa07c5-4aa7-4d32-a1bd-2d17744ffedb, 47, kafka-test/6/3094 (Kafka09AutoCommitConsumer.java:76)
````

- Find which node was responsible for this partition.

````
grep -rI 'Rebalance callback.*kafka-test-6' . | grep -v README | sort -t : -k 5
./logs/Kafka09AutoCommitConsumer_2015-12-09_09:03:30.log:2015-12-09 09:03:32.888 INFO  c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Rebalance callback, assigned: [kafka-test-1, kafka-test-2, kafka-test-0, kafka-test-7, kafka-test-8, kafka-test-5, kafka-test-6, kafka-test-3, kafka-test-4] (Kafka09AutoCommitConsumer.java:95)
./logs/Kafka09SeekingConsumer_2015-12-09_09:03:40.log:2015-12-09 09:03:41.410 INFO  c.a.kafkatests.SeekingConsumerLogic [Kafka09SeekingConsumer-worker-0]: Rebalance callback, assigned: [kafka-test-1, kafka-test-2, kafka-test-0, kafka-test-7, kafka-test-8, kafka-test-5, kafka-test-6, kafka-test-3, kafka-test-4] (SeekingConsumerLogic.java:115)
./logs/Kafka09AutoCommitConsumer_2015-12-09_09:03:30.log:2015-12-09 09:05:14.901 INFO  c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Rebalance callback, revoked: [kafka-test-1, kafka-test-2, kafka-test-0, kafka-test-7, kafka-test-8, kafka-test-5, kafka-test-6, kafka-test-3, kafka-test-4] (Kafka09AutoCommitConsumer.java:90)
./logs/Kafka09AutoCommitConsumer_2015-12-09_09:03:30.log:2015-12-09 09:05:14.908 INFO  c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Rebalance callback, assigned: [kafka-test-7, kafka-test-8, kafka-test-5, kafka-test-6] (Kafka09AutoCommitConsumer.java:95)
./logs/Kafka09AutoCommitConsumer_2015-12-09_09:03:30.log:2015-12-09 09:05:26.914 INFO  c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Rebalance callback, revoked: [kafka-test-7, kafka-test-8, kafka-test-5, kafka-test-6] (Kafka09AutoCommitConsumer.java:90)
./logs/Kafka09AutoCommitConsumer_2015-12-09_09:05:23.log:2015-12-09 09:05:26.931 INFO  c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Rebalance callback, assigned: [kafka-test-7, kafka-test-8, kafka-test-6] (Kafka09AutoCommitConsumer.java:95)
./logs/Kafka09AutoCommitConsumer_2015-12-09_09:05:23.log:2015-12-09 09:05:35.935 INFO  c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Rebalance callback, revoked: [kafka-test-7, kafka-test-8, kafka-test-6] (Kafka09AutoCommitConsumer.java:90)
./logs/Kafka09AutoCommitConsumer_2015-12-09_09:05:33.log:2015-12-09 09:05:35.943 INFO  c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Rebalance callback, assigned: [kafka-test-5, kafka-test-6] (Kafka09AutoCommitConsumer.java:95)
./logs/Kafka09AutoCommitConsumer_2015-12-09_09:05:33.log:2015-12-09 09:05:59.954 INFO  c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Rebalance callback, revoked: [kafka-test-5, kafka-test-6] (Kafka09AutoCommitConsumer.java:90)
./logs/Kafka09AutoCommitConsumer_2015-12-09_09:05:23.log:2015-12-09 09:05:59.961 INFO  c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Rebalance callback, assigned: [kafka-test-7, kafka-test-6] (Kafka09AutoCommitConsumer.java:95)
./logs/Kafka09AutoCommitConsumer_2015-12-09_09:05:23.log:2015-12-09 09:06:23.966 INFO  c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Rebalance callback, revoked: [kafka-test-7, kafka-test-6] (Kafka09AutoCommitConsumer.java:90)
./logs/Kafka09AutoCommitConsumer_2015-12-09_09:05:33.log:2015-12-09 09:06:23.981 INFO  c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Rebalance callback, assigned: [kafka-test-6] (Kafka09AutoCommitConsumer.java:95)
./logs/Kafka09AutoCommitConsumer_2015-12-09_09:05:33.log:2015-12-09 09:06:44.993 INFO  c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Rebalance callback, revoked: [kafka-test-6] (Kafka09AutoCommitConsumer.java:90)
./logs/Kafka09AutoCommitConsumer_2015-12-09_09:05:33.log:2015-12-09 09:06:45.013 INFO  c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Rebalance callback, assigned: [kafka-test-6] (Kafka09AutoCommitConsumer.java:95)
./logs/Kafka09AutoCommitConsumer_2015-12-09_09:05:33.log:2015-12-09 09:07:12.028 INFO  c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Rebalance callback, revoked: [kafka-test-6] (Kafka09AutoCommitConsumer.java:90)
./logs/Kafka09AutoCommitConsumer_2015-12-09_09:05:33.log:2015-12-09 09:07:12.032 INFO  c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Rebalance callback, assigned: [kafka-test-6] (Kafka09AutoCommitConsumer.java:95)
./logs/Kafka09AutoCommitConsumer_2015-12-09_09:05:33.log:2015-12-09 09:07:36.039 INFO  c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Rebalance callback, revoked: [kafka-test-6] (Kafka09AutoCommitConsumer.java:90)
./logs/Kafka09AutoCommitConsumer_2015-12-09_09:05:33.log:2015-12-09 09:07:36.047 INFO  c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Rebalance callback, assigned: [kafka-test-6] (Kafka09AutoCommitConsumer.java:95)
./logs/Kafka09AutoCommitConsumer_2015-12-09_09:05:33.log:2015-12-09 09:09:27.063 INFO  c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Rebalance callback, revoked: [kafka-test-6] (Kafka09AutoCommitConsumer.java:90)
./logs/Kafka09AutoCommitConsumer_2015-12-09_09:03:30.log:2015-12-09 09:09:27.077 INFO  c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Rebalance callback, assigned: [kafka-test-6] (Kafka09AutoCommitConsumer.java:95)
./logs/Kafka09AutoCommitConsumer_2015-12-09_09:03:30.log:2015-12-09 09:09:45.080 INFO  c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Rebalance callback, revoked: [kafka-test-6] (Kafka09AutoCommitConsumer.java:90)
./logs/Kafka09AutoCommitConsumer_2015-12-09_09:03:30.log:2015-12-09 09:09:45.097 INFO  c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Rebalance callback, assigned: [kafka-test-6] (Kafka09AutoCommitConsumer.java:95)
./logs/Kafka09AutoCommitConsumer_2015-12-09_09:03:30.log:2015-12-09 09:09:48.100 INFO  c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Rebalance callback, revoked: [kafka-test-6] (Kafka09AutoCommitConsumer.java:90)
./logs/Kafka09AutoCommitConsumer_2015-12-09_09:03:30.log:2015-12-09 09:09:48.123 INFO  c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Rebalance callback, assigned: [kafka-test-6] (Kafka09AutoCommitConsumer.java:95)
./logs/Kafka09AutoCommitConsumer_2015-12-09_09:03:30.log:2015-12-09 09:27:51.126 INFO  c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Rebalance callback, revoked: [kafka-test-6] (Kafka09AutoCommitConsumer.java:90)
./logs/Kafka09AutoCommitConsumer_2015-12-09_09:06:43.log:2015-12-09 09:27:51.130 INFO  c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Rebalance callback, assigned: [kafka-test-6] (Kafka09AutoCommitConsumer.java:95)
````

- logs/Kafka09AutoCommitConsumer_2015-12-09_09:03:30.log
    - This consumer instance owned the partition but it wasn't consuming it.
    - It was analyzed using jvisualvm according to `Profiler Agent` message (only this one was opened in jvisualvm, much higher CPU in htop -> PID -> jvisualvm).

````
2015-12-09 09:09:48.100 INFO  c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Rebalance callback, revoked: [kafka-test-6] (Kafka09AutoCommitConsumer.java:90)
2015-12-09 09:09:48.123 INFO  c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Rebalance callback, assigned: [kafka-test-6] (Kafka09AutoCommitConsumer.java:95)
2015-12-09 09:09:48.124 TRACE c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Message consumed: 29aa07c5-4aa7-4d32-a1bd-2d17744ffedb, 42, kafka-test/6/3084 (Kafka09AutoCommitConsumer.java:76)
2015-12-09 09:09:48.124 TRACE c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Message consumed: fb1e2354-8c5d-4faf-a2c3-2605c7eeb29f, 42, kafka-test/6/3085 (Kafka09AutoCommitConsumer.java:76)
2015-12-09 09:09:48.125 TRACE c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Message consumed: 29aa07c5-4aa7-4d32-a1bd-2d17744ffedb, 43, kafka-test/6/3086 (Kafka09AutoCommitConsumer.java:76)
2015-12-09 09:09:48.125 TRACE c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Message consumed: fb1e2354-8c5d-4faf-a2c3-2605c7eeb29f, 43, kafka-test/6/3087 (Kafka09AutoCommitConsumer.java:76)
2015-12-09 09:09:48.125 TRACE c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Message consumed: 29aa07c5-4aa7-4d32-a1bd-2d17744ffedb, 44, kafka-test/6/3088 (Kafka09AutoCommitConsumer.java:76)
2015-12-09 09:09:48.125 TRACE c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Message consumed: fb1e2354-8c5d-4faf-a2c3-2605c7eeb29f, 44, kafka-test/6/3089 (Kafka09AutoCommitConsumer.java:76)
2015-12-09 09:09:48.125 TRACE c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Message consumed: 29aa07c5-4aa7-4d32-a1bd-2d17744ffedb, 45, kafka-test/6/3090 (Kafka09AutoCommitConsumer.java:76)
2015-12-09 09:09:48.125 TRACE c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Message consumed: fb1e2354-8c5d-4faf-a2c3-2605c7eeb29f, 45, kafka-test/6/3091 (Kafka09AutoCommitConsumer.java:76)
Profiler Agent: Waiting for connection on port 5140 (Protocol version: 15)
Profiler Agent: Established connection with the tool
Profiler Agent: Local accelerated session
Profiler Agent: Connection with agent closed
2015-12-09 09:27:51.125 INFO  o.a.k.c.c.i.AbstractCoordinator     [Kafka09AutoCommitConsumer-worker-0]: Attempt to heart beat failed since the group is rebalancing, try to re-join group. (AbstractCoordinator.java:633)
2015-12-09 09:27:51.126 INFO  c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Rebalance callback, revoked: [kafka-test-6] (Kafka09AutoCommitConsumer.java:90)
2015-12-09 09:27:51.131 INFO  c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Rebalance callback, assigned: [kafka-test-7] (Kafka09AutoCommitConsumer.java:95)
````

- logs/Kafka09AutoCommitConsumer_2015-12-09_09:06:43.log
    - This consumer instance started owning the partition after touch by execution of another consumer, rebalancing and unblocking.
    - Consuming from this partition was blocked for 15 minutes.
    - External action was needed to unblock consuming.

````
2015-12-09 09:12:06.907 TRACE c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Message consumed: 1faae32a-6bd6-47c1-92c7-bb65933f548b, 98, kafka-test/5/5598 (Kafka09AutoCommitConsumer.java:76)
2015-12-09 09:12:06.908 TRACE c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Message consumed: 1faae32a-6bd6-47c1-92c7-bb65933f548b, 99, kafka-test/5/5599 (Kafka09AutoCommitConsumer.java:76)
2015-12-09 09:27:48.217 INFO  o.a.k.c.c.i.AbstractCoordinator     [Kafka09AutoCommitConsumer-worker-0]: Attempt to heart beat failed since the group is rebalancing, try to re-join group. (AbstractCoordinator.java:633)
2015-12-09 09:27:48.218 INFO  c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Rebalance callback, revoked: [kafka-test-5] (Kafka09AutoCommitConsumer.java:90)
2015-12-09 09:27:51.130 INFO  c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Rebalance callback, assigned: [kafka-test-6] (Kafka09AutoCommitConsumer.java:95)
2015-12-09 09:27:51.132 TRACE c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Message consumed: 29aa07c5-4aa7-4d32-a1bd-2d17744ffedb, 46, kafka-test/6/3092 (Kafka09AutoCommitConsumer.java:76)
2015-12-09 09:27:51.132 TRACE c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Message consumed: fb1e2354-8c5d-4faf-a2c3-2605c7eeb29f, 46, kafka-test/6/3093 (Kafka09AutoCommitConsumer.java:76)
2015-12-09 09:27:51.132 TRACE c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Message consumed: 29aa07c5-4aa7-4d32-a1bd-2d17744ffedb, 47, kafka-test/6/3094 (Kafka09AutoCommitConsumer.java:76)
2015-12-09 09:27:51.132 TRACE c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Message consumed: fb1e2354-8c5d-4faf-a2c3-2605c7eeb29f, 47, kafka-test/6/3095 (Kafka09AutoCommitConsumer.java:76)
2015-12-09 09:27:51.132 TRACE c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Message consumed: 29aa07c5-4aa7-4d32-a1bd-2d17744ffedb, 48, kafka-test/6/3096 (Kafka09AutoCommitConsumer.java:76)
2015-12-09 09:27:51.132 TRACE c.a.k.Kafka09AutoCommitConsumer     [Kafka09AutoCommitConsumer-worker-0]: Message consumed: fb1e2354-8c5d-4faf-a2c3-2605c7eeb29f, 48, kafka-test/6/3097 (Kafka09AutoCommitConsumer.java:76)
````

- logs/Kafka09SeekingConsumer_2015-12-09_09:03:40.log
    - Second consumer group wasn't affected at all.

````
2015-12-09 09:09:43.695 TRACE c.a.kafkatests.SeekingConsumerLogic [Kafka09SeekingConsumer-worker-0]: Message skip: fb1e2354-8c5d-4faf-a2c3-2605c7eeb29f, 0, kafka-test/6/3001 (SeekingConsumerLogic.java:58)
...
2015-12-09 09:09:44.619 TRACE c.a.kafkatests.SeekingConsumerLogic [Kafka09SeekingConsumer-worker-0]: Message consumed: fb1e2354-8c5d-4faf-a2c3-2605c7eeb29f, 0, kafka-test/6/3001 (SeekingConsumerLogic.java:53)
...
2015-12-09 09:09:53.529 TRACE c.a.kafkatests.SeekingConsumerLogic [Kafka09SeekingConsumer-worker-0]: Message skip: fb1e2354-8c5d-4faf-a2c3-2605c7eeb29f, 99, kafka-test/6/3199 (SeekingConsumerLogic.java:58)
...
2015-12-09 09:09:54.338 TRACE c.a.kafkatests.SeekingConsumerLogic [Kafka09SeekingConsumer-worker-0]: Message consumed: fb1e2354-8c5d-4faf-a2c3-2605c7eeb29f, 99, kafka-test/6/3199 (SeekingConsumerLogic.java:53)
````

````
2015-12-09 09:09:43.497 TRACE c.a.kafkatests.SeekingConsumerLogic [Kafka09SeekingConsumer-worker-0]: Message skip: 29aa07c5-4aa7-4d32-a1bd-2d17744ffedb, 0, kafka-test/6/3000 (SeekingConsumerLogic.java:58)
...
2015-12-09 09:09:44.619 TRACE c.a.kafkatests.SeekingConsumerLogic [Kafka09SeekingConsumer-worker-0]: Message consumed: 29aa07c5-4aa7-4d32-a1bd-2d17744ffedb, 0, kafka-test/6/3000 (SeekingConsumerLogic.java:53)
...
2015-12-09 09:09:53.529 TRACE c.a.kafkatests.SeekingConsumerLogic [Kafka09SeekingConsumer-worker-0]: Message skip: 29aa07c5-4aa7-4d32-a1bd-2d17744ffedb, 99, kafka-test/6/3198 (SeekingConsumerLogic.java:58)
...
2015-12-09 09:09:54.338 TRACE c.a.kafkatests.SeekingConsumerLogic [Kafka09SeekingConsumer-worker-0]: Message consumed: 29aa07c5-4aa7-4d32-a1bd-2d17744ffedb, 99, kafka-test/6/3198 (SeekingConsumerLogic.java:53)
````


System, versions
----------------

...if anyone is interested.

- 8 CPUs.
- 16 GB RAM.
- GNU/Linux, Debian Jessie (current stable with few backports).
- Oracle Java 8.
- Kafka 0.9.0.0, Scala 2.11.

````
uname -a
Linux traktor 4.2.0-0.bpo.1-amd64 #1 SMP Debian 4.2.5-1~bpo8+1 (2015-11-02) x86_64 GNU/Linux

java -version
java version "1.8.0_66"
Java(TM) SE Runtime Environment (build 1.8.0_66-b17)
Java HotSpot(TM) 64-Bit Server VM (build 25.66-b17, mixed mode)
````

The issue has been independently reproduced on 4 CPUs virtual machine with CentOS according to these instructions.
It wasn't reproduced on a single CPU virtual machine, but it doesn't mean it can't appear there too. My colleague
tried only few attempts and then added CPUs. More parallelism probably gives the issue more chances to appear.


Conclusion
----------

Issue accepted and very promptly fixed by Kafka developers.

- [https://issues.apache.org/jira/browse/KAFKA-2978](https://issues.apache.org/jira/browse/KAFKA-2978)
- [https://github.com/apache/kafka/pull/666](https://github.com/apache/kafka/pull/666)
