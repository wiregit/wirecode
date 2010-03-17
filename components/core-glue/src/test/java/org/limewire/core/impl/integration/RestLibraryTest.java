package org.limewire.core.impl.integration;

import java.util.Map;
import java.util.Set;

/**
 * library integration tests for Rest APIs
 * 
 * NOTE this test is utilizing library files provided by superclass
 */
public class RestLibraryTest extends AbstractRestIntegrationTestcase {

    private static final String LIBRARY_PREFIX = "library";
    private static final String FILES_PREFIX = "library/files";

    private static final int TOTAL_FILES = 54;
    private static final int MAX_FILES = 50; // LWC-5428

    
    public RestLibraryTest(String name) throws Exception {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        loadLibraryFiles(10000);
    }

    // -------------------------- tests --------------------------

    public void testHelloWorld() throws Exception {
        String response = getHttpResponse("hello",NO_PARAMS);
        assertTrue("hello world response",response.endsWith("Hello world!"));
    }

    public void testBasicGets() throws Exception {
        validateLibraryMetadata(LIBRARY_PREFIX,NO_PARAMS,TOTAL_FILES);
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
        // not implemented in API yet (LWC-5528)
    }

    /**
     * test submitting meaningless garbage and fuzzy inputs
     */
    public void testNegatives() throws Exception {

        String response = "";

        // garbage parameters should be ignored & library data returned
        validateLibraryMetadata(LIBRARY_PREFIX,"this=is_garbage",TOTAL_FILES);
        validateLibraryMetadata(LIBRARY_PREFIX,"?something=test",TOTAL_FILES);
        validateLibraryResults(FILES_PREFIX,"this=is_garbage",MAX_FILES);
        validateLibraryResults(FILES_PREFIX,"type=654655",MAX_FILES);
        validateLibraryResults(FILES_PREFIX,"type=blah",MAX_FILES);
        validateLibraryResults(FILES_PREFIX,"?limit=1",MAX_FILES);

        // unexpected api library targets should return empty page
        response = getHttpResponse(LIBRARY_PREFIX + "/garbage",NO_PARAMS);
        assertTrue("expected empty response "+response,response.isEmpty());
        response = getHttpResponse("garbage",NO_PARAMS);
        assertTrue("expected empty response "+response,response.isEmpty());

        validateLibraryResults(FILES_PREFIX,"type="+bigString(4000),MAX_FILES);         
        /*
         * odd parameters don't throw exceptions (LWC-5523)
         * validateLibraryResults(FILES,"limit=2147483650",0); // longInt
         * response = getHttpResponse(FILES,"limit=not_a_number"); // datatype
         */
    }

    // ---------------------- private methods ----------------------

    /**
     * validate library metadata expectations
     */
    private Map validateLibraryMetadata(String target,String params,int expectedSize)
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
    protected Set<Map<String,String>> validateLibraryResults(String target,String params,
            int expectedSize) throws Exception {
        Set<Map<String,String>> resultSet = listGET(target,params);
        assertEquals("result size invalid: "+resultSet.size(),expectedSize,resultSet.size());
        assertTrue("results invalid "+libraryMap,libraryMap.containsAll(resultSet));
        return resultSet;
    }

}
