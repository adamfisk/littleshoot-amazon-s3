#!/usr/bin/env sh

die()
{
  echo $*
  exit 1
}

installDir=/usr/local/littleshoot
if [ -e "$installDir" ]; then
    echo "Install dir exists.  Overwriting files."
else
    sudo mkdir $installDir || die "Could not make install dir"
fi

echo "Copying files to $installDir."
sudo cp * $installDir/

function link
{
for x
do 
    cd /usr/local/bin
    echo "Creating link: $x"
    if [ -L $x ]; then
        echo "Link exists.  Overwriting."
        sudo rm $x
    fi
    
    sudo ln -s $installDir/$x $x || die "Could not link file $x."
    newOwner=$USER
    sudo chown $newOwner $installDir/$x
    sudo chown -h $newOwner $x
done
}

pushd $installDir

echo "Linking files in /usr/local/bin"
link sss
link aws

popd

