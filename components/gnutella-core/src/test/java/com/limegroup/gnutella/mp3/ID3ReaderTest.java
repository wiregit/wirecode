package com.limegroup.gnutella.mp3;

import junit.framework.*;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.BaseTestCase;
import java.util.*;
import java.net.*;
import java.io.*;

/**
 * Unit tests for ID3Reader - right now it just tests hasVerifiedLicense.
 */
public class ID3ReaderTest extends BaseTestCase {
        
    public final String fileNameToTest = "com/limegroup/gnutella/mp3/mpg1layIII_0h_58k-VBRq30_frame1211_44100hz_joint_XingTAG_sample.mp3";
    public final String webPageContent = "<html> <!-- <rdf:RDF xmlns=\"http://web.resource.org/cc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"> <Work rdf:about=\"urn:sha1:BJ5VVVC26NXKOKYGCODPU6JDP4ZLHABM\"> <dc:date>2002</dc:date> <dc:format>audio/mp3</dc:format> <dc:identifier>http://mirrors.creativecommons.org/copyremix/Analogue Popkick - Two June.mp3</dc:identifier> <dc:rights><Agent><dc:title>Analogue Popkick</dc:title></Agent></dc:rights> <dc:title>Two June</dc:title> <dc:type rdf:resource=\"http://purl.org/dc/dcmitype/Sound\" /> <license rdf:resource=\"http://creativecommons.org/licenses/by-nc-sa/1.0/\" /> </Work>  <Work rdf:about=\"urn:sha1:B2QGGOTVTOFJA4224FFO3MVHMOKQJFPI\"> <dc:date>2002</dc:date> <dc:format>audio/mp3</dc:format> <dc:identifier>http://mirrors.creativecommons.org/copyremix/Bm RELOCATION PROGRAM - Superego Exchange.mp3</dc:identifier> <dc:rights><Agent><dc:title>Bm RELOCATION PROGRAM</dc:title></Agent></dc:rights> <dc:title>superego exchange</dc:title> <dc:type rdf:resource=\"http://purl.org/dc/dcmitype/Sound\" /> <license rdf:resource=\"http://creativecommons.org/licenses/by-nc-sa/1.0/\" /> </Work> </rdf:RDF>--> </html>";
    public final String actualFile0 = "com/limegroup/gnutella/mp3/ccverifytest0.mp3";
    public final String actualFile1 = "com/limegroup/gnutella/mp3/ccverifytest1.mp3";

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
                        String output = "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n\r\n";
                        s.getOutputStream().write(output.getBytes());
                        s.getOutputStream().flush();
                        s.getOutputStream().write(webPageContent.getBytes());
                        s.getOutputStream().flush();
                        Thread.sleep(500);
                        s.close();
                    }
                    catch (Exception bad) {
                        bad.printStackTrace();
                    }
                }
            };
        answerThread.start();
        Thread.sleep(1000);

        File f = new File(fileNameToTest);
        assertTrue(f.exists());
        assertTrue(ID3Reader.hasVerifiedLicense(fileNameToTest,
                                                URN.createSHA1Urn(f).toString()));
        ss.close();
    }

    public void testActualFiles() throws Exception {

        File f = new File(actualFile0);
        assertTrue(f.exists());
        assertTrue(ID3Reader.hasVerifiedLicense(actualFile0,
                                                URN.createSHA1Urn(f).toString()));
        f = new File(actualFile1);
        assertTrue(f.exists());
        assertTrue(ID3Reader.hasVerifiedLicense(actualFile1,
                                                URN.createSHA1Urn(f).toString()));
    }

    public void testGetCCLicense() {
        File f = new File(actualFile0);
        assertTrue(f.exists());
        assertEquals("http://creativecommons.org/licenses/by-sa/1.0/",
                     ID3Reader.getCCLicense(actualFile0));
        f = new File(actualFile1);
        assertTrue(f.exists());
        assertEquals("http://creativecommons.org/licenses/by-nc-sa/1.0/",
                     ID3Reader.getCCLicense(actualFile1));
    }

}
