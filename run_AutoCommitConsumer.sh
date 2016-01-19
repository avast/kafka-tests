#!/bin/bash

CLASS=AutoCommitConsumer

LOGFILE="logs/${CLASS}_`date +%F_%H-%M-%S`.log"
mkdir -p `dirname ${LOGFILE}`
exec java -cp "target/*:config" com.avast.kafkatests.${CLASS} >> ${LOGFILE}
