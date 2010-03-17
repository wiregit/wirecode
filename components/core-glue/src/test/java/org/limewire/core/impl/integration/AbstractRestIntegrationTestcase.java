package org.limewire.core.impl.integration;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.json.JSONArray;
import org.json.JSONObject;
import org.limewire.concurrent.ListeningFuture;
import org.limewire.core.api.library.FileProcessingEvent;
import org.limewire.core.api.library.LibraryFileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.impl.tests.CoreGlueTestUtils;
import org.limewire.core.settings.ApplicationSettings;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.http.httpclient.LimeHttpClient;
import org.limewire.listener.EventListener;
import org.limewire.rest.RestAuthority;
import org.limewire.rest.RestAuthorityFactory;
import org.limewire.util.TestUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.assistedinject.FactoryProvider;
import com.google.inject.util.Modules;
import com.limegroup.gnutella.LifecycleManager;

public abstract class AbstractRestIntegrationTestcase extends LimeTestCase {

    protected static String LOCAL_REST_URL = "http://localhost:45100/remote/";
    protected static String SAMPLE_DIR = "sample_files";
    protected static final String NO_PARAMS = null;

    @Inject protected Injector injector;
    @Inject private LimeHttpClient client;
    @Inject protected LibraryManager libraryMgr;

    protected HashSet<Map<String, String>> libraryMap = null;

    
    public AbstractRestIntegrationTestcase(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        setUpModules(Modules.EMPTY_MODULE);
    }
    
    protected void setUpModules(Module... modules) throws Exception {
        ApplicationSettings.LOCAL_REST_ACCESS_ENABLED.setValue(true);
        Module combined = Modules.combine(modules);        
        CoreGlueTestUtils.createInjectorAndStart(combined,new MockRestModule(),LimeTestUtils
                .createModule(this));
        libraryMap = new HashSet<Map<String, String>>();        
    }    

    @Override
    protected void tearDown() throws Exception {
        injector.getInstance(LifecycleManager.class).shutdown();
    }

    // ---------------------- protected methods ----------------------

    /**
     * returns target JSONObject metadata in Map
     */
    protected Map<String, String> metadataGET(String target, String params) throws Exception {
        String response = getHttpResponse(target, params);
        JSONObject jobj = new JSONObject(response);
        return buildResultsMap(jobj);
    }

    /**
     * returns target JSONObject set in Hashset
     */
    protected Set<Map<String, String>> listGET(String target, String params) throws Exception {
        String response = getHttpResponse(target, params);
        JSONArray jarr = new JSONArray(response);
        Set<Map<String, String>> resultSet = buildResultSet(jarr);
        return resultSet;
    }

    /**
     * returns resulting JSONObject count
     */
    protected int listGETCount(String target, String params) throws Exception {
        String response = getHttpResponse(target, params);
        JSONArray jarr = new JSONArray(response);
        return jarr.length();
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

        libraryMap = new HashSet<Map<String, String>>();
        File folder = TestUtils.getResourceInPackage(SAMPLE_DIR, getClass());

        // load files
        final AtomicInteger aint = new AtomicInteger(0);
        LibraryFileList fileList = libraryMgr.getLibraryManagedList();
        ListeningFuture future = fileList.addFolder(folder, new FileFilter() {
            public boolean accept(File pathname) {
                if (!pathname.toString().contains("CVS")) {
                    aint.incrementAndGet();
                    return true;
                }
                return false;
            }
        });
        /*
        FileManagerTestUtils.assertFutureListFinishes(future,10,TimeUnit.SECONDS);
        */
       
        // wait for load to complete
        final CountDownLatch latch = new CountDownLatch(aint.intValue());
        fileList.addFileProcessingListener(new EventListener<FileProcessingEvent>() {
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
     * generates a huge string for negative testing
     */
    protected String bigString(int size) {
        char[] chars = new char[size];
        Arrays.fill(chars,'a');        
        return new String(chars);
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
    private Set<Map<String, String>> buildResultSet(JSONArray jarr) throws Exception {
        HashSet<Map<String, String>> resultSet = new HashSet<Map<String, String>>();
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
     * returns a url using rest url,target,and params
     */
    private String buildUrl(String target, String params) {
        StringBuffer url = new StringBuffer(LOCAL_REST_URL).append(target);
        if (params != NO_PARAMS) {
            url.append("?").append(params);
        }
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
        }
    }

    private static class MockRestAuthority implements RestAuthority {
        @Override
        public boolean isAuthorized(HttpRequest request) {
            return true;
        }
    }   
    
}
