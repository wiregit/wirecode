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
	
	public void testCreation() {
	    
	    UpdateCollection uc = UpdateCollection.create(
	        "<update id='42' timestamp=\"150973213135\">" +
	            "<msg for='1.2.3' os='*'>" +
	                "<lang id='en' url='http://www.limewire.com/update' current='4.6.0'>" +
	                    "<![CDATA[<html><body>This is the text</body></html>]]>" +
	                "</lang>" +
	                "<lang id='es' url='http://www.limewire.com/update/es' current='4.6.0'>" +
	                    "<![CDATA[<html><body>Hola, no habla espanol.</body></html>]]>" +
	                "</lang>" +
	                "<lang id='bad' current='4.6.0'>" +
	                    "<![CDATA[<html><body>This is the text</body></html>]]>" +
	                "</lang>" +
	                "<lang id='badder' url='http://www.limewire.com/update'>" +
	                    "<![CDATA[<html><body>This is the text</body></html>]]>" +
	                "</lang>" +
	                "<lang id='worst' url='http://www.limewire.com/update' current='4.6.0'>" +
	                "</lang>" +
	            "</msg>" +
	            "<msg for='3.2.4' os='Windows, Unix'>" +
	                "<lang id='en' url='http://www.limewire.com/update' current='4.1.2'>" +
	                    "<![CDATA[<html><body>This is the other text.</body></html>]]>" +
	                "</lang>" +
	                "<lang id='es' url='http://www.limewire.com/update/es' current='4.3.4'>" +
	                    "<![CDATA[<html><body>Hola, no habla espanol (other).</body></html>]]>" +
	                "</lang>" +
	            "</msg>" +	            
	        "</update>");
	    
	    System.out.println(uc.toString());
    }
}