#!/bin/bash
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
