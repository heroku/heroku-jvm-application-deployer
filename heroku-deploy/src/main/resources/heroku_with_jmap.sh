#!/usr/bin/env bash

eval "$@ &"
pid=$!

trap "kill -9 $pid; exit" SIGKILL

sleep 10
while kill -0 $pid
do
  if [ -z "$JMAP_INTERVAL" ]; then
    sleep 10
  else
    jmapOutput=`/app/.jdk/bin/jmap ${JMAP_OPTS:-"-histo"} $pid`
    echo "${jmapOutput}"
    if [[ "$jmapOutput" =~ "Dumping heap to" ]]; then
      dumpFile=`expr "$jmapOutput" : "Dumping heap to \(.*\) \.\.\."`
      filename="heap-${DYNO}-$(date +"%s").hprof"
      if [ ${AWS_ACCESS_KEY_ID:-""} != "" ]; then
        if [ ${AWS_SECRET_ACCESS_KEY:-""} != "" ]; then
          if [ ${S3_BUCKET_NAME:-""} != "" ]; then
            echo "Uploading heap dump to S3 as '${filename}' ..."

            resource="/${S3_BUCKET_NAME}/${filename}"
            dateValue=`date -R`
            contentType="application/binary"
            stringToSign="PUT\n\n${contentType}\n${dateValue}\n${resource}"
            signature=`echo -en ${stringToSign} | openssl sha1 -hmac ${AWS_SECRET_ACCESS_KEY} -binary | base64`

            curl -s -X PUT -T "${dumpFile}" \
              -H "Host: ${S3_BUCKET_NAME}.s3.amazonaws.com" \
              -H "Date: $dateValue" \
              -H "Content-Type: $contentType" \
              -H "Authorization: AWS ${AWS_ACCESS_KEY_ID}:${signature}" \
              https://${S3_BUCKET_NAME}.s3.amazonaws.com/${filename}
          else
            echo "You must set S3_BUCKET_NAME in order to upload heap dump file!"
          fi
        else
          echo "You must set AWS_SECRET_ACCESS_KEY in order to upload heap dump file!"
        fi
      else
        echo "You must set AWS_ACCESS_KEY_ID in order to upload heap dump file!"
      fi
      rm -f ${dumpFile}
    fi

    sleep ${JMAP_INTERVAL}
  fi
done
