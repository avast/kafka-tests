#!/bin/bash

CLASS=ChaoticManager
COMPONENT_TYPE=AutoCommitConsumer

LOGFILE="logs/${CLASS}_${COMPONENT_TYPE}_`date +%F_%H-%M-%S`.log"
mkdir -p `dirname ${LOGFILE}`
exec java -cp "target/*:config" com.avast.kafkatests.${CLASS} ${COMPONENT_TYPE} >> ${LOGFILE}
