javac=javac -O

classes = ForgetfulHashMap.class UnmodifiableIterator.class Buffer.class \
	IllegalArgumentException2.class

all : $(classes)

$(classes) : %.class: %.java
	$(javac) $<
