@ECHO OFF
cd ..\core
jar -cvf ..\gui\lib\core.jar com\bitzi\util\*.class
jar -uvf ..\gui\lib\core.jar com\limegroup\gnutella\connection\*.class
jar -uvf ..\gui\lib\core.jar com\limegroup\gnutella\bootstrap\*.class
jar -uvf ..\gui\lib\core.jar com\limegroup\gnutella\chat\*.class
jar -uvf ..\gui\lib\core.jar com\limegroup\gnutella\mp3\*.class
jar -uvf ..\gui\lib\core.jar com\limegroup\gnutella\http\*.class
jar -uvf ..\gui\lib\core.jar com\limegroup\gnutella\messages\*.class
jar -uvf ..\gui\lib\core.jar com\limegroup\gnutella\updates\*.class
jar -uvf ..\gui\lib\core.jar com\limegroup\gnutella\settings\*.class
jar -uvf ..\gui\lib\core.jar com\limegroup\gnutella\guess\*.class
jar -uvf ..\gui\lib\core.jar com\limegroup\gnutella\security\*.class
jar -uvf ..\gui\lib\core.jar com\limegroup\gnutella\messages\vendor\*.class
jar -uvf ..\gui\lib\core.jar com\limegroup\gnutella\statistics\*.class
jar -uvf ..\gui\lib\core.jar com\limegroup\gnutella\routing\*.class
jar -uvf ..\gui\lib\core.jar com\limegroup\gnutella\uploader\*.class
jar -uvf ..\gui\lib\core.jar com\limegroup\gnutella\filters\*.class
jar -uvf ..\gui\lib\core.jar com\limegroup\gnutella\handshaking\*.class
jar -uvf ..\gui\lib\core.jar com\limegroup\gnutella\downloader\*.class
jar -uvf ..\gui\lib\core.jar com\limegroup\gnutella\search\*.class
jar -uvf ..\gui\lib\core.jar com\limegroup\gnutella\browser\*.class
jar -uvf ..\gui\lib\core.jar com\limegroup\gnutella\xml\*.class
jar -uvf ..\gui\lib\core.jar com\limegroup\gnutella\util\*.class
jar -uvf ..\gui\lib\core.jar com\limegroup\gnutella\*.class
REM Goto the GUI project to compile it with the new core
cd ..\gui\lib
jar -i core.jar
cd ..
