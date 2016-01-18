#!/bin/bash

CLASS=ChaoticManager
COMPONENT_TYPE=SeekingConsumer

LOGFILE="logs/${CLASS}_${COMPONENT_TYPE}`date +%F_%T`.log"
mkdir -p `dirname ${LOGFILE}`
exec java -cp "target/*:config" com.avast.kafkatests.${CLASS} ${COMPONENT_TYPE} >> ${LOGFILE}
