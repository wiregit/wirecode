package org.limewire.core.impl.integration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpStatus;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.GroupedSearchResult;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchManager;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.SearchResultList;
import org.limewire.io.GUID;
import org.limewire.rest.RestPrefix;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;

/**
 * search integration tests for Rest APIs
 * 
 * NOTE this test is utilizing library and query data provided by superclass
 */
public class RestSearchTest extends AbstractRestIntegrationTestcase {

    private static final String SEARCH_PREFIX = RestPrefix.SEARCH.pattern();;
    private static final String FILES_PREFIX = "/files";
    private static final String FAKEGUID = "/BA8DB600AC11FE2EE3033F5AFF57F500";

    // mock query data
    private final String[] queryNames = { "what is available", "speedster", "time runs by" };
    private final String[] filenames = { "superfly.mp3", "quantum.doc", "simplistic mind.mov",
            "another time.mp3" };
    private final GUID[] guids = { new GUID(), new GUID(), new GUID() };
    private final Category[] cats = { Category.AUDIO, Category.DOCUMENT, Category.VIDEO,
            Category.AUDIO };
    protected static Mockery context = new Mockery();

    @Inject private SearchManager searchMgr;

    public RestSearchTest(String name) {
        super(name);
    }

    @Override public void setUp() throws Exception {

        String nm = getName();
        if (nm.contains("NoMock")) {
            super.setUp(); // no mocks
        } else {
            setUpModules(new MockSearchModule());
            if (!nm.contains("NoLoad")) {
                loadMockQueries(); // load queries
            }
        }
    }

    // -------------------------- tests --------------------------

    public void testSearchAll() throws Exception {
        Set<Map<String,String>> resultSet = listGET(SEARCH_PREFIX,NO_PARAMS);
        for (Map map : resultSet) {
            validateMetadata(map);
        }
    }

    public void testSearchByGUID() throws Exception {
        for (GUID guid : guids) {
            Map<String,String> queryByGUID = guidQueryGET(guid);
            validateMetadata(queryByGUID);
        }
    }

    public void testSearchFiles() throws Exception {
        for (GUID guid : guids) {
            Collection<Map<String,String>> filesByGUID = guidQueryFilesGET(guid);
            validateFileData(filesByGUID);
        }
    }

    public void testLimits() throws Exception {

        int fcnt = filenames.length;
        Collection<Map<String,String>> filesByGUID = null;

        // huge limit
        filesByGUID = guidQueryFilesGET(guids[0],"limit=10000");
        assertEquals("count unexpected",fcnt,filesByGUID.size());

        // file count less one
        filesByGUID = guidQueryFilesGET(guids[0],"limit="+(fcnt-1));
        assertEquals("count unexpected",fcnt-1,filesByGUID.size());

        // file count plus one
        filesByGUID = guidQueryFilesGET(guids[0],"limit="+(fcnt+1));
        assertEquals("count unexpected",fcnt,filesByGUID.size());

        // one item
        filesByGUID = guidQueryFilesGET(guids[0],"limit=1");
        assertEquals("count unexpected",1,filesByGUID.size());

        // negative limit
        filesByGUID = guidQueryFilesGET(guids[0],"limit=-10");
        assertEquals("count unexpected",0,filesByGUID.size());
    }

    public void testOffsets() throws Exception {

        int fcnt = filenames.length;
        Collection<Map<String,String>> filesByGUID = null;

        // huge limit
        filesByGUID = guidQueryFilesGET(guids[0],"offset=10000");
        assertEquals("count unexpected",0,filesByGUID.size());

        // file count less one
        filesByGUID = guidQueryFilesGET(guids[0],"offset="+(fcnt-1));
        assertEquals("count unexpected",1,filesByGUID.size());

        // file count plus one
        filesByGUID = guidQueryFilesGET(guids[0],"offset="+(fcnt+1));
        assertEquals("count unexpected",0,filesByGUID.size());

        // one item
        filesByGUID = guidQueryFilesGET(guids[0],"offset=1");
        assertEquals("count unexpected",(fcnt-1),filesByGUID.size());
    }

    public void testPostDeleteNoMock() throws Exception {

        int statusCode = -1;
        HashSet<String> guidset = new HashSet<String>();
        
        assertSearchCount(0);
 
        // post some queries        
        for (int i=0;i<10;i++) {
            String guid = queryPost("q=monkey+on+a+stick"+i).get("id");
            assertResponseEmpty(SEARCH_PREFIX+guid+FILES_PREFIX,NO_PARAMS); // we can ask for files
            guidset.add(guid);
        }       
        assertSearchCount(10);
        
         // delete all queries
        for (String guid : guidset) {
            statusCode = guidQueryDelete(guid);
            assertEquals("status code unexpected",HttpStatus.SC_OK,statusCode);
            statusCode = guidQueryDelete(guid);  // it's really gone
            assertEquals("query not deleted",HttpStatus.SC_NOT_FOUND,statusCode);    
            assertResponseEmpty(SEARCH_PREFIX+guid,NO_PARAMS);            
        }
        assertSearchCount(0);        
    }

