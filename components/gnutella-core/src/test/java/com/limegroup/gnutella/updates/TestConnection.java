package com.limegroup.gnutella.updates;

import java.net.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import java.io.*;


public class TestConnection {

    private ServerSocket _server;
    
    public String _headerVersion;

    private Socket _socket;
    private int _port;
    
    private byte[] _updateData; 
    
    
    // used to determine if the file should actually be sent.
    private boolean _sendUpdateData;

    private boolean _testUpdateNotRequested;


    public TestConnection(int port, String headerVersion, int fileVersion) 
                                                          throws IOException {
        _headerVersion = headerVersion;
        _port = port;
        _updateData = readCorrectFile(fileVersion);        
    }
    
    public void start() {
        //1. set up an incoming socket
        try {
            _server = new ServerSocket();
            _server.setReuseAddress(true);
            _server.bind(new InetSocketAddress(_port));
        } catch (IOException iox) {
            ErrorService.error(iox);
        }
        
        Thread t = new Thread() {
            public void run() {
                try {
                    loop();
                } catch(Throwable t) {
                    ErrorService.error(t);
                }
            }
        };
        t.setDaemon(true);
        t.start();

        try {
            Thread.sleep(2000);//give the server socket a little time to start
        } catch(InterruptedException e) {
            ErrorService.error(e);
        }
        //2. Make an out going connection to the machine
        try {
            _socket = new Socket("localhost", UpdateManagerTest.PORT);
            doHandshake();
        } catch (IOException iox) {
            ErrorService.error(iox);
        }
        
    }
    
    void killThread() {
        try {
            if(_server != null)
                _server.close();
        } catch(IOException iox) {}
    }
    
    private void doHandshake() { //throws IOException {
        try {
        InputStream is = _socket.getInputStream();
        OutputStream os = _socket.getOutputStream();
        ByteReader reader = new ByteReader(is);
        int b = 0;
        //write out the headers this code needs to change if the definition of
        //what's considered a good connection
        //Phase 1 of the handshake
        os.write("GNUTELLA CONNECT/0.6\r\n".getBytes());
        os.write("User-Agent: LimeWire/3.4.4\r\n".getBytes());
        os.write(
            ("Listen-IP:127.0.0.1:"+UpdateManagerTest.PORT+"\r\n").getBytes());
        os.write("X-Query-Routing: 0.1\r\n".getBytes());
        os.write("X-Max-TTL: 3\r\n".getBytes());
        os.write("X-Dynamic-Querying: 0.1\r\n".getBytes());
        os.write(("X-Version: "+_headerVersion+"\r\n").getBytes());
        os.write("X-Ultrapeer: False\r\n".getBytes());
        os.write("\r\n".getBytes());
        //Phase 2 of handshake -- read
        String line = "dummy";
        while(!line.equals("")) 
            line = reader.readLine();

        //Phase 3 of handshake -- write 200 OK
        os.write("GNUTELLA/0.6 200 OK \r\n".getBytes());
        os.write("\r\n".getBytes());
        line = "dummy";
        _socket.setSoTimeout(1000);//keep the socket open for a second.
        while(line!=null) {//read while the socket is open
            try {
                line = reader.readLine();
            } catch (InterruptedIOException iox) {
                break;
            }
        }

        } catch (IOException iox) {
            iox.printStackTrace();
        }
    }

    //Server side code that services requsts for update file. 
    private void loop() throws IOException {
        Socket incoming = null;
        while(true) {
            incoming = _server.accept();
            final Socket mySocket = incoming;
            Thread runner = new Thread() {
                public void run() {
                    try {
                        handleRequest(mySocket);
                    } catch (IOException iox) {

                    }
                }
            };
            runner.setDaemon(true);
            runner.start();
        } //end of while
    }
    

    private void handleRequest(Socket socket) throws IOException {
        System.out.println("\t\t\t handling request");
        if(_testUpdateNotRequested)
            Assert.that(false, "Unexpected behaviour -- update requested");
        InputStream is = socket.getInputStream();
        OutputStream os = socket.getOutputStream();
        ByteReader reader = new ByteReader(is);
        int b = 0;
        String line = "dummy";
        while(line!=null && !line.equals("")) {
            line = reader.readLine();
        }
        if(!_sendUpdateData)//simply close the socket
            throw new IOException();
        os.write(_updateData);
        os.flush();
        socket.close(); //thanks and bye bye
    }

    ////////////////////////////setter methods////////////////////////

    public void setSendUpdateData(boolean sendData) {
        _sendUpdateData = sendData;
    }

    public void setTestUpdateNotRequested(boolean requested) {
        _testUpdateNotRequested = requested;
    }
    
    ////////////////////////utilities//////////////////////

    private byte[] readCorrectFile(int fileVersion) throws IOException {
        File file = null;
        String updateDir = "com/limegroup/gnutella/updates/";
        if(fileVersion == UpdateManagerTest.OLD)
            file = CommonUtils.getResourceFile(updateDir+"old_verFile.xml");
        else if(fileVersion == UpdateManagerTest.NEW) 
            file = CommonUtils.getResourceFile(updateDir+"new_verFile.xml");
        else if(fileVersion == UpdateManagerTest.DEF_MESSAGE)
            file = CommonUtils.getResourceFile(updateDir+"def_messageFile.xml");
        else if(fileVersion == UpdateManagerTest.DEF_SIGNATURE)
            file = CommonUtils.getResourceFile(updateDir+"def_verFile.xml");
        else if(fileVersion == UpdateManagerTest.BAD_XML)
            file = CommonUtils.getResourceFile(updateDir+"bad_xmlFile.xml");
        else if(fileVersion == UpdateManagerTest.RANDOM_BYTES)
            file =CommonUtils.getResourceFile(updateDir+"random_bytesFile.xml");
        else
            Assert.that(false,"updateVersion set to wrong value");
        
        //read the bytes of the file and close it. 
        RandomAccessFile raf = new RandomAccessFile(file,"r");
        byte[] ret = new byte[(int)raf.length()];
        raf.readFully(ret);
        raf.close();
        return ret;
    }

}
