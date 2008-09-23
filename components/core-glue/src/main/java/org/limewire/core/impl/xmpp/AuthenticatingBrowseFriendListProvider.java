package org.limewire.core.impl.xmpp;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.apache.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.apache.http.RequestLine;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpContext;
import org.limewire.http.BasicAuthenticationRequestInterceptor;
import org.limewire.security.MACCalculator;
import org.limewire.security.SecurityToken.TokenData;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.limegroup.gnutella.FileList;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.uploader.HttpException;
import com.limegroup.gnutella.uploader.authentication.HttpRequestFileListProvider;

public class AuthenticatingBrowseFriendListProvider implements HttpRequestFileListProvider {

    private final FileManager fileManager;
    private final MACCalculator calculator;

    @Inject
    public AuthenticatingBrowseFriendListProvider(FileManager fileManager, @Named("xmppMACCalculator") MACCalculator calculator) {
        this.fileManager = fileManager;
        this.calculator = calculator;
    }
    
    @Override
    public FileList getFileList(HttpRequest request, HttpContext httpContext) throws HttpException, IOException, org.apache.http.HttpException {
        String friendId = getFriend(request);
        String password = StringUtils.getUTF8String(calculator.getMACBytes(new FriendTokenData(friendId)));
        
        BasicHttpProcessor processor = new BasicHttpProcessor();
        processor.addRequestInterceptor(new BasicAuthenticationRequestInterceptor(friendId, password));
        processor.process(request, httpContext);

        FileList buddyFileList = fileManager.getBuddyFileList(friendId);
        if (buddyFileList == null) {
            throw new HttpException("no such list for: " + friendId, HttpStatus.SC_NOT_FOUND);
        }
        return buddyFileList;
    }
    
    String parseFriend(HttpRequest request) throws HttpException {
        RequestLine requestLine = request.getRequestLine();
        String uri;
        try {
            uri = URLDecoder.decode(requestLine.getUri(), "UTF-8");
            int lastSlash = uri.lastIndexOf('/');
            // check for trailing slash
            if (lastSlash == uri.length() - 1) {
                lastSlash = uri.lastIndexOf('/', uri.length() - 2);
                if (lastSlash != -1) {
                    return uri.substring(lastSlash + 1, uri.length() - 1);
                }
            }
            if (lastSlash != -1) {
                return uri.substring(lastSlash + 1);
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        throw new HttpException("no friend id:", HttpStatus.SC_BAD_REQUEST);
    }
    
    String getFriend(HttpRequest request) throws HttpException {
        String friendId = parseFriend(request);
        if (friendId.contains(":")) {
            throw new HttpException("invalid friend id:" + String.valueOf(friendId), HttpStatus.SC_BAD_REQUEST);
        }
        return friendId;
    }
    
    private static class FriendTokenData implements TokenData {
        
        private final String friendId;

        public FriendTokenData(String friendId) {
            this.friendId = friendId;
        }

        @Override
        public byte[] getData() {
            return StringUtils.toUTF8Bytes(friendId);
        }
        
    }
     
}
