javac=javac -O

classes = Client.class Server.class \
          BadClient.class BadServer.class \
	      RoutingTester.class

all : $(classes)

$(classes) : %.class: %.java
	$(javac) $<
