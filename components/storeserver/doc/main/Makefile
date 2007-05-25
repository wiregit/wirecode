NAMES=localServer remoteServer main
DOTS=$(addsuffix .$(DOT_OUTPUT), $(NAMES))
OPEN=acrobat

DOT_OUTPUT=jpg

%.$(DOT_OUTPUT):%.dot
	dot -T$(DOT_OUTPUT) $^ -o $@

%.view:%.$(DOT_OUTPUT)
	$(OPEN) $<

all: doc

doc: $(DOTS)

clean:
	rm -f $(DOTS) *~
