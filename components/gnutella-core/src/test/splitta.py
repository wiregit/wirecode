#!/bin/python

"""
da splitta of logz

splits the log file into test cases - useful when comparing why one test
run passed and another didn't    
"""

mylog = open("log.txt")
current = 0 
currentLog = file("log.txt."+str(current),'a')
for line in mylog.readlines() :
   if line.find("INFO") != -1 :
      current+=1
      currentLog.close()
      currentLog = file("log.txt."+str(current),'a')
   currentLog.write(line)
