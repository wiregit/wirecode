CXXFLAGS =	-O2 -g -Wall -fmessage-length=0 

OBJS =		libtorrentwrapper.o 

#LIBS = -l torrent-rasterbar
LIBS = -l ws2_32 -l torrent -l boost_date_time-mgw34-mt-1_36 -l boost_filesystem-mgw34-mt-1_36 -l boost_system-mgw34-mt-1_36 -l boost_thread-mgw34-mt-1_36
TARGET =	libtorrentwrapper.out


LIBRARY = libtorrentwrapper.dll
#LIBRARY = libtorrentwrapper.so

VPATH = src

LIBTORRENT = c:/libtorrent-rasterbar-0.14.2/include 
#LIBTORRENT = /home/pvertenten/Desktop/libtorrent-rasterbar-0.14.2/include
BOOST = c:/boost_1_36_0

WINDOWS = "c:/Program Files/Microsoft Visual Studio 9.0/VC/include"
WINDOWSPLATFORM = "c:/Program Files/Microsoft SDKs/Windows/v6.0A/Lib"

libtorrentwrapper.o: 
	${CXX} -c src/libtorrentwrapper.cpp -DIPV6_PROTECTION_LEVEL -I $(LIBTORRENT) -I $(BOOST)
	
$(TARGET):	$(OBJS)
	$(CXX) -o $(TARGET) $(OBJS) -L ./lib -L $(WINDOWSPLATFORM) $(LIBS)
	

compile:	$(TARGET)

all: library

library: compile
	$(CXX) -static -o $(LIBRARY) $(OBJS) -L ./lib -L $(WINDOWSPLATFORM) $(LIBS)

clean:
	rm -f $(OBJS) $(TARGET) $(LIBRARY) a.out
