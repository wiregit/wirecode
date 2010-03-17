package org.limewire.core.impl.integration;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.json.JSONArray;
import org.json.JSONObject;
import org.limewire.core.api.Category;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.library.FileProcessingEvent;
import org.limewire.core.api.library.LibraryFileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.search.GroupedSearchResult;
import org.limewire.core.api.search.SearchManager;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.SearchResultList;
import org.limewire.core.impl.tests.CoreGlueTestUtils;
import org.limewire.core.settings.ApplicationSettings;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.http.httpclient.LimeHttpClient;
import org.limewire.io.GUID;
import org.limewire.listener.EventListener;
import org.limewire.rest.RestAuthority;
import org.limewire.rest.RestAuthorityFactory;
import org.limewire.util.TestUtils;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.assistedinject.FactoryProvider;
import com.limegroup.gnutella.LifecycleManager;

public class BaseRestTestCase extends LimeTestCase {

    protected static String LOCAL_REST_URL = "http://localhost:45100/remote/";
    protected static String SAMPLE_DIR = "sample_files";    
    protected static final String NO_PARAMS = null;
    
    // mock query data
    protected static Mockery context = new Mockery();    
    protected static String[] queryNames = { "what is available", "speedster", "time runs by" };
    protected static String[] filenames = { "superfly.mp3", "quantum.doc", "simplistic mind.mov","another time.mp3" };
    protected static GUID[] guids = { new GUID(), new GUID(), new GUID() };
    protected static Category[] cats = { Category.AUDIO, Category.DOCUMENT, Category.VIDEO, Category.AUDIO };

    @Inject private Injector injector;
    @Inject private LimeHttpClient client;
    @Inject protected LibraryManager libraryMgr;
    @Inject protected SearchManager searchMgr;
    
    
    protected HashSet<Map> libraryMap = new HashSet<Map>();

    
    public BaseRestTestCase(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        ApplicationSettings.LOCAL_REST_ACCESS_ENABLED.setValue(true);
        CoreGlueTestUtils.createInjectorAndStart(new MockRestModule(), LimeTestUtils
                .createModule(this));
    }

    @Override
    protected void tearDown() throws Exception {
        injector.getInstance(LifecycleManager.class).shutdown();
    }

    // -------------------------- tests --------------------------

    public void testHelloWorld() throws Exception {
        String response = getHttpResponse("hello", NO_PARAMS);
        assertTrue("hello world response", response.endsWith("Hello world!"));
    }

    // ---------------------- protected methods ----------------------

    /**
     * returns target JSONObject metadata in Map
     */
    protected Map metadataGET(String target, String params) throws Exception {
        String response = getHttpResponse(target, params);
        JSONObject jobj = new JSONObject(response);
        return buildResultsMap(jobj);
    }

    /**
     * returns target JSONObject set in Hashset
     */
    protected Set<Map> listGET(String target, String params) throws Exception {
        String response = getHttpResponse(target, params);
        JSONArray jarr = new JSONArray(response);
        Set<Map> resultSet = buildResultSet(jarr);
        return resultSet;
    }

    /**
     * performs http GET and returns response content string
     */
    protected String getHttpResponse(String target, String params) throws Exception {

        String responseStr = null;
        HttpResponse response = null;
        HttpGet method = new HttpGet(buildUrl(target, params));

        try {
            response = client.execute(method);
            HttpEntity entity = response.getEntity();
            responseStr = EntityUtils.toString(entity);
        } finally {
            client.releaseConnection(response);
        }
        return responseStr;
    }

    /**
     * loads sample files to library
     */
    protected void loadLibraryFiles(int timeout) throws Exception {

        libraryMap = new HashSet<Map>();
        File folder = TestUtils.getResourceInPackage(SAMPLE_DIR, getClass());

        // load files
        final AtomicInteger aint = new AtomicInteger(0);
        LibraryFileList fileList = libraryMgr.getLibraryManagedList();
        libraryMgr.getLibraryManagedList().addFolder(folder, new FileFilter() {
            public boolean accept(File pathname) {
                if (!pathname.toString().contains("CVS")) {
                    aint.incrementAndGet();
                    return true;
                }
                return false;
            }
        });
        // wait for load to complete
        final CountDownLatch latch = new CountDownLatch(aint.intValue());
        fileList.addFileProcessingListener(new EventListener<FileProcessingEvent>() {
            @Override
            public void handleEvent(FileProcessingEvent event) {
                if (event.getType().equals("FINISHED")) {
                    latch.countDown();
                }
            }
        });
        latch.await(timeout, TimeUnit.MILLISECONDS);
        Thread.sleep(1000);

        // build library file map for expectations
        List<LocalFileItem> fileItemList = new ArrayList<LocalFileItem>(fileList.getModel());
        for (LocalFileItem file : fileItemList) {
            HashMap<String, String> fileMap = new HashMap<String, String>();
            fileMap.put("category", file.getCategory().getSingularName());
            fileMap.put("size", String.valueOf(file.getSize()));
            fileMap.put("filename", file.getFileName());
            fileMap.put("sha1Urn", getUrn(file));
            libraryMap.add(fileMap);
        }
    }

