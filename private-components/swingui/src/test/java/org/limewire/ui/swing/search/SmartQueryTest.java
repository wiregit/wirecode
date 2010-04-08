package org.limewire.ui.swing.search;

import java.util.Map;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.util.FilePropertyKeyUtils;
import org.limewire.util.BaseTestCase;

/**
 * Test case for SmartQuery. 
 */
public class SmartQueryTest extends BaseTestCase {
    /** Instance of class being tested. */
    private SmartQuery smartQuery;

    private SearchCategory searchCategory;
    
    /**
     * Constructs a test case for the specified method name.
     */
    public SmartQueryTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        searchCategory = SearchCategory.AUDIO;
        smartQuery = new SmartQuery(searchCategory);
    }

    @Override
    protected void tearDown() throws Exception {
        smartQuery = null;
        searchCategory = null;
        super.tearDown();
    }

    /** Tests method to create new instance with single property value. */
    public void testNewInstanceOnePropertyValue() {
        // Create instance.
        smartQuery = SmartQuery.newInstance(searchCategory, FilePropertyKey.AUTHOR, "band");
        
        // Verify query data.
        Map<FilePropertyKey, String> queryData = smartQuery.getQueryData();
        assertEquals(1, queryData.size());
        assertNotNull(queryData.get(FilePropertyKey.AUTHOR));
    }

    /** Tests method to create new instance with two property values. */
    public void testNewInstanceTwoPropertyValues() {
        // Create instance.
        smartQuery = SmartQuery.newInstance(searchCategory, FilePropertyKey.AUTHOR, "band",
                FilePropertyKey.ALBUM, "hits");
        
        // Verify query data.
        Map<FilePropertyKey, String> queryData = smartQuery.getQueryData();
        assertEquals(2, queryData.size());
        assertNotNull(queryData.get(FilePropertyKey.AUTHOR));
        assertNotNull(queryData.get(FilePropertyKey.ALBUM));
    }

    /** Tests method to create new instance with three property values. */
    public void testNewInstanceThreePropertyValues() {
        // Create instance.
        smartQuery = SmartQuery.newInstance(searchCategory, FilePropertyKey.AUTHOR, "band",
                FilePropertyKey.ALBUM, "hits", FilePropertyKey.TITLE, "life");
        
        // Verify query data.
        Map<FilePropertyKey, String> queryData = smartQuery.getQueryData();
        assertEquals(3, queryData.size());
        assertNotNull(queryData.get(FilePropertyKey.AUTHOR));
        assertNotNull(queryData.get(FilePropertyKey.ALBUM));
        assertNotNull(queryData.get(FilePropertyKey.TITLE));
    }

    /** Tests method to add property value. */
    public void testAddData() {
        // Add property value.
        String artist = "band";
        smartQuery.addData(FilePropertyKey.AUTHOR, artist);
        
        // Verify query data.
        Map<FilePropertyKey, String> queryData = smartQuery.getQueryData();
        assertEquals(1, queryData.size());
        assertEquals(artist, queryData.get(FilePropertyKey.AUTHOR));
    }

    /** Tests method to retrieve display string. */
    public void testToString() {
        // Verify text with no data.
        assertTrue(smartQuery.toString().isEmpty());
        
        String artistKey = FilePropertyKeyUtils.getUntraslatedDisplayName(FilePropertyKey.AUTHOR, searchCategory);
        String albumKey = FilePropertyKeyUtils.getUntraslatedDisplayName(FilePropertyKey.ALBUM, searchCategory);
        String titleKey = FilePropertyKeyUtils.getUntraslatedDisplayName(FilePropertyKey.TITLE, searchCategory);
        String artist = "band";
        String album = "hits";
        String title = "life";
        
        // Verify one key.
        smartQuery.addData(FilePropertyKey.AUTHOR, artist);
        String expected = artistKey + " called \"" + artist + "\"";
        assertEquals(expected, smartQuery.toString());
        
        // Verify two keys.
        smartQuery.addData(FilePropertyKey.ALBUM, album);
        expected = artistKey + " \"" + artist + "\" - " + albumKey + " \"" + album + "\"";
        assertEquals(expected, smartQuery.toString());
        
        // Verify three keys.
        smartQuery.addData(FilePropertyKey.TITLE, title);
        expected = artistKey + " \"" + artist + "\" - " + albumKey + " \"" + album + "\" - " + titleKey + " \"" + title + "\"";
        assertEquals(expected, smartQuery.toString());
    }
}
