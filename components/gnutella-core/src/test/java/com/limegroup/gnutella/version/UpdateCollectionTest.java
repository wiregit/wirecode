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
}