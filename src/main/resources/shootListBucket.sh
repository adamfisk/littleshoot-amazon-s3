#!/usr/bin/env sh

die()
{
  echo $*
  exit 1
}

java -jar /usr/local/littleshoot/s3.jar list $* || die "What the hell?"
exit
