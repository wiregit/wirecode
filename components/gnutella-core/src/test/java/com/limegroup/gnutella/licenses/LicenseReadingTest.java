package com.limegroup.gnutella.licenses;

import java.io.*;
import java.util.*;

import junit.framework.Test;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.metadata.*;
import com.limegroup.gnutella.xml.*;    

public final class LicenseReadingTest extends BaseTestCase {

	public LicenseReadingTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(LicenseReadingTest.class);
	}

	/**
	 * Runs this test individually.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	
	public void testReadID3AndXML() throws Exception {
	    File f = CommonUtils.getResourceFile("com/limegroup/gnutella/licenses/cc1.mp3");
	    assertTrue(f.exists());
	    
	    AudioMetaData amd = (AudioMetaData)MetaData.parse(f);
	    assertNotNull(amd);
	    
	    boolean foundLicense = false;
	    List nvList = amd.toNameValueList();
	    for(Iterator i = nvList.iterator(); i.hasNext(); ) {
	        NameValue nv = (NameValue)i.next();
	        assertFalse(AudioMetaData.isNonLimeAudioField(nv.getName()));
	        foundLicense |= nv.getName().equals("audios__audio__license__");
	    }
	    assertTrue(foundLicense);
	    assertEquals("2004 David Byrne Licensed to the " +
	                 "public under http://creativecommons.org/licenses/sampling+/1.0/ " +
	                 "verify at http://ccmixter.org/file/Wired/61", 
	                 amd.getLicense());
        
	    
	    LimeXMLDocument doc = new LimeXMLDocument(nvList, AudioMetaData.schemaURI);
	    assertTrue(doc.isLicenseAvailable());
	    assertEquals(amd.getLicense(), doc.getLicenseString());
	    assertEquals("<?xml version=\"1.0\"?>" +
"<audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\">" +
"<audio title=\"My Fair Lady\" artist=\"David Byrne\" album=" +
"\"The Wired CD: Rip. Sample. Mash. Share.\" genre=\"Other\" licensetype=" +
"\"creativecommons.org/licenses/\" track=\"2\" year=\"2004\" seconds=\"208\"" +
" bitrate=\"138\" license=\"2004 David Byrne Licensed to the public under " +
"http://creativecommons.org/licenses/sampling+/1.0/ verify at " +
"http://ccmixter.org/file/Wired/61\"/></audios>", doc.getXMLString());
	    
	    List indivList = new LinkedList();
	    indivList.add("creativecommons.org/licenses/");
	    assertEquals(indivList, doc.getKeyWordsIndivisible());
	    
	    boolean licenseTypeFound = false;
	    for(Iterator i = doc.getNameValueSet().iterator(); i.hasNext(); ) {
	        Map.Entry next = (Map.Entry)i.next();
	        String name = (String)next.getKey();
	        if(name.equals("audios__audio__licensetype__")) {
	            licenseTypeFound = true;
	            assertEquals("creativecommons.org/licenses/", next.getValue());
	        }
        }
        assertTrue(licenseTypeFound);
        
        License l = doc.getLicense();
        assertNotNull(l);
        assertEquals(CCLicense.class, l.getClass());
        assertFalse(l.isVerified());
        // don't validate -- don't wanna hit the web.
        assertEquals("http://creativecommons.org/licenses/sampling+/1.0/",
                     l.getLicenseDeed().toString());
        assertEquals("http://ccmixter.org/file/Wired/61", l.getLicenseURI().toString());
    }
    
    public void testReadOGG() throws Exception {
 	    File f = CommonUtils.getResourceFile("com/limegroup/gnutella/licenses/ccverifytest0.ogg");
	    assertTrue(f.exists());
	    
	    AudioMetaData amd = (AudioMetaData)MetaData.parse(f);
	    assertNotNull(amd);
	    
	    boolean foundLicense = false;
	    List nvList = amd.toNameValueList();
	    for(Iterator i = nvList.iterator(); i.hasNext(); ) {
	        NameValue nv = (NameValue)i.next();
	        assertFalse(AudioMetaData.isNonLimeAudioField(nv.getName()));
	        foundLicense |= nv.getName().equals("audios__audio__license__");
	    }
	    assertTrue(foundLicense);
	    assertEquals("2002 BM Relocation Program. Licensed to the public under " +
	                 "http://creativecommons.org/licenses/by-sa/1.0/ verify at " +
	                 "http://creativecommons.org/technology/verifytest/", 
	                 amd.getLicense());


        // TODO:  Test ccverifytest1.ogg
        //        Right now JOrbisFile construction is failing on it.
        /*
 	    f = CommonUtils.getResourceFile("com/limegroup/gnutella/licenses/ccverifytest1.ogg");
	    assertTrue(f.exists());
	    
	    amd = (AudioMetaData)MetaData.parse(f);
	    assertNotNull(amd);
	    
	    foundLicense = false;
	    nvList = amd.toNameValueList();
	    for(Iterator i = nvList.iterator(); i.hasNext(); ) {
	        NameValue nv = (NameValue)i.next();
	        assertFalse(AudioMetaData.isNonLimeAudioField(nv.getName()));
	        foundLicense |= nv.getName().equals("audios__audio__license__");
	    }
	    assertTrue(foundLicense);
	    assertEquals("2002 BM Relocation Program. Licensed to the public under " +
	                 "http://creativecommons.org/licenses/by-sa/1.0/ verify at " +
	                 "http://creativecommons.org/technology/verifytest/", 
	                 amd.getLicense());
        */
    }
}
            
