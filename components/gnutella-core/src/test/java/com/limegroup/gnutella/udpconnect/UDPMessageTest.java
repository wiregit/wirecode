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

        SequenceNumberExtender extender = new SequenceNumberExtender(); 

        // Try a range of sequence numbers
        for( long i = 0; i <= 0x1ffff; i++) {
            writeAndReadADataMessage(connectionID, i, data, datalength, 
              extender);
        }

        // Test boundary conditions
        extender = new SequenceNumberExtender(999999l); 
        writeAndReadADataMessage((byte)0xff, 999999l, data, datalength, 
          extender);
    }


    private void writeAndReadADataMessage(
      byte                   connectionID,
      long                   sequenceNumber,
      byte[]                 data,
      int                    datalength,
      SequenceNumberExtender extender) throws Exception {

        DataMessage dmWrite;
        DataMessage dmRead;
        dmWrite = 
          new DataMessage(connectionID, sequenceNumber, data, datalength);

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
        assertEquals(sequenceNumber, dmRead.getSequenceNumber());
        assertEquals(datalength, dmRead.getDataLength());
    }
	
}        
