#!/bin/bash

ps aux | egrep 'Consumer|GeneratorProducer|ResultsUpdater' | grep -v grep
