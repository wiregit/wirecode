package com.limegroup.gnutella.udpconnect;

import junit.framework.Test;

import com.limegroup.gnutella.util.BaseTestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import com.limegroup.gnutella.messages.Message;

/**
 * Unit tests for UDPConnectionMessages
 */
public class UDPMessageTest extends BaseTestCase {
    
    ByteArrayInputStream sin;
    ByteArrayOutputStream sout;

    
	public UDPMessageTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(UDPMessageTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	public void testDataMessage() throws Exception {

        // Construct a data message
        byte        connectionID   = 1;
        long        sequenceNumber = 0;
        byte[]      data           = new byte[512];
        int         datalength     = data.length;
        DataMessage dmWrite;
        DataMessage dmRead;

        SequenceNumberExtender extender = new SequenceNumberExtender(); 

        // Try a range of sequence numbers
        for( long i = 0; i <= 0xffff; i++) {
            dmWrite = new DataMessage(connectionID, i, data, datalength);

            // Write the message out
            sout = new ByteArrayOutputStream();
            dmWrite.write(sout);

            // Read the message in
            sin = new ByteArrayInputStream(sout.toByteArray());
            dmRead = (DataMessage) Message.read(sin);

            // Extend the msgs sequenceNumber to 8 bytes based on past state
            dmRead.extendSequenceNumber(
              extender.extendSequenceNumber( dmRead.getSequenceNumber()) ); 

            assertEquals(connectionID, dmRead.getConnectionID());
            assertEquals(i, dmRead.getSequenceNumber());
            assertEquals(datalength, dmRead.getDataLength());
        }
    }
	
}        
