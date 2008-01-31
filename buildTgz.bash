#!/usr/bin/env bash

die()
{
 echo $*
 exit 1
}

mvn assembly:assembly || die "Could not build assembly for S3"

mkdir s3LittleShoot

cp target/*jar-with-dependencies.jar s3LittleShoot/s3.jar || die "Could not copy jar"
cp src/main/resource/*.sh s3LittleShoot
chmod +x s3LittleShoot/*

tar czvf s3LittleShoot.tgz s3LittleShoot || die "Could not build tgz"

cd s3LittleShoot
./putPublic.sh littleshoot ../s3LittleShoot.tgz || die "Could not upload new tgz!!"

cd ..
rm -rf s3LittleShoot/

exit
