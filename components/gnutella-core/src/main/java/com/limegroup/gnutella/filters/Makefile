javac=javac -O

classes = DuplicateFilter.class KeywordFilter.class

all : $(classes)

$(classes) : %.class: %.java
	$(javac) $<
