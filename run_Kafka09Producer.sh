#!/bin/bash

CLASS=Kafka09Producer

LOGFILE="logs/${CLASS}_`date +%F_%T`.log"
mkdir -p `dirname ${LOGFILE}`
exec java -cp "target/*" com.avast.kafkatests.${CLASS} >> ${LOGFILE}
