javac=javac -O

classes = ForgetfulHashMap.class UnmodifiableIterator.class Buffer.class

all : $(classes)

$(classes) : %.class: %.java
	$(javac) $<
