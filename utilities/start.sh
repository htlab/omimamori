#!/bin/bash

DEV="/dev/ttyWISUN"
RATE=38400

SERVER="omimamori-server.ht.sfc.keio.ac.jp"

for ((i=1;i<=5;i+=1))
do
if ping -q -c 1 -W 1 $SERVER >/dev/null; then
    echo "The network is up"
    java -Djava.library.path=/usr/lib/jni -jar  /root/omimamori/MQTTSNPublisher.jar -o $DEV -s $SERVER -id omimamori
    break
else
echo "The network is not connected"
time=((10 * $i
echo "wait $time seconds"
sleep $time
fi
done
#java -Djava.library.path=/usr/lib/jni -jar  /root/omimamori/MQTTSNPublisher.jar -o $DEV -s $SERVER -id omimamori 
