package com.limegroup.gnutella.mp3;

import junit.framework.*;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.gnutella.ByteReader;
import com.limegroup.gnutella.util.BaseTestCase;
import java.util.*;
import java.net.*;
import java.io.*;

/**
 * Unit tests for ID3Reader - right now it just tests hasVerifiedLicense.
 */
public class ID3ReaderTest extends BaseTestCase {
        
    public final String fileNameToTest = "com/limegroup/gnutella/mp3/mpg1layIII_0h_58k-VBRq30_frame1211_44100hz_joint_XingTAG_sample.mp3";

	public ID3ReaderTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(ID3ReaderTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

    public void testVerifyCCLicense() throws Exception {
        
        final ServerSocket ss = new ServerSocket(9999);
        Thread answerThread = new Thread() {
                public void run() {
                    try {
                        Socket s = ss.accept();
                        String output = "HTTP/1.1 OK\r\n\r\n";
                        s.getOutputStream().write(output.getBytes());
                        s.getOutputStream().flush();
                        s.close();
                    }
                    catch (Exception bad) {
                        bad.printStackTrace();
                    }
                }
            };
        answerThread.start();
        Thread.sleep(1000);

        assertTrue(!ID3Reader.hasVerifiedLicense(fileNameToTest));

    }

}
