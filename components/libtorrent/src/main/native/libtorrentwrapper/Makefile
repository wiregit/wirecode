CXXFLAGS =	-O2 -g -Wall -fmessage-length=0

OBJS =		libtorrentwrapper.o 

LIBS = torrent-rasterbar

TARGET =	libtorrentwrapper.out

LIBRARY = libtorrentwrapper.so

VPATH = src

libtorrentwrapper.o: 
	${CXX} -c src/libtorrentwrapper.cpp -I/home/pvertenten/Desktop/libtorrent-rasterbar-0.14.2/include -L ./lib -l $(LIBS)
	${CXX} src/libtorrentwrapper.cpp -I/home/pvertenten/Desktop/libtorrent-rasterbar-0.14.2/include -L ./lib -l $(LIBS)
	
$(TARGET):	$(OBJS)
	$(CXX) -o $(TARGET) $(OBJS) -I /home/pvertenten/Desktop/libtorrent-rasterbar-0.14.2/include -L ./lib -l $(LIBS)
	

compile:	$(TARGET)

all: library

library: compile
	$(CXX) -shared -o libtorrentwrapper.so $(OBJS) -L ./lib -l $(LIBS)

clean:
	rm -f $(OBJS) $(TARGET) $(LIBRARY) a.out
