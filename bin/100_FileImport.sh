#!/bin/bash

SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do
  DIR="$(cd -P "$(dirname "$SOURCE")" && pwd)"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
done
DIR="$(cd -P "$(dirname "$SOURCE")" && pwd)"

classpath=$(${DIR}/classpath.sh core)
if [ $? -ne 0 ]; then
  echo "${classpath}"
  exit
fi

if [ "$1" != '' ]; then
  dir=$1
else
  dir=${DIR}/../data
fi

if [ "${VIRTUALIZATION_DISABLED}" = 'true' ]; then
  ip=localhost
else
  ip=192.168.33.10
fi

java \
-Dfile.encoding=UTF-8 \
-classpath ${classpath} \
com.altamiracorp.reddawn.cmdline.FileImport \
--zookeeperInstanceName=reddawn \
--zookeeperServerNames=${ip} \
--blurControllerLocation=${ip}:40010 \
--blurPath=hdfs://${ip}/blur \
--username=root \
--password=password \
--directory=${dir}
