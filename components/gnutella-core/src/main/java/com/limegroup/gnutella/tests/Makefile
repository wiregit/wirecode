javac=javac -g

classes = Client.class Server.class \
    BadClient.class BadServer.class \
	RoutingTester.class \
    AddressTest.class \
    RecorderMessageRouter.class MessageSimulator.class TimeAndMessage.class \
	ReceivesOnly.class SendsOnly.class \
	HostCatcherTest.class \
	ActivityCallbackStub.class MessageRouterStub.class

all : $(classes)

$(classes) : %.class: %.java
	$(javac) $<
