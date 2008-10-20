package org.limewire.core.impl.xmpp;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.apache.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.apache.http.RequestLine;
import org.apache.http.auth.Credentials;
import org.apache.http.protocol.HttpContext;
import org.limewire.http.auth.ServerAuthState;

import com.google.inject.Inject;
import com.limegroup.gnutella.FileList;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.uploader.HttpException;
import com.limegroup.gnutella.uploader.authentication.HttpRequestFileListProvider;

public class FriendFileListProvider implements HttpRequestFileListProvider {

    private final FileManager fileManager;

    @Inject
    public FriendFileListProvider(FileManager fileManager) {
        this.fileManager = fileManager;
    }
    
    @Override
    public FileList getFileList(HttpRequest request, HttpContext httpContext) throws HttpException, IOException, org.apache.http.HttpException {
        ServerAuthState authState = (ServerAuthState)httpContext.getAttribute(ServerAuthState.AUTH_STATE);
        if(authState != null) {
            Credentials credentials = authState.getCredentials();
            if(credentials != null) {
                FileList buddyFileList = fileManager.getFriendFileList(credentials.getUserPrincipal().getName());
                if (buddyFileList == null) {
                    throw new HttpException("no such list for: " + credentials.getUserPrincipal().getName(), HttpStatus.SC_NOT_FOUND);
                }
                return buddyFileList;
            }
        }
        throw new HttpException("forbidden", HttpStatus.SC_FORBIDDEN);
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
     
}
