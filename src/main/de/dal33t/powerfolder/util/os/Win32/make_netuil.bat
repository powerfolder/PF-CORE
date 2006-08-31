javah -classpath ../../../../../../ de.dal33t.powerfolder.util.os.Win32.NetworkHelperImpl
gcc -Wall -D_JNI_IMPLEMENTATION_ -IC:/JNI/include -IC:/JNI/include/win32 -c netutil.c
gcc -Wl,--kill-at netutil.o -shared -o netutil.dll -lIphlpapi
strip --strip-all netutil.dll
