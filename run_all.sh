#!/bin/bash

./erase_all.sh

./run_Kafka09AutoCommitConsumer.sh &
./run_Kafka09SeekingConsumer.sh &
./run_ResultsUpdater.sh &
./run_Kafka09Producer.sh &
