The core project includes code required to run the Gnutella protocol and advanced sharing abilities.  This project combined with the gui project make up the LimeWire client.
  
Compilation:

Compiling this package requires ant and Java 1.3.1. These can be downloaded respectively from:
http://jakarta.apache.org/ant/index.html
http://java.sun.com/j2se/1.3/

 
After these tools are installed, using the compile command for Windows or the ./compile command for Unix/Linux should compile the code.  If you set your environment variables as
per the appropriate compile script, typing "ant" at the command line should also compile the code.
  

Run core test shell:

Runing the test shell should be as simple as using the run command for Windows or the ./run script for Unix/Linux.  If your environment is set properly, the straight java command should work: 
java com.limegroup.gnutella.Main


CORE and GUI Interface changes:
Please note that if you are a core and gui developer and you change the interface between the core and the GUI, you will need to rebuild the core.jar file and commit it in the gui/lib directory.

