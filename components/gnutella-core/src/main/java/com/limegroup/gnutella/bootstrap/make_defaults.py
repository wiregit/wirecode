"""
Generates a new DefaultBootstrapServer.java file from the latest
online GWebCache scan and the previous entries of DefaultBootstrapServer
file.  Verifies that all addresses in the file are valid.  Outputs the
new file to standard output.  You should not redirect the output to
DefaultBootstrapServer.java; verify the contents first.

Note: for some reason the generated file has lots of ^M characters.  These
are ugly but don't prevent compilation.  Still, you can remove these
characters in emacs by search for the character and replacing it with nothing:
   M-% C-q m <enter> <enter>
"""

from string import *
from urllib import *
import timeoutsocket
import sys

#The latest complete GWebCache scan.  This is a mirror of
#http://zero-g.net/gwc/GWebCache.net, which is currently out.
#start_url="http://www.gnucleus.net/gwebcache/GWebCache.net"
start_url="http://www.rodage.net/gnetcache/gcache.php?urlfile=400"
#Where to write the output.  Default: console
out=sys.stdout
#Write bad hosts as comments?
write_bad=0
#The list of GWebCaches to consider.
candidates=[]


#The top-level main() method.
def make_defaults():
    #1. Get unvalidated URLs
    get_new_candidates()
    get_old_candidates()
    candidates.sort()

    #2. Validate and print
    write(header)
    print_valid_candidates()
    write(footer)


########################### Find URLs to Consider #####################

#Fills up CANDIDATES with the entries of START_URL, without verifying.
def get_new_candidates():
    f=open("urls.txt")
    while 1:
        line=strip(f.readline())
        if line=="":
            break                     #eof
        if not contains(candidates, line):
            candidates.append(line)   #a new candidate
    f.close()

#Reads old URLs from DefaultBootstrapServer.java and stores them in
#candidates, without verifying them.
def get_old_candidates():
    f=open("DefaultBootstrapServers.java")
    while 1:
        line=f.readline()
        if line=="":
            break #eof
        #Try to extract URL from Java...
        i=find(line, "http://")
        j=rfind(line, "\"")
        if i>0 and j>0:
            url=line[i:j]
            #...and add if unique
            if not contains(candidates, url):
                candidates.append(url)                        
    f.close()

#Returns true if list contains some url "similar" to url
def contains(list, url):
    def strip(url):
        i=rindex(url, "/")
        if (i<0):
            i=0    
        return url[:i]
    
    for url2 in list:
        if strip(url)==strip(url2):
            return 1
    return 0

test_list=["http://a.com/path/gcache.php", "http://b.com/path/"]
assert not contains(test_list, "http://c.com/path/gcache.php")
assert not contains(test_list, "http://a.com/path2/gcache.php")
assert contains(test_list, "http://a.com/path/index.php")
assert contains(test_list, "http://a.com/path/")
assert contains(test_list, "http://b.com/path/")
assert contains(test_list, "http://b.com/path/index.php")


######################### Validate and Print ############################

#Prints the valid URLs of candidates, along with Java formatting.    
def print_valid_candidates():
    timeoutsocket.setDefaultSocketTimeout(10)
    first_line=1     #Used to keep track of commas
    for url in candidates:
        wrote=0
        try:
            #f=urlopen(url+"?hostfile=1")
            f=urlopen(url+"?client=TEST&version=GWC0.8.5&hostfile=1")
            if valid_file(f):
                wrote=print_candidate(url, 1, first_line)
            else:
                wrote=print_candidate(url, 0, first_line, "malformed")
            f.close()
        except:
            wrote=print_candidate(url, 0, first_line, "unreachable")
        first_line=first_line and not wrote

def print_candidate(url, is_good, first_line, msg=""):
    indent="        "
    if not first_line:
        indent=",\n"+indent

    if is_good:
        write(indent+"\""+url+"\"")
        return 1
    elif write_bad:
        write(indent+"//\""+url+"\" ("+msg+")")
        return 1
    return 0

def write(str):
    out.write(str)
    out.flush()

#Returns true if the contents at f appear to be a valid hostfile response.
def valid_file(f):
    line1=f.readline()
    line2=f.readline()
    return valid_line(line1) and valid_line(line2)

def valid_line(line):
    return count(line, ".")==3 and count(line, ":")==1


header="""
package com.limegroup.gnutella.bootstrap;

import java.text.ParseException;

/**
 * The list of default GWebCache urls, used the first time LimeWire starts, or
 * if the gnutella.net file is accidentally deleted.  Entries in the list will
 * eventually be replaced by URLs discovered during urlfile=1 requests.  Order
 * does not matter.
 *
 * THIS FILE IS AUTOMATICALLY GENERATED FROM MAKE_DEFAULT.PY.
 */
public class DefaultBootstrapServers {
    /**
     * Adds all the default servers to bman. 
     */
    public static void addDefaults(BootstrapServerManager bman) {
        for (int i=0; i<urls.length; i++) {
            try {
                BootstrapServer server=new BootstrapServer(urls[i]);
                bman.addBootstrapServer(server);
            } catch (ParseException ignore) {
            }                
        }
    }

    //These should NOT be URL encoded.
    static String[] urls=new String[] {
"""

footer="""
    };
}
"""

if __name__=="__main__":
    make_defaults()
