javac=javac -O

classes = Client.class Server.class \
    BadClient.class BadServer.class \
	RoutingTester.class \
    AddressTest.class \
    RecorderMessageRouter.class MessageSimulator.class TimeAndMessage.class

all : $(classes)

$(classes) : %.class: %.java
	$(javac) $<
