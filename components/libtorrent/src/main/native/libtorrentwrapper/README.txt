
#building libtorrent on windows
bjam --toolset=gcc boost=source link=shared variant=release character-set=unicode invariant-checks=off debug-symbols=off define=IPV6_PROTECTION_LEVEL define=PROTECTION_LEVEL_UNRESTRICTED=30

#building libtorrent on linux
bjam --toolset=gcc boost=source link=shared variant=release character-set=unicode invariant-checks=off debug-symbols=off define=IPV6_PROTECTION_LEVEL define=PROTECTION_LEVEL_UNRESTRICTED=30









