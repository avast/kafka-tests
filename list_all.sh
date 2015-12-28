#!/bin/bash

ps aux | egrep 'Consumer|GeneratorProducer|ResultsUpdater|ChaoticManager' | grep -v grep
