#!/bin/sh
drop=http://thermos.herokuapp.com/insert

a=0
while [ "$a" == 0 ]; do
 round=`ping -c 1 localhost | grep round-trip | awk -F= '{ split($2,a,"/"); print a[2]}'`
 now=`date +%s`
 echo "$drop $now $round"
 curl -s --data "key=$now&value=$round" $drop > /dev/null
 sleep 60
done
