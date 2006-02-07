package com.limegroup.gnutella.simpp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.ByteReader;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVM;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMStubHelper;
import com.limegroup.gnutella.messages.vendor.SimppRequestVM;
import com.limegroup.gnutella.messages.vendor.SimppVM;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.CommonUtils;


public class TestConnection {

    private Socket _socket;
    
    private byte[] _simppData; 
    
    
    // used to determine if the file should actually be sent.
    private boolean _sendSimppData;

    private volatile boolean _expectSimppRequest;

    //The simpp message number this connection is expected to send
    private int _simppMessageNumber;

    private int _capabilitySimppNo;

    private boolean _causeError;

    /**
     * When creating a TestConnection you want to specify 4 things
     * @param port The port on which the TestConnection should connect to LW
     * @param simppNumber The simppNumber the TestConnection should pretend to
     * have, this affects the simpp-data that this TestConnection eventually
     * sends.
     * @param expectSimppReq whether or not the TestConnection should expect to
     * receive a SimppRequestVM
     * @param sendSimppData whether or not the TestConnection should send the
     * simpp-data when it receives a SimppRequestVM
     */
    public TestConnection(int simppNumber, boolean expectSimppReq,
                                     boolean sendSimppData) throws IOException {
        this(simppNumber, expectSimppReq, sendSimppData, simppNumber);
    }
    
    public TestConnection(int simppNumber, boolean expectSimppReq, 
               boolean sendSimppData, int capabilitySimpp) throws IOException {
        _simppData = readCorrectFile(simppNumber);        
        _simppMessageNumber = simppNumber;
        _expectSimppRequest = expectSimppReq;
        _sendSimppData = sendSimppData;
        _capabilitySimppNo = capabilitySimpp;
        _causeError = false;
    }

    public void start() {
        Thread t = new Thread() {
            public void run() {
                //Make an out going connection to the machine
                try {
                    _socket = new Socket("localhost", SimppManagerTest.PORT);
                    establishConnection();
                } catch (IOException iox) {
                    if(!_causeError) //if not expected, show errorservice
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
        os.write("X-Degree: 15\r\n".getBytes());
        os.write("X-Ultrapeer-Query-Routing: 0.1\r\n".getBytes());
        os.write("\r\n".getBytes());
        //Phase 2 of handshake -- read
        String line = null;
        while((line = reader.readLine()) != null && !line.equals("")) {
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
        //Read the first message of type SimppRequest
        Message message = null;
        try {
            message = BaseTestCase.getFirstInstanceOfMessage(
                                           _socket, SimppRequestVM.class, 2000);
        } catch (BadPacketException bpx) {
            Assert.that(false, "limewire sent message with BPX");
        }
        if(_expectSimppRequest)
            Assert.that(message != null, "should have gotten simpp message");
        else
            Assert.that(message == null, "shouldn't have gotten simpp message");

        //send back the simppdata if reqd. 
        if( _sendSimppData) {//we dont want 
            if(_causeError)
                throw new IOException();
            Message simppVM = new SimppVM(_simppData);
            simppVM.write(os);
            os.flush();
        }
        
        //Read messages, if (_expectSimppRequest) make sure we get it, otherwise
        //make sure we do not get it. Then based on _sendSimppData send the data
        //back once if we got the SimppRequestVM        
    }

    private CapabilitiesVM makeCapabilitiesVM() {
        try {
            return  CapabilitiesVMStubHelper.makeCapVM(_capabilitySimppNo);
        } catch (Exception e) {
            Assert.that(false, 
                "couldn't set up test -- failed to manipulate CapabilitiesVM");
        }
        return null;
    }

    ////////////////////////////setter methods////////////////////////

    public void setSendSimppData(boolean sendData) {
        _sendSimppData = sendData;
    }
    
    public void setCauseError(boolean err) {
        _causeError = err;
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
            file = CommonUtils.getResourceFile(simppDir+"randFile.xml");
        else if(fileVersion == SimppManagerTest.ABOVE_MAX)
            file = CommonUtils.getResourceFile(simppDir+"aboveMaxFile.xml");
        else if(fileVersion == SimppManagerTest.BELOW_MIN)
            file = CommonUtils.getResourceFile(simppDir+"belowMinFile.xml");
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