    public void testNegativesNoMock() throws Exception {

        Map<String,String> map = null;
        
        // invalid GUIDs,targets
        assertResponseEmpty(SEARCH_PREFIX+FAKEGUID,NO_PARAMS);
        assertResponseEmpty(SEARCH_PREFIX+FAKEGUID+FILES_PREFIX,NO_PARAMS);
        assertResponseEmpty(SEARCH_PREFIX+"/garbage",NO_PARAMS);
        assertResponseEmpty(SEARCH_PREFIX+FAKEGUID+"/foo",NO_PARAMS);

        // garbage parameters
        assertResponseEmpty(SEARCH_PREFIX,"this=is_garbage");
        assertResponseEmpty(SEARCH_PREFIX,"?something=test");
        assertResponseEmpty(SEARCH_PREFIX,"this=is_garbage");
        
        // post checks
        map = metadataPost(SEARCH_PREFIX,"q="+bigString(250));
        assertTrue("guid should have been generated",map.get("id") != null);        
        /** // LWC-5550
         * map = metadataPost(SEARCH_PREFIX,"no_q_arg"); 
         * map = metadataPost(SEARCH_PREFIX,"q=");         
         * map = metadataPost(SEARCH_PREFIX,"t=bad_arg"); 
         * assertEquals("query should not have been posted",0,map.size());
         */
        // check delete fake query
        int statusCode = guidQueryDelete(FAKEGUID);
        assertEquals("status code unexpected",HttpStatus.SC_NOT_FOUND,statusCode);  
        
        // can't delete twice
        String guid = queryPost("q=monkey").get("id");        
        statusCode = guidQueryDelete(guid);
        statusCode = guidQueryDelete(guid);
        assertEquals("status code unexpected",HttpStatus.SC_NOT_FOUND,statusCode);          
    }

    public void testHugeNoLoad() throws Exception {

        loadHugeQuerySet();

        int queryCnt = listGETCount(SEARCH_PREFIX,NO_PARAMS);
        assertTrue("qryCnt invalid: "+queryCnt,queryCnt==1000);

        Map queryByGUID = guidQueryGET(guids[0]);
        String sz = (String) queryByGUID.get("size");
        assertTrue("file sz invalid: "+sz,sz.equals("20000"));

        String target = SEARCH_PREFIX+"/"+guids[0].toString()+FILES_PREFIX;
        int filecnt = listGETCount(target,NO_PARAMS);
        assertTrue("filecnt "+filecnt,filecnt==1000);
    }

    // ---------------------- private ----------------------


    private void validateFileData(Collection<Map<String,String>> fileset) throws Exception {
        for (Map filemap : fileset) {
            boolean found = false;
            for (int i = 0;i<filenames.length&&!found;i++) {
                if (filemap.get("filename").equals(filenames[i])
                        &&filemap.get("category").equals(cats[i].getSchemaName())) {
                    found = true;
                }
            }
            assertTrue("results not found for: "+filemap.get("filename"),found);
        }
    }

    /**
     * validate results map expectations
     */
    private void validateMetadata(Map queryMap) throws Exception {
        boolean found = false;
        for (int i = 0;i<guids.length&&!found;i++) {
            if (queryMap.get("id").equals(guids[i].toString())
                    &&queryMap.get("name").equals(queryNames[i])
                    &&queryMap.get("size").equals(""+filenames.length)) {
                found = true;
            }
        }
        assertTrue("results not found for: "+queryMap.get("name"),found);
    }

    /**
     * returns GET for a particular GUID query metadata
     */
    private Map<String,String> guidQueryGET(GUID guid) throws Exception {
        String target = SEARCH_PREFIX+"/"+guid.toString();
        Map<String,String> queryByGUID = metadataGET(target,NO_PARAMS);
        return queryByGUID;
    }

    /**
     * performs GET for a particular GUIDs results files
     */
    private Collection<Map<String,String>> guidQueryFilesGET(GUID guid, String params)
            throws Exception {
        String target = SEARCH_PREFIX+"/"+guid.toString()+FILES_PREFIX;
        Collection<Map<String,String>> filesByGUID = listGET(target,params);
        return filesByGUID;
    }

    private Collection<Map<String,String>> guidQueryFilesGET(GUID guid) throws Exception {
        return guidQueryFilesGET(guid,NO_PARAMS);
    }

    /**
     * performs DELETE for a particular GUID search
     */
    private int guidQueryDelete(String guid) throws Exception {
        String target = SEARCH_PREFIX+"/"+guid+FILES_PREFIX;
        int status = httpDelete(target,NO_PARAMS);
        return status;
    }

    /**
     * check active search count
     */
    private void assertSearchCount(int expectCnt) {
        int qsize = searchMgr.getActiveSearchLists().size();
        assertEquals("unexpected queryCnt",expectCnt,qsize);
    }
    
