package com.limegroup.gnutella.mp3;

import junit.framework.*;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.gnutella.util.BaseTestCase;
import com.sun.java.util.collections.*;

/**
 * Unit tests for ID3Editor
 */
public class ID3EditorTest extends BaseTestCase {
    ID3Editor mine = new ID3Editor();
        
	public ID3EditorTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(ID3EditorTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}


   public void testRemoveID3Tags() throws Exception {
        String source = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audios.xsd\"><audio bitrate=\"192\" genre=\"Blues\" title=\"HonkyTonk Man\" artist=\"Elvis\" album=\"Live at Five\" year=\"1978\" comments=\"wiggidy wack!\" leftover=\"stay here\" track=\"3\"/></audios>";
        String expected = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audios.xsd\"><audio leftover=\"stay here\"/></audios>";
        String after = mine.removeID3Tags(source);
        assertEquals(expected, after);
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
