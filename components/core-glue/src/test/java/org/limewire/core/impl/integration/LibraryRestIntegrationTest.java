package org.limewire.core.impl.integration;

import java.util.Map;
import java.util.Set;

/**
 * library integration tests for Rest APIs
 * 
 * NOTE this test is utilizing library files provided by super.setUp()
 */
public class LibraryRestIntegrationTest extends BaseRestIntegrationTest {

    private static final String LIBRARY = "library";
    private static final String FILES = "library/files";
    private static final int TOTAL_FILES = 54;
    private static final int MAX_FILES = 50; // LWC-5428

    
    public LibraryRestIntegrationTest(String name) throws Exception {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        loadLibraryFiles(10000);
    }

    // -------------------------- tests --------------------------

    public void testBasicGets() throws Exception {
        validateLibraryMetadata(LIBRARY, NO_PARAMS, TOTAL_FILES);
    }

    public void testLimits() throws Exception {
        validateLibraryResults(FILES, "limit=10", 10);
        validateLibraryResults(FILES, "limit=50", MAX_FILES);
        validateLibraryResults(FILES, "limit=1000", MAX_FILES);
    }

    public void testTypes() throws Exception {
        validateLibraryResults(FILES, "type=all", MAX_FILES);
    }

    /**
     * TODO: - test for specific results - test submitting meaningless garbage
     * and fuzzy inputs
     */

    // ---------------------- private methods ----------------------

    /**
     * validate library metadata expectations
     */
    private Map validateLibraryMetadata(String target, String params, int expectedSize)
            throws Exception {
        Map resultsMap = metadataGET(target, params);
        String size = (String) resultsMap.get("size");
        assertEquals("result size invalid: " + size, "" + expectedSize, size);
        assertEquals("id invalid", "library", resultsMap.get("id"));
        assertEquals("name invalid", "Library", resultsMap.get("name"));
        return resultsMap;
    }

    /**
     * validate library results expectations
     */
    protected Set<Map> validateLibraryResults(String target, String params, int expectedSize)
            throws Exception {
        Set<Map> resultSet = listGET(target, params);
        assertEquals("result size invalid: " + resultSet.size(), expectedSize, resultSet.size());
        assertTrue("results invalid " + libraryMap, libraryMap.containsAll(resultSet));
        return resultSet;
    }

}