    /**
     * creates a query
     */
    private Map<String,String> queryPost(String query) throws Exception {
        Map<String,String> map = metadataPost(SEARCH_PREFIX,query);
        return map;
    }

    /**
     * populates mock query results. four result files for each query
     */
    public void loadMockQueries() throws Exception {
        loadMockQueries(false);
    }

    public void loadHugeQuerySet() throws Exception {
        loadMockQueries(true);
    }

    public void loadMockQueries(final boolean isHuge) throws Exception {

        final SearchResultList mockResultList = context.mock(SearchResultList.class);
        final GroupedSearchResult mockGroupedResult = context.mock(GroupedSearchResult.class);
        final SearchResult mockSearchResult = context.mock(SearchResult.class);
        final Search mockSearch = context.mock(Search.class);

        final List<SearchResultList> searchResultLists = new ArrayList<SearchResultList>();
        final EventList<GroupedSearchResult> groupedResults = new BasicEventList<GroupedSearchResult>();
        final List<SearchResult> searchResults = new ArrayList<SearchResult>();
        final List<RemoteHost> remoteHosts = new ArrayList<RemoteHost>();

        int queryCnt = (isHuge ? 1000 : queryNames.length);
        int fileCnt = (isHuge ? 20000 : filenames.length);
        for (int i = 0;i<queryCnt;i++) {
            searchResultLists.add(mockResultList);
        }
        for (int j = 0;j<fileCnt;j++) {
            groupedResults.add(mockGroupedResult);
        }
        searchResults.add(mockSearchResult);

        context.checking(new Expectations() {
            {
                // all searches metadata
                allowing(searchMgr).getActiveSearchLists();
                will(returnValue(searchResultLists));
                allowing(mockResultList).getGuid();
                if (!isHuge) {
                    will(onConsecutiveCalls(returnValue(guids[0]),returnValue(guids[1]),
                            returnValue(guids[2]),returnValue(guids[0]),returnValue(guids[1]),
                            returnValue(guids[2])));
                } else {
                    will(returnValue(guids[0]));
                }

                allowing(mockResultList).getSearchQuery();
                if (!isHuge) {
                    will(onConsecutiveCalls(returnValue(queryNames[0]),returnValue(queryNames[1]),
                            returnValue(queryNames[2]),returnValue(queryNames[0]),
                            returnValue(queryNames[1]),returnValue(queryNames[2])));
                } else {
                    will(returnValue(queryNames[0]));
                }
                allowing(mockResultList).getGroupedResults();
                will(returnValue(groupedResults));

                // individual search metadata
                allowing(searchMgr).getSearchResultList(with(any(GUID.class)));
                will(returnValue(mockResultList));

                allowing(mockResultList).getSearch();
                will(returnValue(mockSearch));
                allowing(mockSearch);

                // individual search files
                allowing(mockGroupedResult).getFileName();
                if (!isHuge) {
                    will(onConsecutiveCalls(returnValue(filenames[0]),returnValue(filenames[1]),
                            returnValue(filenames[2]),returnValue(filenames[3]),
                            returnValue(filenames[0]),returnValue(filenames[1]),
                            returnValue(filenames[2]),returnValue(filenames[3]),
                            returnValue(filenames[0]),returnValue(filenames[1]),
                            returnValue(filenames[2]),returnValue(filenames[3]),
                            returnValue(filenames[0]),returnValue(filenames[1]),
                            returnValue(filenames[2]),returnValue(filenames[3])));
                } else {
                    will(returnValue(filenames[0]));
                }

                allowing(mockGroupedResult).getSearchResults();
                will(returnValue(searchResults));

                allowing(mockSearchResult).getProperty(with(any(FilePropertyKey.class)));
                will(returnValue("unset"));
                allowing(mockSearchResult).getSize();
                allowing(mockSearchResult).getMagnetURL();
                allowing(mockSearchResult).isSpam();
                allowing(mockSearchResult).getCategory();
                if (!isHuge) {
                    will(onConsecutiveCalls(returnValue(cats[0]),returnValue(cats[1]),
                            returnValue(cats[2]),returnValue(cats[3]),returnValue(cats[0]),
                            returnValue(cats[1]),returnValue(cats[2]),returnValue(cats[3]),
                            returnValue(cats[0]),returnValue(cats[1]),returnValue(cats[2]),
                            returnValue(cats[3]),returnValue(cats[0]),returnValue(cats[1]),
                            returnValue(cats[2]),returnValue(cats[3])));
                } else {
                    will(returnValue(cats[0]));
                }
                allowing(mockGroupedResult).getSources();
                will(returnValue(remoteHosts));
                allowing(mockGroupedResult).getUrn();

                allowing(searchMgr);
            }
        });
    }
    
    /**
     * mock search
     */
    private static class MockSearchModule extends AbstractModule {
        @Override protected void configure() {
            bind(SearchManager.class).toInstance(context.mock(SearchManager.class));
        }
    }

}
