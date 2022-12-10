#!/bin/bash

#=======================================
# Copyright 2022 AT&T
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
#======================================

#=======================================
# vo_dependency_installer.sh
# version 1.0.0.1
#
# Howto use this script:
#   From your browser, select 'Save' and choose or create an empty folder. Make sure the script has the extension '.sh'.
#   Safari defaults to '.txt' so be sure the change it.
# 
#   Open a terminal in the folder containing the script.
#   Launch the script with './vo_dependency_installer.sh'
#   At one point you will be prompted for your sudo password
#   There are two instances of 'sudo make install'
# =======================================
#
#  This bash shell script will install needed dependancies
# for VideoOptimizer on Macintosh computers.
# This script will run some checks of versions before proceding.
# Conditions can change with each update to operating systems and other software.
# 
# It is advised that the computer is backed up, and important data is safely stored.
# 
# The dependancies that are required include some 'HEAD' versions, which are not yet 
# considered to be stable. 'libimobiledevice' is installed under HEAD, which may bring 
# other dependancies, possibly also HEAD versions.
# Some of the depenancies do not have functional formula to allow 'brew' to install them, 
# and there for must be cloned from github and compiled.
# 
# These include 'ifuse' and 'libimobiledevice-glue'
# see:
#   git clone https://github.com/libimobiledevice/ifuse.git
#   git clone https://github.com/libimobiledevice/libimobiledevice-glue.git
# 
# There are some basic requirements before this script can be run.
# 1 You must have and run this under an Admin account, do not run under 'sudo'
# 1 macOS 12.6.1
# 2 Xcode 14.1
# 3 brew (aka HomeBrew https://brew.sh) 
# 
# Testing:
# This script has been tested on Macbook Pro intel and M1 machines running Monterey 12.6 and Ventura early December 2022
# 
# 
#=======================================
function init_brew (){
echo "

>>>>> init_brew <<<<<"
	if [ -f "`which brew`" ]
	then
		brew update
		brew upgrade
	else
		echo "failded to find brew" >> error
		exit_install
	fi
}

#=======================================
function do_uninstall () {
echo ""
echo ">>>>> uninstall <<<<<"
	echo "uninstall usbmuxd, libplist, libimobiledevice, ifuse"
	brew uninstall --ignore-dependencies usbmuxd 2>/dev/null
	brew uninstall --ignore-dependencies libplist 2>/dev/null
	brew uninstall --ignore-dependencies libimobiledevice 2>/dev/null
	brew uninstall --ignore-dependencies ifuse 2>/dev/null
}

#=======================================
function compile_libimobiledevice-glue () {
echo ""
echo ">>>>> compile_libimobiledevice-glue <<<<<"

	# install dependencies
	# 1 libplist non-head version
	
	brew install libplist

	if [ "${PWD##*/}" = "libimobiledevice-glue" ]; then
		if [ "${ARCH_NAME}" = "x86_64" ]; then
			touch "__compile_x86"
			echo "./autogen.sh --prefix=/usr/local"
 			./autogen.sh --prefix=/usr/local
		else
			touch "__compile_aarch"
			echo "./autogen.sh --prefix=/opt/homebrew"
	 		./autogen.sh --prefix=/opt/homebrew
		fi 
		make
		echo "sudo make install"
		sudo make install
	
		if [ "${ARCH_NAME}" = "x86_64" ]; then
			#For Intel / x86-64 machines - 
			ln -s /usr/local/lib/pkgconfig/libimobiledevice-glue-1.0.pc /usr/local/opt/libplist/lib/pkgconfig/libimobiledevice-glue-1.0.pc 
		else
			#For M1 / aach64 machines - 
			ln -s /opt/homebrew/lib/pkgconfig/libimobiledevice-glue-1.0.pc /opt/homebrew/opt/libplist/lib/pkgconfig/libimobiledevice-glue-1.0.pc  
		fi
	
	else
		echo "libimobiledevice-glue failure, in wrong path: `pwd`"
		exit_install
	fi
}

#=======================================
function install_libimobiledevice () {
echo ""
echo ">>>>> install_libimobiledevice <<<<<"
	
	brew uninstall --ignore-dependencies libimobiledevice 2>/dev/null
	
	brew install --HEAD libimobiledevice
	brew unlink libimobiledevice && brew link libimobiledevice
}

#=======================================
# install ifuse
# plus openssl
# on intel openssl requires non-head version of libimobiledevice
function install_ifuse () {
echo ""
echo ">>>>> install_ifuse <<<<<"

# install dependencies
	# 1 libimobiledevice non-head version for openssl
	# 2 openssl
	
	if [ "${ARCH_NAME}" = "x86_64" ]; then
		brew install libimobiledevice
	else
		install_libimobiledevice
	fi
	brew install openssl
	
	LISTING=$(brew list openssl)
	regex="(^.*)(/Cellar.*@[0-9]{0,9}/)(.*)/bin/openssl"

	PATH_OSSL=""
	VERSION_OSSL=""
	for f in $LISTING
	do
	# 	echo $f
		if [[ "$f" =~ $regex ]]
		then
			echo "found in <<$f>>"
			PATH_OSSL="${BASH_REMATCH[1]}"
			MID="${BASH_REMATCH[2]}"
			VERSION_OSSL="${BASH_REMATCH[3]}"
			break
		fi
	done
	
	echo "==============================="
	echo "version = $VERSION_OSSL"
	echo "path = $PATH_OSSL"
	echo "mid = $MID"
	
	if [ -f $PATH_OSSL$MID$VERSION_OSSL/lib/pkgconfig/openssl.pc ] 
	then
		ln -sF $PATH_OSSL$MID$VERSION_OSSL/lib/pkgconfig/openssl.pc $PATH_OSSL/lib/pkgconfig/openssl.pc
		ln -sF $PATH_OSSL$MID$VERSION_OSSL/lib/pkgconfig/libssl.pc $PATH_OSSL/lib/pkgconfig/libssl.pc
		ln -sF $PATH_OSSL$MID$VERSION_OSSL/lib/pkgconfig/libcrypto.pc $PATH_OSSL/lib/pkgconfig/libcrypto.pc
	else
		echo "install_ifuse (): failed to symlink openSSL" >> error
		exit_install
	fi
	
	# now compile ifuse
	./autogen.sh  
	make
	echo "sudo make install"
	sudo make install
}

