# Non-recursive makefile.


# the java compiler
JAVAC  = javac 
RM      = /bin/rm -rf

# grab all sources - kind of hacky
SRCS := $(wildcard *.java)
# our object files
OBJS  = $(SRCS:.java=.class)

# make everything, (just makes .class files, for now)
all: $(OBJS)

$(OBJS): %.class: %.java
	$(JAVAC) $<

# clean and then make again
again:  clean all

# clean up after making - remove core, ~ files, and .bak files
clean:
	$(RM) $(OBJS) core *~ *.bak
