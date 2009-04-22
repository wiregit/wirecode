package org.limewire.core.impl.xmpp;

import junit.framework.Test;

import org.apache.http.HttpStatus;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.http.auth.ServerAuthState;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.library.SharedFileCollection;
import com.limegroup.gnutella.uploader.HttpException;

public class FriendFileListProviderTest extends BaseTestCase {
    
    private Mockery context;
    private FileManager fileManager;
    private FriendFileListProvider friendFileListProvider;
        
    
    public FriendFileListProviderTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
        fileManager = context.mock(FileManager.class);
        friendFileListProvider = new FriendFileListProvider(fileManager);
    }
    
    public static Test suite() {
        return buildTestSuite(FriendFileListProviderTest.class);
    }

    public void testGetListsForValidUserId() throws Exception {
        context.checking(new Expectations() {{
            one(fileManager).getFriendFileList("me@you.com");
            will(returnValue(context.mock(SharedFileCollection.class)));
        }});
        
        HttpContext httpContext = new BasicHttpContext();
        ServerAuthState authState = new ServerAuthState();
        authState.setCredentials(new UsernamePasswordCredentials("me@you.com", "password"));
        httpContext.setAttribute(ServerAuthState.AUTH_STATE, authState);
        
        friendFileListProvider.getFileLists("me@you.com", httpContext);
        
        context.assertIsSatisfied();
    }
    
    public void testGetListsUserIdAndCredentialsDontMatch() {
        HttpContext httpContext = new BasicHttpContext();
        ServerAuthState authState = new ServerAuthState();
        authState.setCredentials(new UsernamePasswordCredentials("me@you.com", "password"));
        httpContext.setAttribute(ServerAuthState.AUTH_STATE, authState);
        
        try {
            friendFileListProvider.getFileLists("hello@world.com", httpContext);
            fail("expected exception");
        } catch (HttpException he) {
            assertEquals(HttpStatus.SC_UNAUTHORIZED, he.getErrorCode());
        }
    }
    
    public void testGetListsUserIdHasNoFileList() {
        context.checking(new Expectations() {{
            one(fileManager).getFriendFileList("me@you.com");
            will(returnValue(null));
        }});
        
        HttpContext httpContext = new BasicHttpContext();
        ServerAuthState authState = new ServerAuthState();
        authState.setCredentials(new UsernamePasswordCredentials("me@you.com", "password"));
        httpContext.setAttribute(ServerAuthState.AUTH_STATE, authState);
        
        try {
            friendFileListProvider.getFileLists("me@you.com", httpContext);
            fail("expected exception");
        } catch (HttpException he) {
            assertEquals(HttpStatus.SC_NOT_FOUND, he.getErrorCode());
        }
        
        context.assertIsSatisfied();
    }
    
    
    public void testGetFileListsWithBadCredentials() {
        HttpContext httpContext = new BasicHttpContext();
        try {
            friendFileListProvider.getFileLists(null, httpContext);
            fail("Did not throw exception for attempt without user.");
        } catch (HttpException e) {
            // Expected
        }
        
        try {
            friendFileListProvider.getFileLists("tester@test", httpContext);
            fail("Did not throw exception for attempt without an AuthState.");
        } catch (HttpException e) {
            // Expected
        }
        
        ServerAuthState authState = new ServerAuthState();
        httpContext.setAttribute(ServerAuthState.AUTH_STATE, authState);
        
        try {
            friendFileListProvider.getFileLists("tester@test", httpContext);
            fail("Did not throw exception for attempt without credentials.");
        } catch (HttpException e) {
            // Expected 
        }
    }
}