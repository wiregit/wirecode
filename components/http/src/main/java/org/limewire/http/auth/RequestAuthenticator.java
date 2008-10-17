package org.limewire.http.auth;

import java.io.IOException;
import java.net.URI;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AUTH;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.UriPatternMatcher;


public class RequestAuthenticator implements HttpRequestInterceptor, ProtectedURLRegistry {
    final UserStore userStore;
    final UriPatternMatcher protectedURIs;

    public RequestAuthenticator(UserStore userStore) {
        this.userStore = userStore;
        protectedURIs = new UriPatternMatcher();
    }

    public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
        if (request == null) {
            throw new IllegalArgumentException("HTTP request may not be null");
        }
        if (context == null) {
            throw new IllegalArgumentException("HTTP context may not be null");
        }
        
        if(protectedURIs.lookup(request.getRequestLine().getUri()) != null) {
            if (!request.containsHeader(AUTH.WWW_AUTH_RESP)) {
                // TODO 401
            } else {
                ServerAuthState authState = (ServerAuthState) context.getAttribute(ServerAuthState.AUTH_STATE);
                if(authState != null) {
                    ServerAuthScheme authScheme = authState.getScheme();
                    if(authScheme != null) {
                        authState.setCredentials(authScheme.authenticate(request));    
                    } else {
                        // TODO 500
                    }
                } else {
                    // TODO 500
                }
            }
        }
    }

    public void addProtectedURL(URI url) {
        protectedURIs.register(url.toString(), url);
    }
}
