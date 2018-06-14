#!/bin/bash

echo "Starting..." >> /var/log/xdocc/xdocc.log
java -jar /usr/lib/xdocc/xdocc.jar -s /usr/share/xdocc -g /var/www/example-xdocc -c /var/lib/xdocc/xdocc >> /var/log/xdocc/xdocc.log 2>&1

#start with
# sudo systemctl start xdocc.service

#enable with
# sudo systemctl enable xdocc.service