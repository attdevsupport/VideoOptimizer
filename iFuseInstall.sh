#!/bin/bash

# Copyright 2021 AT&T
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

init_brew (){
  brew update
  brew upgrade
}

uninstall () {
	echo "uninstall usbmuxd, libplist, libimobiledevice, ifuse"
	brew uninstall --ignore-dependencies usbmuxd 2>/dev/null
	brew uninstall --ignore-dependencies libplist 2>/dev/null
	brew uninstall --ignore-dependencies libimobiledevice 2>/dev/null
	brew uninstall --ignore-dependencies ifuse 2>/dev/null
}

install_ifuse () {
  FORMULA=$(brew formula ifuse)
  echo "patching $FORMULA"
  sed 's/^\( *disable!\)/#\1/' $FORMULA > $FORMULA"TEMP"
  mv $FORMULA"TEMP" $FORMULA
  brew install ifuse
}

# main
if [[ -d /Library/PreferencePanes/macFUSE.prefPane ]]; then
  init_brew
  uninstall
  install_ifuse
else
  echo "MacFuse not detected"
  echo "To install, visit https://osxfuse.github.io"
fi
