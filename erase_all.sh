#!/bin/bash -x

rm -rf logs

echo 'FLUSHALL' | redis-cli
echo 'KEYS *' | redis-cli
