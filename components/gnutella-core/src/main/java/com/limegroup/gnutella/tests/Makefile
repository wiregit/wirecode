javac=javac -g

classes = StressTester.class \
	RouteClient.class RouteServer.class FakeConnection.class \
	SpamTester.class \
	BadClient.class BadServer.class \
	MessageTimeouts.class RoutingTester.class

all : $(classes)

$(classes) : %.class: %.java
	$(javac) $<
