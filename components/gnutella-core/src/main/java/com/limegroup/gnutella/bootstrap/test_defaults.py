#Input:  DefaultBootstrapServers.java
#Output: which of the defaults in DefaultBootstrapServers.java are reachable

from string import *
from urllib import *
import timeoutsocket

def valid_line(line):
    return count(line, ".")==3 and count(line, ":")==1
assert valid_line("18.239.0.144:6346")
assert not valid_line("18.239.0:6346")
assert not valid_line("18.239.0.144")

def valid_file(f):
    line1=f.readline()
    line2=f.readline()
    return valid_line(line1) and valid_line(line2)

timeoutsocket.setDefaultSocketTimeout(5)
file=open("DefaultBootstrapServers.java")
out=sys.stdout
while 1:
    line=file.readline()
    if line=="":
        break

    #Remove whitespace
    line=strip(line)
    #Ignore comments
    if (line[:2]=="//"):
        line=line[2:]
    #Remove leading quote and trailing quote plus comma.
    #TODO: there's no comma on last line
    line=line[1:][:-2]
    
    if line[:4]=="http":
        try:
            #print "Opening "+line
            f=urlopen(line+"?hostfile=1")
            #print "Done"
            if not valid_file(f):
                raise "Invalid file"
            f.close()
            out.write(".")
            #out.write("%s success\n" % line)
        except:
            out.write("\n%s FAILURE\n" % line)
        out.flush()


