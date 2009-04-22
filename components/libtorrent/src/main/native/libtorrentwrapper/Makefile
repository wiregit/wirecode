CXXFLAGS =	-O2 -g -Wall -fmessage-length=0 


#TODO make OS variable dynamic
OS = "Linux"

ifeq ($(OS),Windows)
	LIB_DIR = ./lib/windows
    LIBS = -l ws2_32 -l torrent
    LIBRARY = torrent-wrapper.dll
    TARGET = libtorrent-wrapper.exe
    OPTIONS = -DIPV6_PROTECTION_LEVEL -DWINDOWS
endif

ifeq  ($(OS),"Linux")
	LIB_DIR = ./lib/linux
	LIBS = -l ssl -l rt -l torrent-0_14_2 -l boost_date_time-gcc42-mt-1_38 -l boost_filesystem-gcc42-mt-1_38 -l boost_system-gcc42-mt-1_38 -l boost_thread-gcc42-mt-1_38
	LIBRARY = libtorrent-wrapper.so
	TARGET = libtorrent-wrapper.out
endif

ifeq  ($(OS),Mac)
	LIB_DIR = ./lib/mac
	LIBS = -l torrent-rasterbar
	LIBRARY = libtorrent-wrapper.so
	TARGET = libtorrent-wrapper.out
endif


ifndef LIBS
    $(error "Unsupported OS: $(OS)")
endif

OBJS = libtorrentwrapper.o
VPATH = src
INCLUDE_DIR = ./include 

libtorrentwrapper.o: 
	${CXX} -c src/libtorrentwrapper.cpp $(OPTIONS) -I $(INCLUDE_DIR)
	

	

compile:    libtorrentwrapper.o

all: library

library: compile
	$(CXX) -shared -o $(LIBRARY) $(OBJS) -L $(LIB_DIR) $(LIBS)

clean:
	rm -f $(OBJS) $(TARGET) $(LIBRARY) a.out

