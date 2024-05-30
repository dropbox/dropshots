#!/bin/bash

adb wait-for-devices

echo "Cleaning up old emulator data"
adb uninstall com.dropbox.dropshots.test || true
adb shell rm -rf /storage/emulated/0/Download/screenshots || true
