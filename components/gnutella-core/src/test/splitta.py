#!/bin/python

"""
da splitta of logz by hommie baz

Splits the log file into test cases - useful when comparing why one test
run passed and another didn't

Command line options:
log=<log file name>  changes which log file to split, default is "log.txt"

delimiter=<record delimiter> changes the token on which to seperate log entries
default is "INFO"

header=<record header> prepends the given string to each record
"""

#
#  Function declarations
#

def split_log(mylog, output_base_name, record_delimiter, current=0, file_header=None): 
   """Splits mylog at record_delimiter, starting numbering with current. 
      Output files start with file_header and will be named starting with
      output_base_name
   """
   currentLog = None
   for line in mylog.readlines() :
      if line.find(record_delimiter) != -1 :
         # close old log and start a new log
         if currentLog != None:
            currentLog.close()
         version_str = str(current)
         while len(version_str) < 4:
            version_str = "0" + version_str
         currentLog = file(output_base_name+version_str,'a')
         if file_header != None:
            currentLog.write(file_header)
         current+=1

      # write the line to a log
      if currentLog != None:
         currentLog.write(line)

   # not strictly necessary, but clean
   if currentLog != None:
      currentLog.close()

#
# Stand-alone functionality, including command line arg parsing
#

if ("__main__" == __name__):
   from sys import argv

   # defaults
   log_name = "log.txt"
   case_delimiter = "INFO"
   file_header = None
   initial_output_number = 0   

   # parse command line
   for arg in argv:
      if len(arg) > 4 and arg[:4] == "log=":
         log_name = arg[4:]
      elif len(arg) > 10 and arg[:10] == "delimiter=":
         case_delimiter = arg[10:]
      elif arg[:7] == "header=":
         file_header = arg[7:]
         if file_header[-1] != "\n":
            file_header += "\n"

   mylog = open(log_name)

   split_log(mylog, log_name+".", case_delimiter, initial_output_number, file_header)


