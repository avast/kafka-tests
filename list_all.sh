#!/bin/bash

ps aux | egrep 'Kafka09.*Consumer|Kafka09Producer|ResultsUpdater' | grep -v grep
