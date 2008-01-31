#!/usr/bin/env sh

die()
{
  echo $*
  exit 1
}

installDir=/usr/local/littleshoot
echo "Copying files to $installDir."
mkdir $installDir || die "Could not make install dir"
cp * $installDir/

link()
{
for x
do 
    echo "Linking $x"
    ln -s $x /usr/local/bin/$x || die "Could not link file $x."
done
}

pushd $installDir
link *
popd

