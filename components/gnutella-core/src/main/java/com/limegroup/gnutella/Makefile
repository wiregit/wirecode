# Makefile


# the java compiler
JAVA	= javac 

RM      = /bin/rm -rf

# grab all sources - kind of hacky
SRCS := $(wildcard *.java)

# our object files
OBJS	= $(SRCS:.java=.class)

# targets

# make everything, (just makes .class files, for now)
all: $(SRCS)
	$(JAVA) $<

# dont need this part... 
# make all class files
# class: $(OBJS)
# 	$(JAVA) $<

show_srcs:
	@echo $(SRCS)

show_objs:
	@echo $(OBJS)	

# clean and then make again
again:	clean all

# clean up after making - remove core, ~ files, and .bak files
clean:
	$(RM) $(OBJS) core *~ *.bak