    /**
     * populates mock query results. nine result files for each query
     */
    public void loadMockQueries() throws Exception {

        final SearchResultList mockResultList = context.mock(SearchResultList.class);
        final GroupedSearchResult mockGroupedResult = context.mock(GroupedSearchResult.class);
        final SearchResult mockSearchResult = context.mock(SearchResult.class);

        final List<SearchResultList> searchResultLists = new ArrayList<SearchResultList>();
        final EventList<GroupedSearchResult> groupedResults = new BasicEventList<GroupedSearchResult>();
        final List<SearchResult> searchResults = new ArrayList<SearchResult>();
        final List<RemoteHost> remoteHosts = new ArrayList<RemoteHost>();

        searchResultLists.add(mockResultList);
        searchResultLists.add(mockResultList);
        searchResultLists.add(mockResultList);
        groupedResults.add(mockGroupedResult);
        groupedResults.add(mockGroupedResult);
        groupedResults.add(mockGroupedResult);
        groupedResults.add(mockGroupedResult);

        searchResults.add(mockSearchResult);

        context.checking(new Expectations() {
            {
                // all searches metadata
                allowing(searchMgr).getActiveSearchLists();
                will(returnValue(searchResultLists));
                allowing(mockResultList).getGuid();
                will(onConsecutiveCalls(returnValue(guids[0]), returnValue(guids[1]),
                        returnValue(guids[2]), returnValue(guids[0]), returnValue(guids[1]),
                        returnValue(guids[2])));

                allowing(mockResultList).getSearchQuery();
                will(onConsecutiveCalls(returnValue(queryNames[0]), returnValue(queryNames[1]),
                        returnValue(queryNames[2]), returnValue(queryNames[0])));
                allowing(mockResultList).getGroupedResults();
                will(returnValue(groupedResults));

                // individual search metadata
                allowing(searchMgr).getSearchResultList(with(any(String.class)));
                will(returnValue(mockResultList));

                // individual search files
                allowing(mockGroupedResult).getFileName();
                will(onConsecutiveCalls(returnValue(filenames[0]), returnValue(filenames[1]),
                        returnValue(filenames[2]), returnValue(filenames[3]),
                        returnValue(filenames[0]), returnValue(filenames[1]),
                        returnValue(filenames[2]), returnValue(filenames[3]),
                        returnValue(filenames[0]), returnValue(filenames[1]),
                        returnValue(filenames[2]), returnValue(filenames[3]),
                        returnValue(filenames[0]), returnValue(filenames[1]),
                        returnValue(filenames[2]), returnValue(filenames[3])));

                allowing(mockGroupedResult).getSearchResults();
                will(returnValue(searchResults));
                allowing(mockSearchResult).getCategory();
                will(onConsecutiveCalls(returnValue(cats[0]), returnValue(cats[1]),
                        returnValue(cats[2]), returnValue(cats[3]), returnValue(cats[0]),
                        returnValue(cats[1]), returnValue(cats[2]), returnValue(cats[3]),
                        returnValue(cats[0]), returnValue(cats[1]), returnValue(cats[2]),
                        returnValue(cats[3]), returnValue(cats[0]), returnValue(cats[1]),
                        returnValue(cats[2]), returnValue(cats[3])));
                allowing(mockGroupedResult).getSources();
                will(returnValue(remoteHosts));

                allowing(searchMgr);

            }
        });
    }

    // ---------------------- private methods ----------------------

    /**
     * builds a Map of JSON object contents (as Strings)
     */
    private Map<String, String> buildResultsMap(JSONObject jobj) throws Exception {
        HashMap<String, String> testmap = new HashMap<String, String>();
        Iterator iter = jobj.keys();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            testmap.put(key, String.valueOf(jobj.get(key)));
        }
        return testmap;
    }

    /**
     * builds a Set containing JSONArray contents (as Strings)
     */
    private Set<Map> buildResultSet(JSONArray jarr) throws Exception {
        HashSet<Map> resultSet = new HashSet<Map>();
        for (int i = 0; i < jarr.length(); i++) {
            JSONObject jobj = jarr.getJSONObject(i);
            resultSet.add(buildResultsMap(jobj));
        }
        return resultSet;
    }

    /**
     * urn for file item w/ workaround for LWC-5478
     */
    private String getUrn(LocalFileItem file) {
        String shortUrn = file.getUrn().toString();
        if (shortUrn.startsWith("urn:sha1:")) {
            shortUrn = shortUrn.substring(9);
        }
        return shortUrn;
    }

    /**
     * returns a url using rest url, target, and params
     */
    private String buildUrl(String target, String params) {
        StringBuffer url = new StringBuffer(LOCAL_REST_URL).append(target);
        if (params != NO_PARAMS) {
            url.append("?").append(params);
        }
        System.out.println("buildUrl: " + url.toString());
        return url.toString();
    }

    /**
     * mock authentication
     */
    private static class MockRestModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(RestAuthorityFactory.class)
                    .toProvider(
                            FactoryProvider.newFactory(RestAuthorityFactory.class,
                                    MockRestAuthority.class));
            bind(SearchManager.class).toInstance(context.mock(SearchManager.class));
        }
    }

    private static class MockRestAuthority implements RestAuthority {
        @Override
        public boolean isAuthorized(HttpRequest request) {
            return true;
        }
    }
}
