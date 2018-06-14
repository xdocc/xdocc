#!/bin/bash

#remove unused css, this cannot be done by modpagespeed, as this includes all HTML files
TMPFILE=$(mktemp)
uncss "**/*.html" > $TMPFILE
csplit -f $TMPFILE -q -z $TMPFILE "//\\*\\*\\*\\s[uncss]/" {*}
for i in `ls $TMPFILE??`; do
  FNAME=`grep -m1 "" $i | cut -d" " -f4`
  mv $i $FNAME
done

rm "$TMPFILE"

#optimize SVG, saves aronud 50%/60%. modpagespeed does not optimize SVG yet
sxvgo design/*.svg
