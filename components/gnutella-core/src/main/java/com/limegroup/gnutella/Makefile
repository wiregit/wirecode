# Makefile for LimeWire.
# Requires Gnu Make.

# the java compiler
JAVAC  = javac 
RM      = /bin/rm -rf

# grab all sources - kind of hacky
SRCS := $(wildcard *.java)
# our object files
OBJS  = $(SRCS:.java=.class)

# make everything, (just makes .class files, for now)
all: $(OBJS) gui util filters

$(OBJS): %.class: %.java
	$(JAVAC) $<

# make subdirectories.  TODO: can you make this more general?
.PHONY: gui
gui:
	$(MAKE) -C $@
.PHONY: util
util:
	$(MAKE) -C $@
.PHONY: filters
filters:
	$(MAKE) -C $@

# clean and then make again
again:  clean
	$(JAVAC) *.java

# clean up after making - remove core, ~ files, and .bak files
clean:
	$(RM) $(OBJS) core *~ *.bak
