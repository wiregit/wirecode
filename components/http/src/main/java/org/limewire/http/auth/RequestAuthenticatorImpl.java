package org.limewire.http.auth;

import java.io.IOException;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.auth.Credentials;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.nio.protocol.NHttpRequestHandler;
import org.apache.http.nio.protocol.NHttpResponseTrigger;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.UriPatternMatcher;

import com.google.inject.Inject;
import com.google.inject.Singleton;


@Singleton
public class RequestAuthenticatorImpl implements RequestAuthenticator {
    final UserStore userStore;
    final UriPatternMatcher protectedURIs;
    
    @Inject
    public RequestAuthenticatorImpl(UserStore userStore) {
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
        
        ServerAuthState authState = new ServerAuthState();
        ServerAuthScheme authScheme = new BasicAuthScheme(userStore);
        authState.setScheme(authScheme);  // TODO other schemes, scheme registry, etc
        context.setAttribute(ServerAuthState.AUTH_STATE, authState);
        if(protectedURIs.lookup(request.getRequestLine().getUri()) != null) {
            Credentials credentials = authScheme.authenticate(request);
            if(credentials != null) {
                authState.setCredentials(credentials);
                authScheme.setComplete();
            }                
        } else {
            authScheme.setComplete();
        }
    }

    public void process(HttpResponse response, HttpContext context) throws HttpException, IOException {
        if (response == null) {
            throw new IllegalArgumentException("HTTP response may not be null");
        }
        if (context == null) {
            throw new IllegalArgumentException("HTTP context may not be null");
        }
        
        ServerAuthState authState = (ServerAuthState) context.getAttribute(ServerAuthState.AUTH_STATE);
        ServerAuthScheme authScheme = authState.getScheme();
        if (!authScheme.isComplete()) {
            response.addHeader(authScheme.createChallenge());    
        } 
    }
    
    public NHttpRequestHandler guardedHandler(String url, NHttpRequestHandler handler) {
        if(isProtected(handler)) {
            protectedURIs.register(url, url);
            return new GuardingHandler(handler);
        } else {
            return handler;
        }
    }
    
    private boolean isProtected(NHttpRequestHandler handler) {
        return handler.getClass().getAnnotation(Protected.class) != null;        
    }
    
    private class GuardingHandler implements NHttpRequestHandler {
        private final NHttpRequestHandler handler;

        public GuardingHandler(NHttpRequestHandler handler) {
            this.handler = handler;
        }

        public ConsumingNHttpEntity entityRequest(HttpEntityEnclosingRequest request, HttpContext context) throws HttpException, IOException {
            ServerAuthState authState = (ServerAuthState) context.getAttribute(ServerAuthState.AUTH_STATE);
            ServerAuthScheme authScheme = authState.getScheme();
            if(authScheme.isComplete()) {
                return handler.entityRequest(request, context);
            } else {
                return null;
            }
        }

        public void handle(HttpRequest request, HttpResponse response, NHttpResponseTrigger trigger, HttpContext context) throws HttpException, IOException {
            ServerAuthState authState = (ServerAuthState) context.getAttribute(ServerAuthState.AUTH_STATE);
            ServerAuthScheme authScheme = authState.getScheme();
            if(authScheme.isComplete()) {
                handler.handle(request, response, trigger, context);
            }
        }
    }
}
