package com.limegroup.gnutella.simpp;

import java.net.*;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.messages.vendor.*;
import com.limegroup.gnutella.util.*;
import java.io.*;


public class TestConnection {

    private Socket _socket;
    
    private byte[] _simppData; 
    
    
    // used to determine if the file should actually be sent.
    private boolean _sendSimppData;

    private volatile boolean _expectSimppRequest;

    //The simpp message number this connection is expected to send
    private int _simppMessageNumber;


    public TestConnection(int port, int simppNumber, boolean expectSimppReq) 
                                                          throws IOException {
        _simppData = readCorrectFile(simppNumber);        
        _simppMessageNumber = simppNumber;
        _expectSimppRequest = expectSimppReq;
        _sendSimppData = true;//we want to send in the default case
    }
    
    public void start() {
        Thread t = new Thread() {
            public void run() {
                //Make an out going connection to the machine
                try {
                    _socket = new Socket("localhost", SimppManagerTest.PORT);
                    establishConnection();
                } catch (IOException iox) {
                    ErrorService.error(iox);
                }        
            }//end of run
        };
        t.setDaemon(true);
        t.start();
    }

    public void killConnection() {
        try {
            if(_socket != null)
                _socket.close();
        } catch(IOException iox) {}
    }

    private void establishConnection() throws IOException {
        InputStream is = _socket.getInputStream();
        OutputStream os = _socket.getOutputStream();
        ByteReader reader = new ByteReader(is);
        int b = 0;
        //write out the headers this code needs to change if the definition of
        //what's considered a good connection
        //Phase 1 of the handshake
        os.write("GNUTELLA CONNECT/0.6\r\n".getBytes());
        os.write("User-Agent: LimeWire/3.4.4\r\n".getBytes());
        os.write("Listen-IP:127.0.0.1:6346\r\n".getBytes());
        os.write("X-Query-Routing: 0.1\r\n".getBytes());
        os.write("X-Max-TTL: 3\r\n".getBytes());
        os.write("X-Dynamic-Querying: 0.1\r\n".getBytes());
        os.write(("X-Version: @version@\r\n").getBytes());
        os.write("X-Ultrapeer: False\r\n".getBytes());
        os.write("\r\n".getBytes());
        //Phase 2 of handshake -- read
        String line = "dummy";
        while(!line.equals("")) {
            line = reader.readLine();
            //System.out.println(line);
        }

        //Phase 3 of handshake -- write 200 OK
        os.write("GNUTELLA/0.6 200 OK \r\n".getBytes());
        os.write("\r\n".getBytes());
        //Handshake complete
        //Make sure Simpp code is behaving correctly.
        checkSimppBehaviour(is, os);
    }
    
    private void checkSimppBehaviour(InputStream is, OutputStream os) 
                                                          throws IOException {
        //This method will create a CapabilitiesVM and send it to the LimeWire
        //it is connected to, it will then test that LimeWire is responding
        //correctly, and if necessary upload simpp-bytes to the limewire so the
        //testing can be done.
        
        CapabilitiesVM capVM = makeCapabilitiesVM();
        capVM.write(os);
        os.flush();
        //now, lets see if we expect a request or not
        Message message = null;
        boolean done = false;
        while(!done) {
            try {
                message = Message.read(is, Message.N_TCP);
            } catch (BadPacketException bpx) {
                bpx.printStackTrace();
            }
            System.out.println(message);
            if(message instanceof SimppRequestVM) {
                System.out.println("Sumeet: we are done");
                done = true;
            }
        }
        

        //Read messages, if (_expectSimppRequest) make sure we get it, otherwise
        //make sure we do not get it. Then based on _sendSimppData send the data
        //back once if we got the SimppRequestVM        
    }

