javac=javac -O

classes = StressTester.class \
	RouteClient.class RouteServer.class FakeConnection.class \
	SpamTester.class \
	BadClient.class BadServer.class \
	MessageTimeouts.class

all : $(classes)

$(classes) : %.class: %.java
	$(javac) $<
