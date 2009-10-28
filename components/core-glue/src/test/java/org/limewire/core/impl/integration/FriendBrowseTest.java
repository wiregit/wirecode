package org.limewire.core.impl.integration;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicHeader;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.limewire.core.impl.tests.CoreGlueTestUtils;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.http.auth.Authenticator;
import org.limewire.http.auth.AuthenticatorRegistry;
import org.limewire.http.httpclient.HttpClientUtils;
import org.limewire.http.httpclient.LimeHttpClient;
import org.limewire.io.LocalSocketAddressProvider;
import org.limewire.io.LocalSocketAddressProviderStub;
import org.limewire.util.TestUtils;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.LifecycleManager;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnSet;
import com.limegroup.gnutella.library.FileCollectionManager;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileManagerTestUtils;
import com.limegroup.gnutella.library.Library;
import com.limegroup.gnutella.library.SharedFileCollection;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.Message.Network;

public class FriendBrowseTest extends LimeTestCase {
    
    private static final String FILE_NAME = "alphabet test file#2.txt";

    private static final String FRIEND_ID = "me@you.com";
    
    @Inject private Injector injector;
    @Inject private Acceptor acceptor;
    private int port;
    @Inject private FileCollectionManager fileManager;

    @Inject private LimeHttpClient client;
    @Inject private Library library;
    @Inject private MessageFactory messageFactory;
    
    private FileDesc fileDesc;

    private Mockery context;
    
    public FriendBrowseTest(String name) {
        super(name);
    }
    
    @Override
    protected void setUp() throws Exception {
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        context = new Mockery();
        CoreGlueTestUtils.createInjectorAndStart(TestUtils.bind(LocalSocketAddressProvider.class).to(LocalSocketAddressProviderStub.class), LimeTestUtils.createModule(this));
        port = acceptor.getPort(false);
        FileManagerTestUtils.waitForLoad(library, 4 * 1000);
        loadFiles();
    }
    
    private void loadFiles() throws Exception {
        SharedFileCollection friendFileList = fileManager.createNewCollection("test collection");
        friendFileList.addFriend(FRIEND_ID);
        File testFile = TestUtils.getResourceInPackage(FILE_NAME, getClass());
        fileDesc = friendFileList.add(testFile).get();
        // add fake nms1 urn
        fileDesc.addUrn(URN.createNMS1FromBytes(fileDesc.getSHA1Urn().getBytes()));
    }

    @Override
    protected void tearDown() throws Exception {
        injector.getInstance(LifecycleManager.class).shutdown();
    }
    
    public void testFriendBrowseIncludeNMS1UrnInQuery() throws Exception {
        HttpGet request = new HttpGet("http://localhost:" + port+ "/friend/browse/me%40you.com/?nms1=1");
        friendBrowse(request, true);
    }
    
    public void testFriendBrowseNoNMS1Urn() throws Exception {
        HttpGet request = new HttpGet("http://localhost:" + port+ "/friend/browse/me%40you.com/");
        friendBrowse(request, false);
    }
    
    public void testFriendBrowseNMS1UrnInHeader() throws Exception {
        HttpGet request = new HttpGet("http://localhost:" + port+ "/friend/browse/me%40you.com/");
        request.addHeader(new BasicHeader("X-NMS1", "1"));
        friendBrowse(request, true);
    }
    
    public void testFriendBrowseNMS1UrnInHeaderAndExtraHeader() throws Exception {
        HttpGet request = new HttpGet("http://localhost:" + port+ "/friend/browse/me%40you.com/");
        request.addHeader(new BasicHeader("X-NMS1", "1"));
        request.addHeader(new BasicHeader("foo", "bar"));
        friendBrowse(request, true);
    }
    
    private void friendBrowse(HttpUriRequest request, boolean expectNMS1Urn) throws Exception {
        registerAuthenticator(true);
        
        request.addHeader("Accept", "application/x-gnutella-packets");
        // add authentication header
        // auth data: me@you.com:hello
        request.addHeader("Authorization", " Basic bWVAeW91LmNvbTpoZWxsbw==");
        HttpResponse response = null;
        try {
             response = client.execute(request);
             assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
             InputStream in = new BufferedInputStream(response.getEntity().getContent());
             List<Response> responses = new ArrayList<Response>();
             while (true){
                 try {
                     Message message = messageFactory.read(in, Network.TCP);
                     assertInstanceof(QueryReply.class, message);
                     QueryReply reply = (QueryReply)message;
                     responses.addAll(reply.getResultsAsList());
                 } catch (IOException ie) {
                     if (!"Connection closed.".equals(ie.getMessage())) {
                         throw ie;
                     }
                     break; 
                 }
             }
             assertEquals(1, responses.size());
             URN nms1Urn = UrnSet.getNMS1(responses.get(0).getUrns());
             if (expectNMS1Urn) {
                 assertEquals(fileDesc.getNMS1Urn(), nms1Urn);
             } else {
                 assertNull(nms1Urn);
             }
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
        context.assertIsSatisfied();
    }
    
    private void registerAuthenticator(final boolean authenticate) {
        AuthenticatorRegistry registry = injector.getInstance(AuthenticatorRegistry.class);
        final Authenticator authenticator = context.mock(Authenticator.class);
        registry.register(authenticator);
        context.checking(new Expectations() {{
            one(authenticator).authenticate(with(any(UsernamePasswordCredentials.class)));
            will(new CustomAction("check password") {
                @Override
                public Object invoke(Invocation invocation) throws Throwable {
                    UsernamePasswordCredentials credentials = (UsernamePasswordCredentials)invocation.getParameter(0);
                    assertEquals("me@you.com", credentials.getUserName());
                    assertEquals("hello", credentials.getPassword());
                    return authenticate;
                }
            });
        }});  
    }
}
