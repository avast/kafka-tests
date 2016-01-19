#!/bin/bash

SERVERS='kafka01-test kafka02-test kafka03-test'
REMOTE_DIR='kafka-tests'

for server in ${SERVERS}
do
    echo "==================== ${server} ===================="
    rsync -av *.sh "${server}:${REMOTE_DIR}/"
    rsync -av target/*.jar "${server}:${REMOTE_DIR}/target/"
    rsync -av config_servers/* "${server}:${REMOTE_DIR}/config/"
    echo
done
