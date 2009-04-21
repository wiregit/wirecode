CXXFLAGS =	-O2 -g -Wall -fmessage-length=0 

OBJS =		libtorrentwrapper.o 

#LIBS = -l torrent-rasterbar
LIBS = -l ws2_32 -l torrent
TARGET =	libtorrentwrapper.exe


LIBRARY = libtorrentwrapper.dll
#LIBRARY = libtorrentwrapper.so

VPATH = src

LIBTORRENT = c:/libtorrent-rasterbar-0.14.2/include 
#LIBTORRENT = /home/pvertenten/Desktop/libtorrent-rasterbar-0.14.2/include
BOOST = c:/boost_1_36_0

WINDOWS = "c:/Program Files/Microsoft Visual Studio 9.0/VC/include"

libtorrentwrapper.o: 
	${CXX} -c src/libtorrentwrapper.cpp -DIPV6_PROTECTION_LEVEL -I $(LIBTORRENT) -I $(BOOST)
	
$(TARGET):	$(OBJS)
	$(CXX) -o $(TARGET) $(OBJS) -L ./lib $(LIBS)
	

compile:	$(TARGET)

all: library

library: compile
	$(CXX) -shared -o $(LIBRARY) $(OBJS) -L ./lib $(LIBS)

clean:
	rm -f $(OBJS) $(TARGET) $(LIBRARY) a.out
