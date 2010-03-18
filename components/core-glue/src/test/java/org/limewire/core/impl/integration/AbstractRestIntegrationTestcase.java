package org.limewire.core.impl.integration;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.limewire.core.api.Category;
import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.impl.tests.CoreGlueTestUtils;
import org.limewire.core.settings.ApplicationSettings;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.http.httpclient.LimeHttpClient;
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
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileManagerTestUtils;
import com.limegroup.gnutella.library.Library;

public abstract class AbstractRestIntegrationTestcase extends LimeTestCase {

    protected static String LOCAL_REST_URL = "http://localhost:45100/remote/";
    protected static String SAMPLE_DIR = "sample_files";
    protected static final String NO_PARAMS = null;

    @Inject protected Injector injector;
    @Inject private LimeHttpClient client;
    @Inject protected Library library;
    @Inject protected CategoryManager categoryMgr;

    protected HashSet<Map<String,String>> libraryMap = null;

    public AbstractRestIntegrationTestcase(String name) {
        super(name);
    }

    @Override protected void setUp() throws Exception {
        setUpModules(Modules.EMPTY_MODULE);
    }

    protected void setUpModules(Module... modules) throws Exception {
        ApplicationSettings.LOCAL_REST_ACCESS_ENABLED.setValue(true);
        Module combined = Modules.combine(modules);
        CoreGlueTestUtils.createInjectorAndStart(combined,new MockRestModule(),LimeTestUtils
                .createModule(this));
        libraryMap = new HashSet<Map<String,String>>();
    }

    @Override protected void tearDown() throws Exception {
        injector.getInstance(LifecycleManager.class).shutdown();
    }

    // ---------------------- protected methods ----------------------

    /**
     * returns target JSONObject metadata in Map
     */
    protected Map<String,String> metadataGET(String target, String params) throws Exception {
        String response = getHttpResponse(target,params);
        JSONObject jobj = new JSONObject(response);
        return buildResultsMap(jobj);
    }

    /**
     * returns target JSONObject set in Hashset
     */
    protected Set<Map<String,String>> listGET(String target, String params) throws Exception {
        String response = getHttpResponse(target,params);
        JSONArray jarr = new JSONArray(response);
        Set<Map<String,String>> resultSet = buildResultSet(jarr);
        return resultSet;
    }

    /**
     * returns resulting JSONObject count
     */
    protected int listGETCount(String target, String params) throws Exception {
        String response = getHttpResponse(target,params);
        JSONArray jarr = new JSONArray(response);
        return jarr.length();
    }

    /**
     * performs http GET and returns response content string
     */
    protected String getHttpResponse(String target, String params) throws Exception {

        String responseStr = null;
        HttpResponse response = null;
        HttpGet method = new HttpGet(buildUrl(target,params));

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
    protected void loadLibraryFiles() throws Exception {

        File folder = TestUtils.getResourceInPackage(SAMPLE_DIR,getClass());
        List<FileDesc> files = FileManagerTestUtils.assertAddsFolder(library,folder,
                new FileFilter() {
                    public boolean accept(File pathname) {
                        if (!pathname.getPath().contains("CVS")) {
                            return true;
                        }
                        return false;
                    }
                });

        // build library file map for expectations
        for (FileDesc file : files) {
            HashMap<String,String> fileMap = new HashMap<String,String>();
            Category category = categoryMgr.getCategoryForFilename(file.getFileName());
            fileMap.put("category",category.getSingularName());
            fileMap.put("size",String.valueOf(file.getFileSize()));
            fileMap.put("filename",file.getFileName());
            fileMap.put("sha1Urn",getUrn(file));
            libraryMap.add(fileMap);
        }
    }

    /**
     * response is empty
     */
    @SuppressWarnings("unused") 
    protected void assertResponseEmpty(String target, String params)
            throws Exception {
        String r = getHttpResponse(target,params);
        boolean isEmpty = r.isEmpty()|r.equals("{}")|r.equals("[]");
        assertTrue("expected empty response: "+r,isEmpty);
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
    private Map<String,String> buildResultsMap(JSONObject jobj) throws Exception {
        HashMap<String,String> testmap = new HashMap<String,String>();
        Iterator iter = jobj.keys();
        while(iter.hasNext()) {
            String key = (String) iter.next();
            testmap.put(key,String.valueOf(jobj.get(key)));
        }
        return testmap;
    }

    /**
     * builds a Set containing JSONArray contents (as Strings)
     */
    private Set<Map<String,String>> buildResultSet(JSONArray jarr) throws Exception {
        HashSet<Map<String,String>> resultSet = new HashSet<Map<String,String>>();
        for (int i = 0;i<jarr.length();i++) {
            JSONObject jobj = jarr.getJSONObject(i);
            resultSet.add(buildResultsMap(jobj));
        }
        return resultSet;
    }

    /**
     * urn for file item w/ workaround for LWC-5478
     */
    private String getUrn(FileDesc file) {
        String shortUrn = file.getSHA1Urn().toString();
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
        if (params!=NO_PARAMS) {
            url.append("?").append(params);
        }
        return url.toString();
    }

    /**
     * mock authentication
     */
    private static class MockRestModule extends AbstractModule {
        @Override protected void configure() {
            bind(RestAuthorityFactory.class).toProvider(
                    FactoryProvider.newFactory(RestAuthorityFactory.class,MockRestAuthority.class));
        }
    }

    private static class MockRestAuthority implements RestAuthority {
        @Override public boolean isAuthorized(HttpRequest request) {
            return true;
        }
    }

}
