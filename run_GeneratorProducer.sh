#!/bin/bash

CLASS=GeneratorProducer

LOGFILE="logs/${CLASS}_`date +%F_%T`.log"
mkdir -p `dirname ${LOGFILE}`
exec java -cp "target/*:config" com.avast.kafkatests.${CLASS} >> ${LOGFILE}
