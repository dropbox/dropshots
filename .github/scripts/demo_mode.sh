#!/bin/bash
CMD=$1

if [[ $ADB == "" ]]; then
  ADB=adb
fi

if [[ $CMD != "on" && $CMD != "off" ]]; then
  echo "Usage: $0 [on|off] [hhmm]" >&2
  exit
fi

if [[ "$2" != "" ]]; then
  HHMM="$2"
fi

$ADB root || exit
$ADB wait-for-device
$ADB shell settings put global sysui_demo_allowed 1

if [ $CMD == "on" ]; then
  $ADB shell am broadcast -a com.android.systemui.demo -e command enter || exit
  if [[ "$HHMM" != "" ]]; then
    $ADB shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm ${HHMM}
  fi
  $ADB shell am broadcast -a com.android.systemui.demo -e command battery -e plugged false
  $ADB shell am broadcast -a com.android.systemui.demo -e command battery -e level 100
  $ADB shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4
  $ADB shell am broadcast -a com.android.systemui.demo -e command network -e mobile show -e datatype none -e level 4
  $ADB shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false
elif [ $CMD == "off" ]; then
  $ADB shell am broadcast -a com.android.systemui.demo -e command exit
fi
