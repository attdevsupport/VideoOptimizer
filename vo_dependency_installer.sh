#!/bin/bash

#=======================================
# Copyright 2022, 2023 AT&T
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
VERSION="1.0.1.1"
#
# Howto use this script:
#   From your browser, select 'Save' and choose or create an empty folder. Make sure the script has the extension '.sh'.
#   Safari defaults to '.txt' so be sure the change it.
# 
#   Open a terminal in the folder containing the script.
#   Launch the script with './vo_dependency_installer.sh'
#   At one point you will be prompted for your sudo password,
#   this will be to completely remove any old version of ifuse
#   DO NOT LAUNCH THIS INSTALL USING SUDO
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
# 1 You must have and run this under an Admin account, do not launch under 'sudo'
# 2 macOS 13.4 or 13.4.1
# 3 Xcode 14.3.1
# 4 brew (aka HomeBrew https://brew.sh) 
# 
# Testing:
# This script has been tested on Macbook Pro intel and M1 machines running Monterey 12.6 and Ventura early December 2022
# 	Ventura 13.2.1 Mar 31, 2023
#   Ventura 13.4.1, Intel & M1 July 18 2023
# 
#=======================================
function init_brew (){
echo "

>>>>> init_brew <<<<<"
	if [ -f "`which brew`" ]
	then
		brew update
		brew upgrade
				
		# make sure we can compile
		brew install cmake
		brew install automake
		brew install autogen
		brew install autoconf
		
	else
		echo "failed to find brew" >> error
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
	brew uninstall --ignore-dependencies openssl 2>/dev/null
	sudo rm `which ifuse` 2>/dev/null
}

#=======================================
function compile_libimobiledevice-glue () {
echo ""
echo ">>>>> compile_libimobiledevice-glue <<<<<"

	# install dependencies
	# 1 libplist non-head version
	
	brew install --head libplist

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
	
	brew install --head usbmuxd
# 	brew install --head libplist
	brew install --head libimobiledevice
	brew unlink libimobiledevice && brew link libimobiledevice
}

#=======================================
# install ifuse
# plus openssl
function install_ifuse () {
echo ""
echo ">>>>> install_ifuse <<<<<"

# install dependencies
	# 1 libimobiledevice non-head version for openssl
	# 2 openssl
	
	install_libimobiledevice
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
	echo "$0 $VERSION"
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
}
#=======================================
function exit_install (){
	exit 0
}

#----------------------------
# Brew config version checks
#----------------------------

#=================================
# regex extraction from target data
# compares version Major.SUB_1.SUB_2
# example regex pattern:
#   regex="(macOS): ([0-9]*)\.([0-9]*)\.([0-9]*)\-"
#
# args
# 	1: <$1> target data
# 	2: <$2> regex pattern
# 	3: <$3> label 
# 	4: <$4> major version
# 	5: <$5> sub:1 version
# 	6: <$6> sub:1 version
#   7: <$7> full version
function check_version () {

	txt=$1
	rgx=$2
	label=$3
	M1=$4
	M2=$5
	M3=$6
	F_VERSION=$7

# 	echo "[check $label]"	
	if [[ "$txt" =~ $rgx ]]
	then
		V_CHECK="pass"
		if [ ${BASH_REMATCH[2]} \< $M1 ] \
			|| [[ $M2 != "" && ${BASH_REMATCH[3]} < $M2 ]] \
			|| [[ $M3 != "" && ${BASH_REMATCH[4]} < $M3 ]]
		then
			if [ "${BASH_REMATCH[2]}" == "N/A" ]
			then
				CHECK="fail:$label needs to be installed"
				echo $CHECK
			else
				CHECK="fail:${BASH_REMATCH[1]} version needs updating to at least $F_VERSION"
				echo $CHECK
			fi
			V_CHECK="fail"
		else
			V_CHECK="PASS"
		fi
	else
		# look for "N/A"
		rgx="($label): ([0-9A-Za-z/]*)"
		if [[ "$txt" =~ $rgx ]]
		then
			if [ ${BASH_REMATCH[2]} == "N/A" ]
	    	then
				CHECK="fail:$label needs to be installed, Please review installations of brew, Xcode and Xcode's commandline tools"
	    		echo $CHECK
				V_CHECK="fail"
	    		return
			fi
		fi
		echo "Unable to retrieve 'brew config' results"
		echo "Please review installations of brew, Xcode and Xcode's commandline tools"
	fi
}

function machine_precheck (){

	if [[ `which brew` == "" ]];then
		echo "brew is not installed"
		echo "To install, please visit: https://brew.sh"
		CHECK="brew is not installed, To install, please visit: https://brew.sh"
		return
	else
		BREW_CONFIG=`brew config`
	fi

	if [[ -d /Library/PreferencePanes/macFUSE.prefPane ]]; then
		echo "MacFuse is installed"
	else
		echo "MacFuse not detected"
		echo "To install, visit https://osxfuse.github.io"
		CHECK="fail:MacFuse not detected"
	fi

	# macOS: 13.2.1-arm64
	regex="(macOS): ([0-9]*)\.([0-9]*)\.([0-9]*)\-"
	check_version "$BREW_CONFIG" "$regex" macOS 13 2 1 13.2.1

	# CLT: 14.2.0.0.1.1668646533
	regex="(CLT): ([0-9A-Za-z/]*)\.([0-9]*)\.([0-9]*)"
	check_version "$BREW_CONFIG" "$regex" CLT 14 3 0 14.2.0 "CLT: 14.3.0.0.1.1668646533"

	# Xcode: 14.1
	regex="(Xcode): ([0-9]*)\.([0-9]*)"
	check_version "$BREW_CONFIG" "$regex" Xcode 14 3 "" "Xcode: 14.3"
	echo $CHECK

	if [[ ${CHECK} = "fail"* ]] ; then
		if [[ $CHECK == *"CLT"* ]] ; then
			echo "verify active developer directory with 'xcode-select -p'"
			echo "use 'xcode-select --install' to install the correct commandline tools for your Xcode installation"
		fi
	else
		echo "ready to proceed\!"
	fi
	
}

#-----------------------------------------------------
# vo_dependency_installer.sh
#-----------------------------------------------------

if [ "`id -u`" = "0" ]; then
	echo "vo_dependency_installer.sh cannot be launched under sudo or from the root user"
	exit 0
fi 

ARCH_NAME=`uname -m`
PTH=`pwd |grep " "`
if [ ! "$PTH" = "" ]; then
	echo "The current path <`pwd`> has a space in it."
	echo "This can cause problems during installation."
	echo "Please launch $0 from an other location where there are no spaces in the path"
	exit_install
fi

machine_precheck > error.log

test=`cat error.log|grep "ready to proceed"`

if [[ "$test" != "ready to proceed"* ]];then
	cat error.log
	echo ""
	echo "Aborting $0, no changes to your system"
	exit_install
else
	rm error.log
	echo $test
fi

#-----------------------------------------------------
# version pre-checks DONE
#   will now configure system
#-----------------------------------------------------

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
	echo "Please exit the folder 'libimobile_installation'"
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
