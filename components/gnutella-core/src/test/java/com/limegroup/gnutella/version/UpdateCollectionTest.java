package com.limegroup.gnutella.version;

import java.io.*;
import java.util.*;

import junit.framework.Test;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;

import org.apache.commons.httpclient.*;

public final class UpdateCollectionTest extends BaseTestCase {

	public UpdateCollectionTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(UpdateCollectionTest.class);
	}

	/**
	 * Runs this test individually.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	public void testBasicCreation() throws Exception {
	    
	    UpdateCollection uc = UpdateCollection.create(
	        "<update id='42' timestamp=\"150973213135\">" +
	            "<msg for='4.6.0' url='http://www.limewire.com/update' style='2'>" +
	                "<lang id='en'>" +
	                    "<![CDATA[<html><body>This is the text</body></html>]]>" +
	                "</lang>" +
	                "<lang id='es' button1='b1' button2='b2'>" +
	                    "Hola, no habla espanol." +
	                "</lang>" +	                
	                "<lang id='notext'></lang>" +
	            "</msg>" +
	            "<msg/> " +
	            "<msg for='4.1.2' url='http://limewire.com/hi'>" +
	                "<lang id='en'>" + 
	                    "This didn't have a style, it should be ignored." +
	                "</lang>" +
	            "</msg>" +
	            "<msg for='4.1.2' style='3'>" +
	                "<lang id='en'>" + 
	                    "This didn't have a URL, it should be ignored." +
	                "</lang>" +
	            "</msg>" +
	            "<msg style='3' url='nostyle'>" +
	                "<lang id='en'>" + 
	                    "This didn't have a 'for', it should be ignored." +
	                "</lang>" +
	            "</msg>" +	            	            
	        "</update>");
	        
        // First make sure it ignored the invalid msgs.
        assertEquals(uc.getUpdateData().toString(), 2, uc.getUpdateData().size());
        assertEquals(42, uc.getId());
        assertEquals(150973213135L, uc.getTimestamp());
	    
	    UpdateData data;
	    
        // if we already have 4.6.0, this should find nothing.     
	    data = uc.getUpdateDataFor(new Version("4.6.0"), "en", false, UpdateInformation.STYLE_MAJOR, null);
	    assertNull(data);
	    
	    // if we're above 4.6.0, this should find nothing.
	    data = uc.getUpdateDataFor(new Version("4.7.0"), "en", false, UpdateInformation.STYLE_MAJOR, null);
	    assertNull(data);
	    
	    // if we only want critical updates, this should find nothing.
	    data = uc.getUpdateDataFor(new Version("0.0.0"), "en", false, UpdateInformation.STYLE_CRITICAL, null);
	    assertNull(data);
	    
	    // find the english version.
	    data = uc.getUpdateDataFor(new Version("0.0.0"), "en", false, UpdateInformation.STYLE_MAJOR, null);
	    assertEquals("en", data.getLanguage());
	    assertEquals("<html><body>This is the text</body></html>", data.getUpdateText());
	    assertEquals("4.6.0", data.getUpdateVersion());
	    assertEquals(UpdateInformation.STYLE_MAJOR, data.getUpdateStyle());
	    assertNull(data.getButton1Text());
	    assertNull(data.getButton2Text());
	    
	    // find the spanish version.
	    data = uc.getUpdateDataFor(new Version("4.5.123509781 Pro"), "es", true, UpdateInformation.STYLE_MINOR, null);
	    assertEquals("es", data.getLanguage());
	    assertEquals("Hola, no habla espanol.", data.getUpdateText());
	    assertEquals("4.6.0", data.getUpdateVersion());
	    assertEquals(UpdateInformation.STYLE_MAJOR, data.getUpdateStyle());
	    assertEquals("b1", data.getButton1Text());
	    assertEquals("b2", data.getButton2Text());
	    
	    // can't find deutch, so defaults to english.
	    data = uc.getUpdateDataFor(new Version("4.0.0"), "de", false, UpdateInformation.STYLE_BETA, null);
	    assertEquals("en", data.getLanguage());
	    assertEquals("<html><body>This is the text</body></html>", data.getUpdateText());
	    assertEquals("4.6.0", data.getUpdateVersion());
	    assertEquals(UpdateInformation.STYLE_MAJOR, data.getUpdateStyle());
	    assertNull(data.getButton1Text());
	    assertNull(data.getButton2Text());
    }
    
    public void testRanges() throws Exception {
	    UpdateCollection uc = UpdateCollection.create(
	        "<update id='42'>" +
	            "<msg from='0.0.0' to='3.0.0' for='4.6.0' url='http://www.limewire.com/update/force' style='4'>" +
	                "<lang id='en'>FORCED Text</lang>" +
	            "</msg>" +
	            "<msg from='3.0.0' to='4.0.0' for='4.6.0' url='http://www.limewire.com/update/old' style='2'>" +
	                "<lang id='en'>Major Text (really old version)</lang>" +
	            "</msg>" +
	            "<msg from='4.0.0' for='4.6.0' url='http://www.limewire.com/update' style='2'>" +
	                "<lang id='en'>Major Text</lang>" +
	            "</msg>" +
	            "<msg from='4.6.0' for='4.6.5' url='http://www.limewire.com/update' style='1'>" +
	                "<lang id='en'>Text</lang>" +
	            "</msg>" +
	            "<msg from='4.6.5' to='4.7.2' for='4.7.3' url='http://www.limewire.com/beta' style='0'>" +
	                "<lang id='en'>Text</lang>" +
	            "</msg>" +
	            "<msg from='4.8.0' for='4.8.3' url='http://www.limewire.com/beta' style='3'>" +
	                "<lang id='en'>Text</lang>" +
	            "</msg>" +	            
	        "</update>");
	        
        // First make sure it ignored the invalid msgs.
        assertEquals(uc.getUpdateData().toString(), 6, uc.getUpdateData().size());
        assertEquals(42, uc.getId());
	    
	    // Idea:
	    // People who have [0.0.0, 3.0.0) are told about a FORCED update to 4.6.0 (with one set of text)
	    // People who have [3.0.0, 4.0.0) are told about a MAJOR update to 4.6.0  (with another set of text)
	    // People who have [4.0.0, 4.6.0) are told about a MAJOR update to 4.6.0  (with yet another set of text)
	    // People who have [4.6.0, 4.6.5) are told about a SERVICE update to 4.6.5
	    // People who have [4.6.5, 4.7.2) are told about a BETA update to 4.7.3
	    // People who have [4.8.0, 4.8.3) are told about a CRITICAL update to 4.8.3
	    // Note that the upper boundary is always exlusive, whereas the lower boundary is inclusive.
	    UpdateData data;
	    
	    data = uc.getUpdateDataFor(new Version("2.0.0"), "en", false, UpdateInformation.STYLE_BETA, null);
	    assertEquals("4.6.0", data.getUpdateVersion());
	    assertEquals("FORCED Text", data.getUpdateText());
	    data = uc.getUpdateDataFor(new Version("2.0.0"), "en", false, UpdateInformation.STYLE_MINOR, null);
	    assertEquals("4.6.0", data.getUpdateVersion());
	    assertEquals("FORCED Text", data.getUpdateText());
	    data = uc.getUpdateDataFor(new Version("2.0.0"), "en", false, UpdateInformation.STYLE_MAJOR, null);
	    assertEquals("4.6.0", data.getUpdateVersion());
	    assertEquals("FORCED Text", data.getUpdateText());
	    
	    data = uc.getUpdateDataFor(new Version("3.0.0"), "en", false, UpdateInformation.STYLE_BETA, null);
	    assertEquals("4.6.0", data.getUpdateVersion());
	    assertEquals("Major Text (really old version)", data.getUpdateText());
	    data = uc.getUpdateDataFor(new Version("3.0.0"), "en", false, UpdateInformation.STYLE_MINOR, null);
	    assertEquals("4.6.0", data.getUpdateVersion());
	    assertEquals("Major Text (really old version)", data.getUpdateText());
	    data = uc.getUpdateDataFor(new Version("3.0.0"), "en", false, UpdateInformation.STYLE_MAJOR, null);
	    assertEquals("4.6.0", data.getUpdateVersion());
	    assertEquals("Major Text (really old version)", data.getUpdateText());

	    data = uc.getUpdateDataFor(new Version("4.0.0"), "en", false, UpdateInformation.STYLE_BETA, null);
	    assertEquals("4.6.0", data.getUpdateVersion());
	    assertEquals("Major Text", data.getUpdateText());
	    data = uc.getUpdateDataFor(new Version("4.0.0"), "en", false, UpdateInformation.STYLE_MINOR, null);
	    assertEquals("4.6.0", data.getUpdateVersion());
	    assertEquals("Major Text", data.getUpdateText());
	    data = uc.getUpdateDataFor(new Version("4.0.0"), "en", false, UpdateInformation.STYLE_MAJOR, null);
	    assertEquals("4.6.0", data.getUpdateVersion());
	    assertEquals("Major Text", data.getUpdateText());

	    data = uc.getUpdateDataFor(new Version("4.6.0"), "en", false, UpdateInformation.STYLE_BETA, null);
	    assertEquals("4.6.5", data.getUpdateVersion());
	    data = uc.getUpdateDataFor(new Version("4.6.0"), "en", false, UpdateInformation.STYLE_MINOR, null);
	    assertEquals("4.6.5", data.getUpdateVersion());
	    data = uc.getUpdateDataFor(new Version("4.6.0"), "en", false, UpdateInformation.STYLE_MAJOR, null);
	    assertNull(data);
	    
	    data = uc.getUpdateDataFor(new Version("4.6.5"), "en", false, UpdateInformation.STYLE_BETA, null);
	    assertEquals("4.7.3", data.getUpdateVersion());
	    data = uc.getUpdateDataFor(new Version("4.6.5"), "en", false, UpdateInformation.STYLE_MINOR, null);
	    assertNull(data);
	    data = uc.getUpdateDataFor(new Version("4.6.5"), "en", false, UpdateInformation.STYLE_MAJOR, null);
	    assertNull(data);
	    
	    data = uc.getUpdateDataFor(new Version("4.7.1"), "en", false, UpdateInformation.STYLE_BETA, null);
	    assertEquals("4.7.3", data.getUpdateVersion());
	    data = uc.getUpdateDataFor(new Version("4.7.2"), "en", false, UpdateInformation.STYLE_BETA, null);
	    assertNull(data);
	    
	    data = uc.getUpdateDataFor(new Version("4.8.0"), "en", false, UpdateInformation.STYLE_BETA, null);
	    assertEquals("4.8.3", data.getUpdateVersion());
	    data = uc.getUpdateDataFor(new Version("4.8.0"), "en", false, UpdateInformation.STYLE_MINOR, null);
	    assertEquals("4.8.3", data.getUpdateVersion());
	    data = uc.getUpdateDataFor(new Version("4.8.0"), "en", false, UpdateInformation.STYLE_MAJOR, null);
	    assertEquals("4.8.3", data.getUpdateVersion());
	    data = uc.getUpdateDataFor(new Version("4.8.3"), "en", false, UpdateInformation.STYLE_MAJOR, null);
	    assertNull(data);
    }
            
}