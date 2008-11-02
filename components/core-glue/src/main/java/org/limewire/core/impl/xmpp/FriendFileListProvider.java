package org.limewire.core.impl.xmpp;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

import org.apache.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.apache.http.RequestLine;
import org.apache.http.auth.Credentials;
import org.apache.http.protocol.HttpContext;
import org.limewire.http.auth.ServerAuthState;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.library.SharedFileList;
import com.limegroup.gnutella.uploader.HttpException;
import com.limegroup.gnutella.uploader.authentication.HttpRequestFileListProvider;

/**
 * Returns the appropriate friend file list for friend parsing the http request
 * 
 * the request URL has to be of the form
 * 
 * <pre>../../friend-id[/]</pre>
 * 
 * The parsed friend id will be checked against the credentials in the {@link ServerAuthState}
 * of the {@link HttpContext}.
 */
@Singleton
public class FriendFileListProvider implements HttpRequestFileListProvider {

    private final FileManager fileManager;

    @Inject
    public FriendFileListProvider(FileManager fileManager) {
        this.fileManager = fileManager;
    }
    
    @Override
    public Iterable<SharedFileList> getFileLists(HttpRequest request, HttpContext httpContext) throws HttpException {
        ServerAuthState authState = (ServerAuthState)httpContext.getAttribute(ServerAuthState.AUTH_STATE);
        if(authState != null) {
            Credentials credentials = authState.getCredentials();
            if (credentials != null) {
                // authorized by checking if friend is asking for their own list of files
                if (!credentials.getUserPrincipal().getName().equals(getFriend(request))) {
                    throw new HttpException("not authorized", HttpStatus.SC_UNAUTHORIZED);
                }
                SharedFileList buddyFileList = fileManager.getFriendFileList(credentials.getUserPrincipal().getName());
                if (buddyFileList == null) {
                    throw new HttpException("no such list for: " + credentials.getUserPrincipal().getName(), HttpStatus.SC_NOT_FOUND);
                }
                return Collections.singletonList(buddyFileList);
            }
        }
        throw new HttpException("forbidden", HttpStatus.SC_FORBIDDEN);
    }
    
    /**
     * Parses out the last element of the request uri's path and returns it.
     * @throws HttpException if there was no such element
     */
    String getFriend(HttpRequest request) throws HttpException {
        RequestLine requestLine = request.getRequestLine();
        try {
            URI uri = new URI(requestLine.getUri());
            String path = uri.getPath();
            if (path == null) {
                throw new HttpException("no friend id:", HttpStatus.SC_BAD_REQUEST);
            }
            if (path.endsWith("/")) {
                int previousSlash = path.lastIndexOf('/', path.length() - 2);
                if (previousSlash != -1) {
                    return path.substring(previousSlash + 1, path.length() - 1);
                }
            } else {
                int lastSlash = path.lastIndexOf('/');
                if (lastSlash != -1) {
                    return path.substring(lastSlash + 1);
                }
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        throw new HttpException("no friend id:", HttpStatus.SC_BAD_REQUEST);
    }
     
}