#=======================================
function do_install () {

echo ""
echo "====>>>>> do_install <<<<<===="
echo ""

	echo "Archetecture $ARCH_NAME"
	
	echo "============================ prepare ============================"
	if [[ -d /Library/PreferencePanes/macFUSE.prefPane ]]; then
	  init_brew
	  do_uninstall
	else
	  echo "MacFuse not detected"
	  echo "To install, visit https://osxfuse.github.io"
	fi

	echo "============================ libimobiledevice-glue ============================"
	rm -fr libimobiledevice-glue
	git clone https://github.com/libimobiledevice/libimobiledevice-glue.git
	# unzip ../bu/libimobiledevice-glue.zip -d ./ ; rm -fr __MACOSX
	if [ -d libimobiledevice-glue ]; then
		( cd libimobiledevice-glue && compile_libimobiledevice-glue )
	else
		echo "clone: failed to git clone https://github.com/libimobiledevice/libimobiledevice-glue.git" >> error
		exit_install
	fi

	echo "============================ compile iFuse ============================"
	rm -fr ifuse
	git clone https://github.com/libimobiledevice/ifuse.git
	# unzip ../bu/ifuse.zip -d ./ ; rm -fr __MACOSX
	if [ -d ifuse ]; then
		( cd ifuse && install_ifuse )
	else
		echo "clone: failed to git clone https://github.com/libimobiledevice/ifuse.git" >> error
		exit_install
	fi

	echo "============================ libimobiledevice HEAD version ============================"
	if [ "${ARCH_NAME}" = "x86_64" ]; then
		install_libimobiledevice
	fi
}

#=======================================
function exit_install (){
	exit 0
}

function machine_precheck (){

	if [[ `which brew` == "" ]];then
		echo "brew is not installed"
		echo "To install, please visit: https://brew.sh"
		exit 0
	fi

	BREW_CONFIG=`brew config`
	
	regex="(macOS: ([0-9]*)\.([0-9]*)\.([0-9]*))\-.+(CLT: ([0-9]*)\.([0-9]*)\.([0-9]*)).+(Xcode: ([0-9]*)\.([0-9]*))"

	if [[ "$BREW_CONFIG" =~ $regex ]]
	then
		FAIL=false
		echo ""
		echo "check "${BASH_REMATCH[1]}
	
		if [ ${BASH_REMATCH[2]} \< 13 ]
		then
			if [ ${BASH_REMATCH[2]} \< 12 ] || [ ${BASH_REMATCH[3]} \< 6 ]
			then 
				echo "${BASH_REMATCH[1]} version needs updating to at least 12.6.1"
				FAIL=true
			fi
		fi
	
		echo "check "${BASH_REMATCH[5]}
		if [ ${BASH_REMATCH[6]} \< 15 ] ; then
			if [ ${BASH_REMATCH[6]} \< 14 ] || [ ${BASH_REMATCH[7]} \< 1 ] ; then 
				echo "${BASH_REMATCH[5]} version needs updating to at least 14.1"
				FAIL=true
			fi
		fi

		echo "check "${BASH_REMATCH[9]}
		if [ ${BASH_REMATCH[10]} \< 15 ] ; then
			if [ ${BASH_REMATCH[10]} \< 14 ] || [ ${BASH_REMATCH[11]} \< 1 ] ; then 
				echo "${BASH_REMATCH[9]} version needs updating to at least 14.1"
				FAIL=true
			fi
		fi
	
		if [ $FAIL == true ] ; then
			echo "This configuration cannot complete unless the above updates have been performed"
		else
			echo "ready to proceed!"
		fi
	else
		echo "Unable to retrieve 'brew config' results"
		echo "Please review installations of brew, Xcode and Xcode's commandline tools"
	fi
}
#-----------------------------------------------------
# vo_installer.sh
#-----------------------------------------------------

ARCH_NAME=`uname -m`
PTH=`pwd |grep " "`
if [ ! "$PTH" = "" ]; then
	echo "The current path <`pwd`> has a space in it."
	echo "This can cause problems during installation."
	echo "Please launch $0 from an other location where there are no spaces in the path"
	exit_install
fi

machine_precheck > error.log

test=`cat error.log|grep "ready to proceed\!"`

if [ "$test" == "" ];then
	cat error.log
	exit 0
else
	rm error.log
	echo $test
fi

if [ ! "${PWD##*/}" = "libimobile_installation" ]
then
	if [ ! -d libimobile_installation ]
	then
		echo "create libimobile_installation"
		mkdir "libimobile_installation"
	fi
	if [ -d libimobile_installation ]
	then
		echo "cd into libimobile_installation folder and launch do_install"
		( cd libimobile_installation  && (do_install | tee vo_install.log))
		exit_install
	else
		echo "$0: Failed to create the temporary directory 'libimobile_installation'"
		exit_install
	fi
	# 
else
	echo "I am here `pwd`"
	exit_install
fi

if [ -f "./error" ]
then
	cat error
	exit_install
else
	echo "No errors"
	rm -fr libimobile_installation
fi
