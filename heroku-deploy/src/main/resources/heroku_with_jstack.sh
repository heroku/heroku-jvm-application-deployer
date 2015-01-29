#!/usr/bin/env bash

eval "$@ &"
pid=$!

trap "kill -3 $pid; kill $pid" SIGTERM
trap "kill -9 $pid; exit" SIGKILL

sleep 10
while kill -0 $pid
do
  if [ -z "$JSTACK_INTERVAL" ]; then
    sleep 10
  else
    kill -3 $pid
    sleep ${JSTACK_INTERVAL}
  fi
done
