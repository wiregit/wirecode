"""
A program that attempts to replace old-fashioned tests of the form
      Assert.that(condition, message);
to the standard JUnit form:
      assertTrue(message, condition);

This code is VERY fragile--it doesn't perform a full parse--so make
backups of your code and look at the processed version.

It's also ugly as heck.  Maybe someone can clean it up using Python's
__main_ convention at some point.
"""

from sys import *
from string import *

#Splits the given parameter list into arguments.  More formally
#returns a list [a1,a2] s.t. no element of a1 contains ",", the
#parentheses of a1 are balanced and a1+","+a2=args.  If this
#is not possible, returns [a1].  Examples:
#
#  split_arguments("x.equals(f(y, z))") ==> ["x.equals(f(y, z))"]
#  split_arguments("x.equals(f(y, z)), a") ==> ["x.equals(f(y, z))", " a"]
def split_arguments(args):
    parens=0
    for i in xrange(0, len(args)):
        if (args[i])=="(":
            parens=parens+1
        elif (args[i])==")":
            parens=parens-1
        elif args[i]=="," and parens==0:
            return [args[0:i], args[i+1:]]
    return [ args ]

assert split_arguments("x.equals(f(y, z))")==["x.equals(f(y, z))"]        
assert split_arguments("x.equals(f(y, z)), a")==["x.equals(f(y, z))", " a"]    

#Read main file.
if len(argv)!=2:
    stderr.write("Syntax: replace_asserts <input.java>\n")
    exit(1)

ASSERT_THAT="Assert.that("
END_CALL=");"
EOL="\n"

file=open(argv[1])
out=stdout
while 1:
    #Read each line.
    line=file.readline()
    if line=="":
        break

    #Is this an assert statement?  If so, given
    #    line=" Assert.that(a.equals(b), msg);",
    #break this into
    #    head=" Assert.that("
    #    arguments="a.equals(b), msg"
    #    tail=");"
    #    args=["a.equals(b)", "msg"]
    i=find(line, ASSERT_THAT)
    j=rfind(line, END_CALL)
    if (i<0 or j<0):
        #Print line untranslated
        out.write(line)
        continue    
    head=line[0:(i+len(ASSERT_THAT))]
    arguments=line[(i+len(ASSERT_THAT)):j]
    tail=line[j:]
    assert line==head+arguments+tail
    args=split_arguments(arguments)

    #And print this out again.
    out.write(replace(head, ASSERT_THAT, "assertTrue("))
    if (len(args)==1):
        out.write(args[0])
    else:
        out.write(strip(args[1]))
        out.write(", ")
        out.write(strip(args[0]))    
    out.write(tail)
