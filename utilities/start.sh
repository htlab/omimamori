#!/bin/bash

DEV="/dev/ttyWISUN"
RATE=38400
java -Djava.library.path=/usr/lib/jni -jar BaseStation.jar -o $DEV -b $RATE
