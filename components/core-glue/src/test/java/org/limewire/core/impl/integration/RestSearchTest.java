package org.limewire.core.impl.integration;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.limewire.io.GUID;

/**
 * search integration tests for Rest APIs
 * 
 * NOTE this test is utilizing library and query data provided by superclass
 */
public class RestSearchTest extends BaseRestTestCase {

    private static final String SEARCH = "search";
    private static final String FILES = "/files";
 
    
    public RestSearchTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        loadLibraryFiles(10000);
        loadMockQueries();
    }

    
    // -------------------------- tests --------------------------

    public void testSearchAll() throws Exception {
        Set<Map> resultSet = listGET(SEARCH, NO_PARAMS);
        for (Map map : resultSet) {
            validateMetadata(map);
        }
    }

    public void testSearchByGUID() throws Exception {
        for (GUID guid : guids) {
            Map queryByGUID = guidQueryGET(guid);
            validateMetadata(queryByGUID);
        }
    }

    public void testSearchFiles() throws Exception {
        for (GUID guid : guids) {
            Collection<Map> filesByGUID = guidQueryFilesGET(guid);
            validateFileData(filesByGUID);
        }
    }
    
    public void testByType() throws Exception {
        // not implemented in API yet (LWC-5427)
    }

    /**
     * advanced searches, garbage/fuzzy tests etc?
     */

    // ---------------------- private ----------------------

    private void validateFileData(Collection<Map> fileset) throws Exception {        
        for (Map filemap : fileset) {
            boolean found = false;
            for (int i = 0; i < filenames.length && !found; i++) {
                if (filemap.get("filename").equals(filenames[i]) 
                        && filemap.get("category").equals(cats[i].getPluralName())) {
                    found = true;
                }
            }
            assertTrue("not all results found in: " + filemap, found);            
        }
    }
    
    /**
     * validate results map expectations
     */
    private void validateMetadata(Map queryMap) throws Exception {
        boolean found = false;
        for (int i = 0; i < guids.length && !found; i++) {
            if (queryMap.get("id").equals(guids[i].toString())
                    && queryMap.get("name").equals(queryNames[i])
                    && queryMap.get("size").equals("" + filenames.length)) {
                found = true;
            }
        }
        assertTrue("result not found: " + queryMap, found);
    }

    /**
     * returns GET for a particular GUID query metadata
     */
    private Map guidQueryGET(GUID guid) throws Exception {
        String target = SEARCH + "/" + guid.toString();
        Map queryByGUID = metadataGET(target, NO_PARAMS);
        return queryByGUID;
    }

    /**
     * performs GET for a particular GUIDs results files
     */
    private Collection<Map> guidQueryFilesGET(GUID guid) throws Exception {
        String target = SEARCH + "/" + guid.toString() + FILES;
        Collection<Map> filesByGUID = listGET(target, NO_PARAMS);
        return filesByGUID;
    }

}
