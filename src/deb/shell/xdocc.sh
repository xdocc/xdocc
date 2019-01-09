#!/bin/bash

#start with
# sudo systemctl start xdocc.service

#enable with
# sudo systemctl enable xdocc.service

SRC="/usr/share/xdocc"
GEN="/var/www/example-xdocc"
CACHE="/var/lib/xdocc/xdocc"
LOG="/var/log/xdocc/xdocc.log"


if [ -d /etc/xdocc ]; then
    if [ "$(ls -A /etc/xdocc)" ]; then
        for filename in /etc/xdocc/*; do
            . /etc/xdocc/$filename
            echo "Starting..." >> $LOG
            java -jar /usr/lib/xdocc/xdocc.jar -s $SRC -g $GEN -c $CACHE >> $LOG 2>&1
        done
        exit 0;
    fi
fi

echo "Starting..." >> $LOG
java -jar /usr/lib/xdocc/xdocc.jar -s $SRC -g $GEN -c $CACHE >> $LOG 2>&1