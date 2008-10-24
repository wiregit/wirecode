package org.limewire.core.impl.xmpp;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.auth.AUTH;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.protocol.BasicHttpContext;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.http.auth.ServerAuthState;
import org.limewire.util.BaseTestCase;
import org.limewire.util.StringUtils;

import com.limegroup.gnutella.library.FileList;
import com.limegroup.gnutella.library.FileManager;
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

    public void testGetFriendForValidEntries() throws Exception {
        BasicHttpRequest request = new BasicHttpRequest("GET", "/friend/me%40me.com");
        assertEquals("me@me.com", friendFileListProvider.getFriend(request));
        request = new BasicHttpRequest("GET", "/friend/me%40me.com/");
        assertEquals("me@me.com", friendFileListProvider.getFriend(request));
    }
    
    public void testGetFriendForInvalidEntries() {
        BasicHttpRequest request = new BasicHttpRequest("GET", "me%40me.com");
        try {
            friendFileListProvider.getFriend(request);
            fail("exception expected, not a valid request");
        } catch (HttpException he) {
        }
        request = new BasicHttpRequest("GET", "");
        try {
            friendFileListProvider.getFriend(request);
            fail("exception expected, not a valid request");
        } catch (HttpException he) {
        }
        request = new BasicHttpRequest("GET", "me:@me.com");
        try {
            friendFileListProvider.getFriend(request);
            fail("exception expected, not a valid request");
        } catch (HttpException he) {
        }
    }
    
    public void testGetFileListToBrowse() throws Exception {
        final FileList expectedFileList = context.mock(FileList.class);
        context.checking(new Expectations() { {
            one(fileManager).getFriendFileList("me@me.com");
            will(returnValue(expectedFileList));
        }});
        BasicHttpRequest request = new BasicHttpRequest("GET", "/friend/me%40me.com");
        request.addHeader(new BasicHeader(AUTH.WWW_AUTH_RESP, "Basic " + StringUtils.getASCIIString(Base64.encodeBase64(StringUtils.toUTF8Bytes("me@me.com:password")))));
        BasicHttpContext httpContext = new BasicHttpContext();
        ServerAuthState serverAuthState = new ServerAuthState();
        serverAuthState.setCredentials(new UsernamePasswordCredentials("me@me.com", "doesnotmatter"));
        httpContext.setAttribute(ServerAuthState.AUTH_STATE, serverAuthState);
        Iterable<FileList> fileLists = friendFileListProvider.getFileList(request, httpContext);
        assertSame(expectedFileList, fileLists.iterator().next());
        context.assertIsSatisfied();
    }
    
    public void testGetFileListToBrowseWithNoFileList() throws Exception {
        context.checking(new Expectations() { {
            one(fileManager).getFriendFileList("me@me.com");
            will(returnValue(null));
        }});
        BasicHttpRequest request = new BasicHttpRequest("GET", "/friend/me%40me.com");
        BasicHttpContext httpContext = new BasicHttpContext();
        ServerAuthState serverAuthState = new ServerAuthState();
        serverAuthState.setCredentials(new UsernamePasswordCredentials("me@me.com", "doesnotmatter"));
        httpContext.setAttribute(ServerAuthState.AUTH_STATE, serverAuthState);
        request.addHeader(new BasicHeader(AUTH.WWW_AUTH_RESP, "Basic " + StringUtils.getASCIIString(Base64.encodeBase64(StringUtils.toUTF8Bytes("me@me.com:me@me.com")))));
        try {
            friendFileListProvider.getFileList(request, httpContext);
            fail("should have thrown exception");
        } catch (HttpException e) {
        }
        context.assertIsSatisfied();
    }

}
