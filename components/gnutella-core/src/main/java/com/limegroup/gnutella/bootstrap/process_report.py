#Input: report.html, downloaded from http://www.zero-g.net/gwebcache/report.html
#
#Output: the good hosts in that list, in Java format suiteable for inclusion
#       in DefaultBootstrapServers.java
#
#Algorithm: Look for lines like of the format "\t<td>a href="U">U</a></td>,
#       where is a URL, e.g., lines like
#            <td><a href="http://who.net/gcache.php">http://who.net/gcache.php</a></td>
#       Here U="http://who.net/gcache.php".  Yes, it's very fragile.

from re import *
from string import *

def startswith(str, prefix):
    return str[:len(prefix)]==prefix

file=open("report.html")
while 1:
    line=file.readline()
    if line=="":
        break
    if startswith(line, "\t<td><a href=\""):
        i=index(line, "\"")
        j=rindex(line, "\"")
        url=line[i+1:j]
        #TODO: URL decode
        print "        \"%s\"," % url
