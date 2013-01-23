#!/bin/sh

hdiutil create -size 15m -type UDIF -fs HFS+ -volname $2 -attach $1/$2_v$3.dmg

cp -R $1/$2.app /Volumes/$2/
ln -s /Applications /Volumes/$2/Applications

umount /Volumes/$2
