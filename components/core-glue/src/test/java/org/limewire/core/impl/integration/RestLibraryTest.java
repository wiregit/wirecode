package org.limewire.core.impl.integration;

import java.util.Map;
import java.util.Set;

import org.limewire.rest.RestPrefix;

/**
 * library integration tests for Rest APIs NOTE this test is utilizing library
 * files provided by superclass
 */
public class RestLibraryTest extends AbstractRestIntegrationTestcase {

    private static final String LIBRARY_PREFIX = RestPrefix.LIBRARY.pattern();
    private static final String FILES_PREFIX = LIBRARY_PREFIX+"/files";

    private static final int MAX_FILES = 50; // LWC-5428
    private static final int TOTAL_FILES = 54;

    public RestLibraryTest(String name) throws Exception {
        super(name);
    }

    @Override public void setUp() throws Exception {
        super.setUp();
        loadLibraryFiles();
    }

    // -------------------------- tests --------------------------

    public void testHelloWorld() throws Exception {
        String response = getHttpResponse("hello",NO_PARAMS);
        assertTrue("hello world response",response.endsWith("Hello world!"));
    }

    public void testBasicGets() throws Exception {
        validateLibraryMetadata(LIBRARY_PREFIX,NO_PARAMS,TOTAL_FILES);
        validateLibraryResults(FILES_PREFIX,NO_PARAMS,MAX_FILES);        
    }

    public void testLimits() throws Exception {
        validateLibraryResults(FILES_PREFIX,"limit=10",10);
        validateLibraryResults(FILES_PREFIX,"limit=49",MAX_FILES-1);
        validateLibraryResults(FILES_PREFIX,"limit=50",MAX_FILES);
        validateLibraryResults(FILES_PREFIX,"limit=1000",MAX_FILES);
    }

    public void testTypes() throws Exception {
        validateLibraryResults(FILES_PREFIX,"type=all",MAX_FILES);
        // other types not implemented in API yet (LWC-5427)
    }

    public void testOffsets() throws Exception {
        validateLibraryResults(FILES_PREFIX,"offset=0",MAX_FILES);
        validateLibraryResults(FILES_PREFIX,"offset=5",TOTAL_FILES-5);
        validateLibraryResults(FILES_PREFIX,"offset="+(TOTAL_FILES-1),1);
        validateLibraryResults(FILES_PREFIX,"offset=10000",0);
    }

    /**
     * test submitting meaningless garbage and fuzzy inputs
     */
    public void testNegatives() throws Exception {

        // negative offsets and limits
        // validateLibraryResults(FILES_PREFIX,"offset=-1",MAX_FILES); //LWC-5548
        validateLibraryResults(FILES_PREFIX,"limit=-1",0);

       
        // garbage parameters should be ignored & library data returned
        validateLibraryMetadata(LIBRARY_PREFIX,"this=is_garbage",TOTAL_FILES);
        validateLibraryMetadata(LIBRARY_PREFIX,"?something=test",TOTAL_FILES);
        validateLibraryResults(FILES_PREFIX,"this=is_garbage",MAX_FILES);
        validateLibraryResults(FILES_PREFIX,"type=654655",MAX_FILES);
        validateLibraryResults(FILES_PREFIX,"type=blah",MAX_FILES);
        validateLibraryResults(FILES_PREFIX,"?limit=1",MAX_FILES);

        // unexpected api library targets should return empty page
        assertResponseEmpty(LIBRARY_PREFIX+"/garbage",NO_PARAMS);
        assertResponseEmpty("garbage",NO_PARAMS);
        validateLibraryResults(FILES_PREFIX,"type="+bigString(4000),MAX_FILES);        
        /*
         * odd parameters don't throw exceptions (LWC-5523)
         * validateLibraryResults(FILES,"limit=2147483650",0); // longInt
         * response = getHttpResponse(FILES,"limit=not_a_number"); // datatype
         */        
        // library contains deleted file
        forceMissingLibFile();
        validateLibraryMetadata(LIBRARY_PREFIX,NO_PARAMS,TOTAL_FILES+1);
        validateLibraryResults(FILES_PREFIX,NO_PARAMS,MAX_FILES);        
    }

    // ---------------------- private methods ----------------------

    /**
     * validate library metadata expectations
     */
    private Map validateLibraryMetadata(String target, String params, int expectedSize)
            throws Exception {
        Map resultsMap = metadataGET(target,params);
        String size = (String) resultsMap.get("size");
        String id = (String) resultsMap.get("id");
        String name = (String) resultsMap.get("name");
        assertEquals("result size invalid: "+size,""+expectedSize,size);
        assertEquals("id invalid "+id,"library",id);
        assertEquals("name invalid "+name,"Library",name);
        return resultsMap;
    }

    /**
     * validate library results expectations
     */
    protected Set<Map<String,String>> validateLibraryResults(String target, String params,
            int expectedSize) throws Exception {
        Set<Map<String,String>> resultSet = listGET(target,params);
        assertEquals("result size invalid: "+resultSet.size(),expectedSize,resultSet.size());
        String assertInfo = "unexpected key diffs ----------EXPECT: "+librarySet
                +"---------- FOUND: "+resultSet;
        assertTrue(assertInfo,librarySet.containsAll(resultSet));
        return resultSet;
    }

}
