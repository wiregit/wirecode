javac=javac -g

classes = Main.class ConnectionManager.class RouteTable.class \
	Connection.class BadPacketException.class GUID.class \
	UnmodifiableIterator.class Assert.class Const.class \
	Message.class PingRequest.class PingReply.class \
	QueryRequest.class QueryReply.class Response.class \
	PushRequest.class ByteOrder.class HostCatcher.class

all : $(classes)

$(classes) : %.class: %.java
	$(javac) $<

# doc:
# 	cd ../../..
# 	javadoc170 -d doc/api com.limegroup.gnutella"

# skip:
