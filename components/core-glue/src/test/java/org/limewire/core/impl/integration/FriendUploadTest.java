package org.limewire.core.impl.integration;


/**
 * Collection of friend upload integration tests.
 */
public class FriendUploadTest extends IntegrationTestCase {

//    private static final String TEST_DIR_NAME = "com/limegroup/gnutella/uploader/data";
//
//    private static final String FILE_NAME = "alphabet test file#2.txt";
//
//    private static final String FRIEND_ID = "me@you.com";
//    
//    private Injector injector;
//    private Acceptor acceptor;
//    private int port;
//    private FileManager fileManager;
//
//    private String relativeFileNameUrl;
//
//    private LimeHttpClient client;
//
//    private FileDesc fileDesc;
//
//    private Mockery context;
    
    public FriendUploadTest(String name) {
        super(name);
    }
    
    @Override
    protected void setUp() throws Exception {
//        context = new Mockery();
//        injector = CoreGlueTestUtils.createInjectorAndStart(LocalSocketAddressProviderStub.STUB_MODULE);
//        acceptor = injector.getInstance(Acceptor.class);
//        port = acceptor.getPort(false);
//        client = injector.getInstance(LimeHttpClient.class);
//        fileManager = injector.getInstance(FileManager.class);
////        FileManagerTestUtils.waitForLoad(fileManager, 4 * 1000);
//        loadFiles();
    }
    
//    private void loadFiles() throws Exception {
//        FriendFileList friendFileList = fileManager.getOrCreateFriendFileList(FRIEND_ID);
//        File testDir = TestUtils.getResourceFile(TEST_DIR_NAME);
//        fileDesc = friendFileList.add(new File(testDir, FILE_NAME)).get();
//        relativeFileNameUrl = LimeTestUtils.getRelativeRequest(fileDesc.getSHA1Urn());
//    }
//
//    @Override
//    protected void tearDown() throws Exception {
//        injector.getInstance(LifecycleManager.class).shutdown();
//    }
//    
//    /**
//     * Ensures unauthenticated download request fails and requestor receives basic
//     * authentication challenge and connection stays open.
//     * Then ensures that authenticated request goes through.
//     */
//    public void testUnauthenticatedFriendDownloadGetsChallengeResponse() throws Exception {
//        HttpGet method = new HttpGet("http://localhost:" + port+ "/friend/download/me%40you.com" + relativeFileNameUrl);
//        HttpResponse response = null;
//        try {
//            response = client.execute(method);
//            assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatusLine().getStatusCode());
//            assertEquals("Basic realm=\"secure\"", response.getFirstHeader("WWW-Authenticate").getValue());
//        } finally {
//            HttpClientUtils.releaseConnection(response);
//        }
//        assertConnectionIsOpen(true, method);
//
//        // see if everything works if authentication is sent correctly
//        testAuthenticatedFriendGetRequestIsOKed();
//    }
//    
//    /**
//     * Ensures that an authenticated request without challenge/response is OKed immediately 
//     */
//    public void testAuthenticatedFriendGetRequestIsOKed() throws Exception {
//        registerAuthenticator(true);
//        HttpGet method = new HttpGet("http://localhost:" + port+ "/friend/download/me%40you.com" + relativeFileNameUrl);
//        // add authentication header
//        // auth data: me@you.com:hello
//        method.addHeader("Authorization", " Basic bWVAeW91LmNvbTpoZWxsbw==");
//        HttpResponse response = null;
//        try {
//             response = client.execute(method);
//             assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
//             String contents = EntityUtils.toString(response.getEntity());
//             assertEquals("abcdefghijklmnopqrstuvwxyz", contents);
//        } finally {
//            HttpClientUtils.releaseConnection(response);
//        }
//        assertConnectionIsOpen(true, method);
//        
//        context.assertIsSatisfied();
//    }
//    
//    public void testWrongAuthenticationIsRejectedAsUnauthorized() throws Exception {
//        registerAuthenticator(false);
//        HttpGet method = new HttpGet("http://localhost:" + port+ "/friend/download/me%40you.com" + relativeFileNameUrl);
//        // add authentication header
//        // auth data: me@you.com:hello
//        method.addHeader("Authorization", " Basic bWVAeW91LmNvbTpoZWxsbw==");
//        HttpResponse response = null;
//        try {
//             response = client.execute(method);
//             assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatusLine().getStatusCode());
//         } finally {
//            HttpClientUtils.releaseConnection(response);
//        }
//        assertConnectionIsOpen(true, method);
//        
//        context.assertIsSatisfied();
//    }
//    
//    public void testAuthenticatedRangeHeadDownload() throws Exception {
//        registerAuthenticator(true);
//        HttpHead method = new HttpHead("http://localhost:" + port+ "/friend/download/me%40you.com" + relativeFileNameUrl);
//        // add authentication header
//        // auth data: me@you.com:hello
//        method.addHeader("Authorization", " Basic bWVAeW91LmNvbTpoZWxsbw==");
//        method.addHeader("Range", "bytes 2-5");
//        HttpResponse response = null;
//        try {
//             response = client.execute(method);
//             assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response.getStatusLine().getStatusCode());
//             assertNotNull(response.getFirstHeader("Content-range"));
//             assertEquals("bytes 2-5/26", response.getFirstHeader(
//                     "Content-range").getValue());
//             assertNull(response.getEntity());
//         } finally {
//            HttpClientUtils.releaseConnection(response);
//        }
//        assertConnectionIsOpen(true, method);
//        
//        context.assertIsSatisfied();
//    }
//    
//    public void testConnectionCloseHeaderIsHonoredAuthenticated() throws Exception {
//        testConnectionCloseHeaderIsHonored(true, HttpStatus.SC_OK);
//    }
//    
//    public void testConnectionCloseHeaderIsHonoredUnAuthenticated() throws Exception {
//        testConnectionCloseHeaderIsHonored(false, HttpStatus.SC_UNAUTHORIZED);
//    }
//    
//    public void testConnectionCloseHeaderIsHonored(boolean authenticate, int code) throws Exception {
//        registerAuthenticator(authenticate);
//        HttpGet method = new HttpGet("http://localhost:" + port+ "/friend/download/me%40you.com" + relativeFileNameUrl);
//        // add authentication header
//        // auth data: me@you.com:hello
//        method.addHeader("Authorization", " Basic bWVAeW91LmNvbTpoZWxsbw==");
//        method.addHeader("Connection", "close");
//        HttpResponse response = null;
//        try {
//             response = client.execute(method);
//             assertEquals(code, response.getStatusLine().getStatusCode());
//        } finally {
//            HttpClientUtils.releaseConnection(response);
//        }
//        assertConnectionIsOpen(false, method);
//        
//        context.assertIsSatisfied();
//    }
//    
//    public void testFriendRequestIsNeverBanned() throws Exception {
//        PrivilegedAccessor.setValue(RequestCache.class, "FIRST_CHECK_TIME",
//                new Long(10 * 1000));
//        registerAuthenticator(true);
//        HttpGet method = new HttpGet("http://localhost:" + port+ "/friend/download/me%40you.com" + relativeFileNameUrl);
//        // add authentication header
//        // auth data: me@you.com:hello
//        method.addHeader("Authorization", " Basic bWVAeW91LmNvbTpoZWxsbw==");
//        HttpResponse response = null;
//        try {
//             response = client.execute(method);
//             assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
//             String contents = EntityUtils.toString(response.getEntity());
//             assertEquals("abcdefghijklmnopqrstuvwxyz", contents);
//        } finally {
//            HttpClientUtils.releaseConnection(response);
//        }
//        assertConnectionIsOpen(true, method);
//        
//        context.assertIsSatisfied();
//    }
//    
//    private void registerAuthenticator(final boolean authenticate) {
//        AuthenticatorRegistry registry = injector.getInstance(AuthenticatorRegistry.class);
//        final Authenticator authenticator = context.mock(Authenticator.class);
//        registry.register(authenticator);
//        context.checking(new Expectations() {{
//            one(authenticator).authenticate(with(any(UsernamePasswordCredentials.class)));
//            will(new CustomAction("check passowrd") {
//                @Override
//                public Object invoke(Invocation invocation) throws Throwable {
//                    UsernamePasswordCredentials credentials = (UsernamePasswordCredentials)invocation.getParameter(0);
//                    assertEquals("me@you.com", credentials.getUserName());
//                    assertEquals("hello", credentials.getPassword());
//                    return authenticate;
//                }
//            });
//        }});  
//    }
//    
//    private void assertConnectionIsOpen(boolean open, HttpUriRequest request) throws InterruptedException, ConnectionPoolTimeoutException {
//        URI uri = request.getURI();
//        HttpRoute route = new HttpRoute(new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme()));
//        ManagedClientConnection connection = client.getConnectionManager().requestConnection(route, null).getConnection(0, null);
//        assertEquals(open, connection.isOpen());
//        client.getConnectionManager().releaseConnection(connection, -1, null);
//    }

}
