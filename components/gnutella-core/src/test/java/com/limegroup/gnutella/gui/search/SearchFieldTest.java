package com.limegroup.gnutella.gui.search;

import javax.swing.text.BadLocationException;

import junit.framework.Test;

import com.limegroup.gnutella.gui.GUIBaseTestCase;
import com.limegroup.gnutella.gui.search.SearchField.SearchFieldDocument;
import com.limegroup.gnutella.settings.SearchSettings;

public class SearchFieldTest extends GUIBaseTestCase {

    public SearchFieldTest(String name) {
        super(name);
    }
    
    public static Test suite() { 
        return buildTestSuite(SearchFieldTest.class); 
    }
    
    /**
     * Tests if too long documents can be created. 
     * @throws BadLocationException 
     */
    public void testSearchFieldDocumentInsertString() throws BadLocationException {
        SearchFieldDocument doc = new SearchFieldDocument();
        int maxQueryLength = SearchSettings.MAX_QUERY_LENGTH.getValue();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < maxQueryLength; i++) {
            builder.append('a');
        }
        String s = builder.toString();
        doc.insertString(0, s, null);
        assertEquals(s, doc.getText(0, doc.getLength()));
        
        // testing if insertion at end fails
        doc.insertString(s.length(), "b", null);
        assertEquals(s, doc.getText(0, doc.getLength()));
        
        // testing if insertion at beginning fails
        doc.insertString(0, "b", null);
        assertEquals(s, doc.getText(0, doc.getLength()));
        
        // testing if insertion in the middle fails
        doc.insertString(doc.getLength() / 2, "b", null);
        assertEquals(s, doc.getText(0, doc.getLength()));
        
        // testing for length - 1
        doc.insertString(doc.getLength() - 1, "b", null);
        assertEquals(s, doc.getText(0, doc.getLength()));
        
        doc = new SearchFieldDocument();
        // too long string by one
        s += "a";
        doc.insertString(0, s, null);
        assertEquals(0, doc.getLength());
        
        // emulate user input
        s = "hello world foo bar";
        assertTrue(s.length() < maxQueryLength);
        for (char c : s.toCharArray()) {
            doc.insertString(doc.getLength(), Character.toString(c), null);
        }
        assertEquals(s, doc.getText(0, doc.getLength()));
    }

}
