
#building libtorrent on windows
bjam --toolset=gcc boost=source link=shared variant=release character-set=unicode invariant-checks=off debug-symbols=off define=IPV6_PROTECTION_LEVEL define=PROTECTION_LEVEL_UNRESTRICTED=30

#building libtorrent on linux
bjam --toolset=gcc boost=source link=static variant=release character-set=unicode invariant-checks=off debug-symbols=off

#building libtorrent on osx
export CXXFLAGS=" -arch x86_64"
export LDFLAGS=" -arch x86_64"
bjam --toolset=darwin boost=source link=static variant=release character-set=unicode invariant-checks=off debug-symbols=off architecture=x86 address-model=64










