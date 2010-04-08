package org.limewire.ui.swing.search;

import java.util.Collection;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.util.BaseTestCase;

/**
 * Test case for SmartAutoCompleteDictionary. 
 */
public class SmartAutoCompleteDictionaryTest extends BaseTestCase {
    /** Instance of class being tested. */
    private SmartAutoCompleteDictionary smartDictionary;

    private SearchCategory searchCategory;
    private Mockery context;

    /**
     * Constructs a test case for the specified method name.
     */
    public SmartAutoCompleteDictionaryTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        // Create mockery.
        context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        // Create mock keyword search builder.
        final KeywordAssistedSearchBuilder keywordSearchBuilder = context.mock(KeywordAssistedSearchBuilder.class);
        context.checking(new Expectations() {{
            allowing(keywordSearchBuilder).getTranslatedKeySeprator();
            will(returnValue(":"));
        }});
        
        // Create test instances.
        searchCategory = SearchCategory.AUDIO;
        smartDictionary = new SmartAutoCompleteDictionary(searchCategory, keywordSearchBuilder);
    }

    @Override
    protected void tearDown() throws Exception {
        smartDictionary = null;
        searchCategory = null;
        super.tearDown();
    }

    /** Tests constructor. */
    public void testSmartAutoCompleteDictionary() {
        assertEquals(searchCategory, smartDictionary.getSearchCategory());
    }

    /** Tests method to retrieve collection of smart queries. */
    public void testGetPrefixedBy() {
        // Get smart queries.
        String input = "band";
        Collection<SmartQuery> smartQueries = smartDictionary.getPrefixedBy(input);
        
        // Verify queries.
        assertEquals(3, smartQueries.size());
        for (SmartQuery query : smartQueries) {
            assertEquals(1, query.getQueryData().size());
        }
    }

    /** Tests method to retrieve collection of smart queries. */
    public void testGetPrefixedByWithOneDash() {
        // Get smart queries.
        String input = "band - hits";
        Collection<SmartQuery> smartQueries = smartDictionary.getPrefixedBy(input);
        
        // Verify queries.
        assertEquals(5, smartQueries.size());
        for (SmartQuery query : smartQueries) {
            int size = query.getQueryData().size();
            assertTrue(size > 0 && size < 3);
        }
    }

    /** Tests method to retrieve collection of smart queries. */
    public void testGetPrefixedByWithTwoDashes() {
        // Get smart queries.
        String input = "band - hits - life";
        Collection<SmartQuery> smartQueries = smartDictionary.getPrefixedBy(input);
        
        // Verify queries.
        assertEquals(7, smartQueries.size());
        for (SmartQuery query : smartQueries) {
            int size = query.getQueryData().size();
            assertTrue(size > 0 && size < 4);
        }
    }

    /** Tests method to retrieve collection of smart queries. */
    public void testGetPrefixedByWithColon() {
        // When input specifies field with a colon, verify smart queries are empty.
        String input = "artist:band";
        Collection<SmartQuery> smartQueries = smartDictionary.getPrefixedBy(input);
        assertTrue(smartQueries.isEmpty());
    }

    /** Tests method to retrieve immediate indicator. */
    public void testIsImmediate() {
        assertTrue(smartDictionary.isImmediate());
    }
}
