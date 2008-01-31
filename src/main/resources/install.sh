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
sudo cp shoot* README $installDir/

function link
{
for x
do 
    link=/usr/local/bin/$x
    echo "Creating link: $link"
    if [ -L $link ]; then
        echo "Link exists.  Overwriting."
        sudo rm $link
    fi
    sudo ln -s $x $link || die "Could not link file $x."
done
}

pushd $installDir

echo "Linking files in /usr/local/bin"
link shoot*
popd

