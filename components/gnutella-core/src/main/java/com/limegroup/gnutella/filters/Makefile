javac=javac -O

classes = DuplicateFilter.class KeywordFilter.class CompositeFilter.class \
	BlackListFilter.class

all : $(classes)

$(classes) : %.class: %.java
	$(javac) $<
