#!/bin/bash
init_brew (){
  brew update
  brew upgrade
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
  install_ifuse
else
  echo "MacFuse not detected"
  echo "To install, visit https://osxfuse.github.io"
fi