    private CapabilitiesVM makeCapabilitiesVM() {        
        //1. Get the capabilitiesVM and cache it
        CapabilitiesVM capVM = CapabilitiesVM.instance();
        //2. Nullify the instance of CapabilitiesVM and recreate instance
        try {
            PrivilegedAccessor.setValue(CapabilitiesVM.class,"_instance", null);
        } catch(IllegalAccessException iax) {
            Assert.that(false,
                "couldn't set up test -- failed to manipulate CapabilitiesVM");
        } catch(NoSuchFieldException nsfx) {
            Assert.that(false,
                "couldn't set up test -- failed to manipulate CapabilitiesVM");
        }
        CapabilitiesVM.instance();//recreate it for RouterService
        
        //3. Modify the cached copy of CapabilitiesVM 
        
        //a. Make a new SupportedMessageBlock with the simpp capability and
        //version
        byte[] simpp_bytes = {(byte)83, (byte) 73, (byte)77, (byte)80};

        //smb is of type CapabilitiesVM.SupportedMessageBlock which is in
        //accessible from this package, never mind we just need to add it to the
        //HashSet
        Object smb = 
        CapabilitiesVMStubHelper.makeSMB(simpp_bytes, _simppMessageNumber);
        //b.clear the hashset in capVM
        Set set = null;
        try {
            set = (Set)PrivilegedAccessor.getValue(
                                              capVM, "_capabilitiesSupported");
        } catch (IllegalAccessException iax) {
            Assert.that(false, "problem with cached CapabilitiesVM");
        } catch(NoSuchFieldException nsfx) {
            Assert.that(false, "problem with cached CapabilitiesVM");
        }
        set.clear();
        //c. add the SupportedMessageBlock created in a to the HashSet of CapVM
        set.add(smb);
        //4. return the modified CapabilitiesVM
        return capVM;
    }


//      private CapabilitiesVM makeCapVM() {
//          Integer oldVer = null;
//          int prevSimppVersion = -1;
//          //1.
//          CapabilitiesVM capVM = CapabilitiesVM.instance();
//          //2. cache the older value of simppVersion
//          try {
//              oldVer = (Integer)PrivilegedAccessor.getValue(
//                                           CapabilitiesVM.class, "_simppVersion");
//          } catch (IllegalAccessException iax) {
//              Assert.that(false, 
//                  "couldn't set up test -- failed to manipulate CapabilitiesVM");
//          } catch (NoSuchFieldException nsfx) {
//              Assert.that(false,
//                  "couldn't set up test -- failed to manipulate CapabilitiesVM");
//          }
//          prevSimppVersion = oldVer.intValue();
//          //3. set the simppversion to what we want to send
//          CapabilitiesVM.updateSimppVersion(_simppMessageNumber);
//          CapabilitiesVM ret = CapabilitiesVM.instance();
//          CapabilitiesVM.updateSimppVersion(prevSimppVersion);
//          return ret;
//      }

    ////////////////////////////setter methods////////////////////////

    public void setSendSimppData(boolean sendData) {
        _sendSimppData = sendData;
    }

    public void setExpectSimppRequest(boolean expected) {
        _expectSimppRequest = expected;
    }
    
    public void setSimppMessageNumber(int number) {
        _simppMessageNumber = number;
        try {
            _simppData = readCorrectFile(number);        
        } catch(IOException iox) {
            Assert.that(false, "unable to read correct simpp test data");
        }
    }

    ////////////////////////utilities//////////////////////

    private byte[] readCorrectFile(int fileVersion) throws IOException {
        File file = null;
        String simppDir = "com/limegroup/gnutella/simpp/";
        if(fileVersion == SimppManagerTest.OLD)
            file = CommonUtils.getResourceFile(simppDir+"oldFile.xml");
        else if(fileVersion == SimppManagerTest.MIDDLE)
            file = CommonUtils.getResourceFile(simppDir+"middleFile.xml");
        else if(fileVersion == SimppManagerTest.NEW) 
            file = CommonUtils.getResourceFile(simppDir+"newFile.xml");
        else if(fileVersion == SimppManagerTest.DEF_MESSAGE)
            file = CommonUtils.getResourceFile(simppDir+"defMessageFile.xml");
        else if(fileVersion == SimppManagerTest.DEF_SIGNATURE)
            file = CommonUtils.getResourceFile(simppDir+"defSigFile.xml");
        else if(fileVersion == SimppManagerTest.BAD_XML)
            file = CommonUtils.getResourceFile(simppDir+"badXmlFile.xml");
        else if(fileVersion == SimppManagerTest.RANDOM_BYTES)
            file =CommonUtils.getResourceFile(simppDir+"randFile.xml");
        else
            Assert.that(false,"simpp version set to illegal value");
        
        //read the bytes of the file and close it. 
        RandomAccessFile raf = new RandomAccessFile(file,"r");
        byte[] ret = new byte[(int)raf.length()];
        raf.readFully(ret);
        raf.close();
        return ret;
    }

}
