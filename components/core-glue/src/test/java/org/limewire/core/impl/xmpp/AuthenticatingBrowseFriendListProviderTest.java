package org.limewire.core.impl.xmpp;

import java.util.Arrays;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.auth.AUTH;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.protocol.BasicHttpContext;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.security.MACCalculator;
import org.limewire.security.SecurityToken.TokenData;
import org.limewire.util.BaseTestCase;
import org.limewire.util.StringUtils;

import com.limegroup.gnutella.FileList;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.uploader.HttpException;

public class AuthenticatingBrowseFriendListProviderTest extends BaseTestCase {

    private Mockery context;
    private FileManager fileManager;
    private MACCalculator calculator;
    private AuthenticatingBrowseFriendListProvider browseFriendListProvider;

    public AuthenticatingBrowseFriendListProviderTest(String name) {
        super(name);
    }
    
    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
        fileManager = context.mock(FileManager.class);
        calculator = context.mock(MACCalculator.class);
        browseFriendListProvider = new AuthenticatingBrowseFriendListProvider(fileManager, calculator);
    }

    public void testGetFriendForValidEntries() throws Exception {
        BasicHttpRequest request = new BasicHttpRequest("GET", "/friend/me%40me.com");
        assertEquals("me@me.com", browseFriendListProvider.getFriend(request));
        request = new BasicHttpRequest("GET", "/friend/me%40me.com/");
        assertEquals("me@me.com", browseFriendListProvider.getFriend(request));
    }
    
    public void testGetFriendForInvalidEntries() {
        BasicHttpRequest request = new BasicHttpRequest("GET", "me%40me.com");
        try {
            browseFriendListProvider.getFriend(request);
            fail("exception expected, not a valid request");
        } catch (HttpException he) {
        }
        request = new BasicHttpRequest("GET", "");
        try {
            browseFriendListProvider.getFriend(request);
            fail("exception expected, not a valid request");
        } catch (HttpException he) {
        }
        request = new BasicHttpRequest("GET", "me:@me.com");
        try {
            browseFriendListProvider.getFriend(request);
            fail("exception expected, not a valid request");
        } catch (HttpException he) {
        }
    }
    
    public void testGetFileListToBrowse() throws Exception {
        final FileList expectedFileList = context.mock(FileList.class);
        context.checking(new Expectations() { {
            one(calculator).getMACBytes(with(new BaseMatcher<TokenData>() {
                @Override
                public boolean matches(Object item) {
                    TokenData tokenData = (TokenData)item;
                    return Arrays.equals(tokenData.getData(), StringUtils.toUTF8Bytes("me@me.com"));
                }
                @Override
                public void describeTo(Description description) {
                }
            }));
            will(returnValue(StringUtils.toUTF8Bytes("password")));
            one(fileManager).getFriendFileList("me@me.com");
            will(returnValue(expectedFileList));
        }});
        BasicHttpRequest request = new BasicHttpRequest("GET", "/friend/me%40me.com");
        request.addHeader(new BasicHeader(AUTH.WWW_AUTH_RESP, "Basic " + StringUtils.getASCIIString(Base64.encodeBase64(StringUtils.toUTF8Bytes("me@me.com:password")))));
        FileList fileList = browseFriendListProvider.getFileList(request, new BasicHttpContext());
        assertSame(expectedFileList, fileList);
        context.assertIsSatisfied();
    }
    
    public void testGetFileListToBrowseWithNoFileList() throws Exception {
        context.checking(new Expectations() { {
            one(calculator).getMACBytes(with(new BaseMatcher<TokenData>() {
                @Override
                public boolean matches(Object item) {
                    TokenData tokenData = (TokenData)item;
                    return Arrays.equals(tokenData.getData(), StringUtils.toUTF8Bytes("me@me.com"));
                }
                @Override
                public void describeTo(Description description) {
                }
            }));
            will(returnValue(StringUtils.toUTF8Bytes("me@me.com")));
            one(fileManager).getFriendFileList("me@me.com");
            will(returnValue(null));
        }});
        BasicHttpRequest request = new BasicHttpRequest("GET", "/friend/me%40me.com");
        request.addHeader(new BasicHeader(AUTH.WWW_AUTH_RESP, "Basic " + StringUtils.getASCIIString(Base64.encodeBase64(StringUtils.toUTF8Bytes("me@me.com:me@me.com")))));
        try {
            browseFriendListProvider.getFileList(request, new BasicHttpContext());
            fail("should have thrown exception");
        } catch (HttpException e) {
        }
        context.assertIsSatisfied();
    }

}
