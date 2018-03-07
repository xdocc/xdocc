#!/bin/bash

TMPFILE=$(mktemp)
uncss **/*.html > $TMPFILE
csplit -f $TMPFILE -q -z $TMPFILE "//\\*\\*\\*\\s[uncss]/" {*}
for i in `ls $TMPFILE??`; do
  FNAME=`grep -m1 "" $i | cut -d" " -f4`
  mv $i $FNAME
done

rm "$TMPFILE"
