#!/bin/sh

DMG_NAME=$4_v$3

echo "Creating uncompressed disk image: "
hdiutil create -size 160m -fs HFS+ -volname "$2" $1/$DMG_NAME-temp.dmg

echo "Mounting uncompressed disk image: "
hdiutil attach $1/$DMG_NAME-temp.dmg -readwrite -mount required

echo "Putting app into disk image: "
cp -R "$1/$2.app" "/Volumes/$2/"
ln -s "/Applications" "/Volumes/$2/Applications"

echo "Unmounting uncompressed image: "
hdiutil detach "/Volumes/$2"

echo "Compressing image: "
hdiutil convert $1/$DMG_NAME-temp.dmg -format UDZO -imagekey zlib-level=9 -o $1/$DMG_NAME.dmg

echo "Removing temporary disk image: "
rm $1/$DMG_NAME-temp.dmg
