package org.limewire.core.impl.integration;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.limewire.io.GUID;

public class SearchIntegrationTest extends BaseRestIntegrationTest {

    private static final String SEARCH = "search";
    private static final String FILES = "/files";

    
    public SearchIntegrationTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        loadLibraryFiles(10000);
        loadMockQueries();
    }

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
            System.out.println("fileByGUID: " + filesByGUID);
        }
    }

    /**
     * advanced searches, garbage/fuzzy tests etc?
     */

    // ---------------------- private ----------------------

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
        assertTrue("result found: " + queryMap, found);
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
