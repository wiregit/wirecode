javac=javac -O

classes = DuplicateFilter.class KeywordFilter.class CompositeFilter.class \
	BlackListFilter.class AllowFilter.class GreedyQueryFilter.class

all : $(classes)

$(classes) : %.class: %.java
	$(javac) $<
