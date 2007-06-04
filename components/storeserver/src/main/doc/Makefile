NAMES=localServer remoteServer main
DOTS=$(addsuffix .$(DOT_OUTPUT), $(NAMES))
OPEN=acrobat
BASE=../../../
JAVA=`find $(BASE) -name *.java`

DOT_OUTPUT=jpg

%.$(DOT_OUTPUT):%.dot
	dot -T$(DOT_OUTPUT) $^ -o $@

%.view:%.$(DOT_OUTPUT)
	$(OPEN) $<

all: doc

doc: $(DOTS)

check: message_format_check.pl
	./message_format_check.pl $(JAVA)

clean:
	rm -f $(DOTS) *~
