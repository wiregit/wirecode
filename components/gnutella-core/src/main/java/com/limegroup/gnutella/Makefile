#Makefile for LimeWire
javac=javac -O

classes = Main.class ConnectionManager.class RouteTable.class \
	Connection.class BadPacketException.class GUID.class \
	UnmodifiableIterator.class Assert.class \
	Message.class PingRequest.class PingReply.class \
	QueryRequest.class QueryReply.class Response.class \
	PushRequest.class ByteOrder.class HostCatcher.class \
	ForgetfulHashMap.class Endpoint.class \
	SettingsInterface.class SettingsManager.class \


all : $(classes) gui networkdiscoverer

$(classes) : %.class: %.java
	$(javac) $<

#Recursively call make
.PHONY: gui
gui:
	$(MAKE) -C gui
# .PHONY: doc
# doc:
# 	cd ../../..
# 	javadoc170 -d doc/api com.limegroup.gnutella"
