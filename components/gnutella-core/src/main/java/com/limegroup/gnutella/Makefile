javac=C:/jdk1.2.2/bin/javac -g

classes = Main.class Router.class RouteTable.class \
	Connection.class BadPacketException.class GUID.class \
	UnmodifiableIterator.class Assert.class \
	Message.class PingRequest.class PingReply.class \
	QueryRequest.class QueryReply.class Response.class \
	PushRequest.class ByteOrder.class

all : $(classes)

$(classes) : %.class: %.java
	$(javac) $<

# doc:
# 	cd p:/crohrs
# 	C:\progra~1\Cygwin\bin\sh.exe  -c " ./javadoc170 -d doc/api com.limegroup.gnutella"

# skip:
