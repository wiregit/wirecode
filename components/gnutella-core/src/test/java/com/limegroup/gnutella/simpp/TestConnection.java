package com.limegroup.gnutella.simpp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.limewire.concurrent.ManagedThread;
import org.limewire.io.ByteReader;
import org.limewire.service.ErrorService;
import org.limewire.util.AssertComparisons;
import org.limewire.util.FileUtils;
import org.limewire.util.StringUtils;

import com.limegroup.gnutella.BlockingConnectionUtils;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVM;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMStubHelper;
import com.limegroup.gnutella.messages.vendor.SimppRequestVM;
import com.limegroup.gnutella.messages.vendor.SimppVM;


public class TestConnection extends AssertComparisons {

    private Socket _socket;
    
    private byte[] _simppData; 
    
    
    // used to determine if the file should actually be sent.
    private boolean _sendSimppData;

    private volatile boolean _expectSimppRequest;

    private int _capabilitySimppNo;

    private boolean _causeError;
    
    private final MessageFactory messageFactory;
    
    private final CountDownLatch done = new CountDownLatch(1);

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
    public TestConnection(File simppFile, int simppNumber, boolean expectSimppReq,
                                     boolean sendSimppData, MessageFactory messageFactory) throws IOException {
        this(simppFile, simppNumber, expectSimppReq, sendSimppData, simppNumber, messageFactory);
    }
    
    public TestConnection(File simppFile, int simppNumber, boolean expectSimppReq, 
               boolean sendSimppData, int capabilitySimpp, MessageFactory messageFactory) throws IOException {
        super("FakeTest");
        setSimppMessageFile(simppFile);
        _expectSimppRequest = expectSimppReq;
        _sendSimppData = sendSimppData;
        _capabilitySimppNo = capabilitySimpp;
        _causeError = false;
        this.messageFactory = messageFactory;
    }

    public void start() {
        Thread t = new ManagedThread() {
            @Override
            public void run() {
                //Make an out going connection to the machine
                try {
                    _socket = new Socket("localhost", SimppManagerTest.PORT);
                    establishConnection();
                } catch (IOException iox) {
                    if(!_causeError) //if not expected, show errorservice
                        ErrorService.error(iox);
                } finally {
                    done.countDown();
                }
            }
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
        //write out the headers this code needs to change if the definition of
        //what's considered a good connection
        //Phase 1 of the handshake
        os.write(StringUtils.toAsciiBytes("GNUTELLA CONNECT/0.6\r\n"));
        os.write(StringUtils.toAsciiBytes("User-Agent: LimeWire/3.4.4\r\n"));
        os.write(StringUtils.toAsciiBytes("Listen-IP:127.0.0.1:6346\r\n"));
        os.write(StringUtils.toAsciiBytes("X-Query-Routing: 0.1\r\n"));
        os.write(StringUtils.toAsciiBytes("X-Max-TTL: 3\r\n"));
        os.write(StringUtils.toAsciiBytes("X-Dynamic-Querying: 0.1\r\n"));
        os.write((StringUtils.toAsciiBytes("X-Version: @version@\r\n")));
        os.write(StringUtils.toAsciiBytes("X-Ultrapeer: False\r\n"));
        os.write(StringUtils.toAsciiBytes("X-Degree: 15\r\n"));
        os.write(StringUtils.toAsciiBytes("X-Ultrapeer-Query-Routing: 0.1\r\n"));
        os.write(StringUtils.toAsciiBytes("\r\n"));
        //Phase 2 of handshake -- read
        String line = null;
        while((line = reader.readLine()) != null && !line.equals("")) {
            //System.out.println(line);
        }

        //Phase 3 of handshake -- write 200 OK
        os.write(StringUtils.toAsciiBytes("GNUTELLA/0.6 200 OK \r\n"));
        os.write(StringUtils.toAsciiBytes("\r\n"));
        //Handshake complete
        //Make sure initial SIMPP behavior is correct
        checkInitialSimppBehavior(is, os);
        //Make sure Simpp code is behaving correctly.
        checkSimppBehaviour(is, os);
    }
    
    private void checkInitialSimppBehavior(InputStream is, OutputStream os) 
            throws IOException {
        //This method checks that the initial simpp request is always followed
        //by a response.
        CapabilitiesVM capVM = makeCapabilitiesVM();
        capVM.write(os);
        os.flush();
        //Read the first message of type SimppRequest
        Message message = null;
        try {
            message = BlockingConnectionUtils.getFirstInstanceOfMessage(
                                           _socket, SimppRequestVM.class, 2000, messageFactory);
        } catch (BadPacketException bpx) {
            fail("limewire sent message with BPX");
        }
        if (_expectSimppRequest) {
            assertNotNull(message);
        } else {
            assertNull(message);
        }
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
        SimppRequestVM message = null;
        try {
            message = BlockingConnectionUtils.getFirstInstanceOfMessage(
                                           _socket, SimppRequestVM.class, 2000, messageFactory);
        } catch (BadPacketException bpx) {
            fail("limewire sent message with BPX");
        }
        if(_expectSimppRequest)
            assertNotNull("should have gotten simpp message", message);
        else
            assertNull("shouldn't have gotten simpp message", message);

        //send back the simppdata if reqd. 
        if( _sendSimppData) {//we dont want 
            if(_causeError)
                throw new IOException();
            if(message == null)
                message = new SimppRequestVM();
            Message simppVM = SimppVM.createSimppResponse(message, _simppData);
            simppVM.write(os);
            os.flush();
        }
        
        //Read messages, if (_expectSimppRequest) make sure we get it, otherwise
        //make sure we do not get it. Then based on _sendSimppData send the data
        //back once if we got the SimppRequestVM        
    }

    private CapabilitiesVM makeCapabilitiesVM() {
        try {
            return  CapabilitiesVMStubHelper.makeCapibilitesWithSimpp(_capabilitySimppNo);
        } catch (Exception e) {
            fail("couldn't set up test -- failed to manipulate CapabilitiesVM");
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
    
    public void setSimppMessageFile(File file) {
        _simppData = FileUtils.readFileFully(file);
        if (_simppData == null) {
            throw new RuntimeException("unable to read correct simpp test data");
        }
    }
    
    public boolean waitForConnection(long timeout, TimeUnit unit) throws InterruptedException {
        return done.await(timeout, unit);
    }

}
