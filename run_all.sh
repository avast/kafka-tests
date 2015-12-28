#!/bin/bash

./run_ResultsUpdater.sh &
./run_AutoCommitConsumer_ChaoticManager.sh &
./run_SeekingConsumer_ChaoticManager.sh &
./run_GeneratorProducer_ChaoticManager.sh &
