#!/bin/bash

SSH_OPTS='-o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o LogLevel=QUIET'

hosts_file=$1

accumulomaster=$(awk '/ +accumulomaster/ {print $1}' ${hosts_file})

echo "ssh to ${accumulomaster} and as the accumulo user run: /usr/lib/accumulo/bin/accumulo init"
while [ "${ready}" != 'yes' ]; do
  echo "then type 'yes' and press return"
  read ready
done

ssh ${SSH_OPTS} ${accumulomaster} su - accumulo -c '/usr/lib/accumulo/bin/start-here.sh'

for node in $(awk '/node[0-9]+/ {print $1}' ${hosts_file}); do
  ssh ${SSH_OPTS} ${node} su - accumulo -c '/usr/lib/accumulo/bin/start-here.sh'
done
