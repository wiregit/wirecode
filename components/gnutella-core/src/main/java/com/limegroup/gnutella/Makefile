#Makefile for LimeWire
javac=javac -g

classes = Main.class ConnectionManager.class RouteTable.class \
	Connection.class BadPacketException.class GUID.class \
	Assert.class \
	Message.class PingRequest.class PingReply.class \
	QueryRequest.class QueryReply.class Response.class \
	PushRequest.class ByteOrder.class HostCatcher.class \
	Endpoint.class \
	SettingsInterface.class SettingsManager.class \
	HTTPManager.class HTTPUploader.class HTTPDownloader.class HTTPUtil.class \
	SpamFilter.class \
	RouterService.class ResponseVerifier.class \
	MediaType.class \
	FileManager.class RejectConnection.class \
	ConnectionWatchdog.class Acceptor.class

subpackages = util gui filters networkdiscoverer

all : $(classes) $(subpackages)

$(classes) : %.class: %.java
	$(javac) $<

#Recursively call make
.PHONY: gui
gui:
	$(MAKE) -C gui

.PHONY: util
util:
	$(MAKE) -C util

.PHONY: filters
filters:
	$(MAKE) -C filters

# to remove all class files
clean: 
	rm -rf *.class; rm -rf */*.class


# .PHONY: doc
# doc:
# 	cd ../../..
# 	javadoc170 -d doc/api com.limegroup.gnutella"
