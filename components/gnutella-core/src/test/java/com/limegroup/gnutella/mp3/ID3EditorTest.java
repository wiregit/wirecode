package com.limegroup.gnutella.mp3;

import junit.framework.Test;

import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.PrivilegedAccessor;

/**
 * Unit tests for ID3Editor
 */
public class ID3EditorTest extends BaseTestCase {
    MP3DataEditor mine = new MP3DataEditor();
        
	public ID3EditorTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(ID3EditorTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}



    public void testRipTag() throws Exception {
        
        String source = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audios.xsd\"><audio bitrate=\"192\" genre=\"Blues\"/></audios>";
        //        String source = "bitrate=\"192\" genre=\"Susheel\"";

        Object[] test = ripTag(source, "bitrate");
        assertEquals(new Integer(110), test[0]);
        assertEquals(new Integer(124), test[1]);
        assertEquals("192", test[2]);

        {
            int i = ((Integer)test[0]).intValue();
            int j = ((Integer)test[1]).intValue();
            source = source.substring(0,i) + source.substring(j,source.length());
        }

        test = ripTag(source, "genre");
        assertEquals(new Integer(110), test[0]);
        assertEquals(new Integer(124), test[1]);
        assertEquals("Blues", test[2]);
        
        {
            int i = ((Integer)test[0]).intValue();
            int j = ((Integer)test[1]).intValue();
            source = source.substring(0,i) + source.substring(j,source.length());
        }        
        
        String expected = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audios.xsd\"><audio/></audios>";
        assertEquals(expected, source);
        assertEquals(121, source.length());
        
        
    }
    
    private Object[] ripTag(String source, String tag) throws Exception {
        return (Object[])PrivilegedAccessor.invokeMethod(
            mine, "ripTag", new Object[] { source, tag } );
    }
}
