
#building libtorrent on windows
export BOOST_ROOT=xxx
bjam boost=source link=shared variant=release character-set=unicode invariant-checks=off debug-symbols=off --without-python
bjam boost=source link=static variant=release character-set=unicode invariant-checks=off debug-symbols=off --without-python
bjam link=shared variant=release --without-python
bjam link=static variant=release --without-python

#building libtorrent on linux
export BOOST_ROOT=xxx
bjam --toolset=gcc boost=source link=static variant=release character-set=unicode invariant-checks=off debug-symbols=off

#building libtorrent on osx
export BOOST_ROOT=xxx
export CXXFLAGS=" -arch x86_64"
export LDFLAGS=" -arch x86_64"
bjam --toolset=darwin boost=source link=static variant=release character-set=unicode invariant-checks=off debug-symbols=off architecture=x86 address-model=64










