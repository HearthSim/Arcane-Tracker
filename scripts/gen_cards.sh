#!/usr/bin/env bash

# this script will download ~1GB of unity3d files to the current directory
# it expects only one device connected to adb

if false
then
for i in $(adb shell su -c ls '/data/data/com.blizzard.wtcg.hearthstone/files/Data/dxt/*.unity3d')
do
  BASENAME=$(basename $i)
  adb shell su -c cp $i /sdcard/${BASENAME}
  adb pull /sdcard/${BASENAME}
  adb shell rm /sdcard/${BASENAME}
done
fi

# requires python 3 and a recent version of setuptools
if false
then
git clone https://github.com/HearthSim/UnityPack
cd UnityPack
python3 setup.py install
fi

# this is the actual tools that will produce images in the correct format
if false
then
git clone https://github.com/HearthSim/HearthstoneJSON
fi