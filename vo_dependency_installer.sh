#!/bin/bash

#=======================================
init_brew (){
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
do_uninstall () {
echo "

>>>>> uninstall <<<<<"
	echo "uninstall usbmuxd, libplist, libimobiledevice, ifuse"
	brew uninstall --ignore-dependencies usbmuxd 2>/dev/null
	brew uninstall --ignore-dependencies libplist 2>/dev/null
	brew uninstall --ignore-dependencies libimobiledevice 2>/dev/null
	brew uninstall --ignore-dependencies ifuse 2>/dev/null
}

#=======================================
compile_libimobiledevice-glue () {
echo "

>>>>> compile_libimobiledevice-glue <<<<<"

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
install_libimobiledevice () {
echo "

>>>>> install_libimobiledevice <<<<<"
	
	brew uninstall --ignore-dependencies libimobiledevice 2>/dev/null
	
	brew install --HEAD libimobiledevice
	brew unlink libimobiledevice && brew link libimobiledevice
}

#=======================================
# install ifuse
# plus openssl
# on intel openssl requires non-head version of libimobiledevice
install_ifuse () {
echo "

>>>>> install_ifuse <<<<<"

# install dependencies
	# 1 libimobiledevice non-head version for openssl
	# 2 openssl
	
	if [ "${ARCH_NAME}" = "x86_64" ]; then
		brew install libimobiledevice 
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
	sudo make install
}

#=======================================
do_install () {

echo "
====>>>>> do_install <<<<<====
"
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
	install_libimobiledevice

}

#=======================================
exit_install (){
	exit 0
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