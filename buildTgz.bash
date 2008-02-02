#!/usr/bin/env bash

die()
{
 echo $*
 exit 1
}

if [ $# -ne "1" ]
then
  die "Usage: ./buildTgz.bash version"
fi

BUILD_VER=$1
mvn assembly:assembly || die "Could not build assembly for S3"

BUILD_NAME=s3LittleShoot-$BUILD_VER
rm -rf $BUILD_NAME
mkdir $BUILD_NAME

cp target/*jar-with-dependencies.jar $BUILD_NAME/s3.jar || die "Could not copy jar"
cp src/main/resources/*.sh $BUILD_NAME
cp src/main/resources/sss $BUILD_NAME
cp src/main/resources/s3 $BUILD_NAME
chmod +x $BUILD_NAME/*

tar czvf $BUILD_NAME.tgz $BUILD_NAME || die "Could not build tgz"

cd $BUILD_NAME
./install.sh || die "Could not install"
cd ..

#shootPutPublic.sh $BUILD_NAME.tgz || die "Could not upload new tgz!!"
#./s3.jar put public littleshoot ../$BUILD_NAME.tgz || die "Could not upload new tgz!!" 

#cd ..
rm -rf $BUILD_NAME/

exit
