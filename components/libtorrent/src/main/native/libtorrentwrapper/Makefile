CXXFLAGS =	-O2 -g -Wall -fmessage-length=0 

OBJS =		libtorrentwrapper.o 

LIB_DIR = ./lib
#LIBS = -l torrent-rasterbar
LIBS = -l ws2_32 -l torrent

TARGET =	libtorrentwrapper.exe
#TARGET =	libtorrentwrapper.out

LIBRARY = libtorrentwrapper.dll
#LIBRARY = libtorrentwrapper.so

VPATH = src

INCLUDE_DIR = ./include 
#INCLUDE_DIR = /home/pvertenten/Desktop/libtorrent-rasterbar-0.14.2/include

WINDOWS = "c:/Program Files/Microsoft Visual Studio 9.0/VC/include"

libtorrentwrapper.o: 
	${CXX} -c src/libtorrentwrapper.cpp -DIPV6_PROTECTION_LEVEL -I $(INCLUDE_DIR)
	
$(TARGET):	$(OBJS)
	$(CXX) -o $(TARGET) $(OBJS) -L $(LIB_DIR) $(LIBS)
	

compile:	$(TARGET)

all: library

library: compile
	$(CXX) -shared -o $(LIBRARY) $(OBJS) -L $(LIB_DIR) $(LIBS)

clean:
	rm -f $(OBJS) $(TARGET) $(LIBRARY)

