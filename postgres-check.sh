#!/usr/bin/env bash
checked=0
while [ ! $(netstat -na | grep '0.0.0.0.5444 ') ] && [ $checked -lt 10 ];
do
  echo 'Checked port 5444 to see if Postgres is up - not yet'
  sleep 5
  ((checked += 1))
done
netstat -na | grep '0.0.0.0.5444 '
