CXXFLAGS =	-O2 -g -Wall -fmessage-length=0 


#TODO make OS variable dynamic
OS = "Linux"

ifeq ($(OS),Windows)
    LIBS = -l ws2_32 -l torrent
    LIBRARY = torrent-wrapper.dll
    TARGET = libtorrent-wrapper.exe
    OPTIONS = -DIPV6_PROTECTION_LEVEL -DWINDOWS
endif

ifeq  ($(OS),"Linux")
	LIBS = -l torrent-rasterbar
	LIBRARY = libtorrent-wrapper.so
	TARGET = libtorrent-wrapper.out
endif

ifeq  ($(OS),Mac)
	LIBS = -l torrent-rasterbar
	LIBRARY = libtorrent-wrapper.so
	TARGET = libtorrent-wrapper.out
endif


ifndef LIBS
    $(error "Unsupported OS: $(OS)")
endif

OBJS = libtorrentwrapper.o
VPATH = src
LIB_DIR = ./lib
INCLUDE_DIR = ./include 

libtorrentwrapper.o: 
	${CXX} -c src/libtorrentwrapper.cpp $(OPTIONS) -I $(INCLUDE_DIR)
	
$(TARGET):	$(OBJS)
	$(CXX) -o $(TARGET) $(OBJS) -L $(LIB_DIR) $(LIBS)
	

compile:	$(TARGET)

all: library

library: compile
	$(CXX) -shared -o $(LIBRARY) $(OBJS) -L $(LIB_DIR) $(LIBS)

clean:
	rm -f $(OBJS) $(TARGET) $(LIBRARY)

