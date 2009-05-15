package org.limewire.facebook.service;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.RedirectHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.concurrent.ListeningFuture;
import org.limewire.concurrent.ThreadPoolListeningExecutor;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.client.FriendConnection;
import org.limewire.core.api.friend.client.FriendConnectionConfiguration;
import org.limewire.core.api.friend.client.FriendException;
import org.limewire.listener.EventBroadcaster;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;

import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;

@Singleton
public class FacebookFriendConnection implements FriendConnection {
    private final FriendConnectionConfiguration configuration;
    private final Provider<String> apiKey;
    private static final String DUMMY_SECRET = "__";
    private static final String FACEBOOK_LOGIN_GET_URL = "http://coelacanth:5555/getlogin/";
    private static final String FACEBOOK_LOGIN_POST_ACTION_URL = "https://login.facebook.com/login.php?";
    private static final String FACEBOOK_GET_SESSION_URL = "http://coelacanth:5555/getsession/";
    private String authToken;
    private String session;
    private HttpClient httpClient;
    private String uid;
    private String secret;
    private static final String USER_AGENT_HEADER = "User-Agent: Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.0.8) Gecko/2009032609 Firefox/3.0.8";
    private ThreadPoolListeningExecutor executorService;
    private final AtomicBoolean loggedIn = new AtomicBoolean(false);
    private final AtomicBoolean loggingIn = new AtomicBoolean(false);
    private final EventBroadcaster<XMPPConnectionEvent> connectionBroadcaster;
    private final Map<String, FacebookFriend> friends;

    @AssistedInject
    public FacebookFriendConnection(@Assisted FriendConnectionConfiguration configuration,
                                    @Named("facebookApiKey") Provider<String> apiKey,
                                    EventBroadcaster<XMPPConnectionEvent> connectionBroadcaster) {
        this.configuration = configuration;
        this.apiKey = apiKey;
        this.connectionBroadcaster = connectionBroadcaster;
        httpClient = new AuthTokenInterceptingHttpClient();
        this.friends = new TreeMap<String, FacebookFriend>(String.CASE_INSENSITIVE_ORDER);
        executorService = ExecutorsHelper.newSingleThreadExecutor(ExecutorsHelper.daemonThreadFactory(getClass().getSimpleName()));
    }

    @Override
    public ListeningFuture<Void> login() {
        return executorService.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                loginImpl();
                return null;
            }
        });
    }

    @Override
    public ListeningFuture<Void> logout() {
        loggedIn.set(false);
        return null;
    }

    @Override
    public boolean isLoggedIn() {
        return loggedIn.get();
    }

    @Override
    public boolean isLoggingIn() {
        return loggingIn.get();
    }

    synchronized void loginImpl() throws FriendException {
      try {
            connectionBroadcaster.broadcast(new XMPPConnectionEvent(this, XMPPConnectionEvent.Type.CONNECTING));
            loggingIn.set(true);
            loginToFacebook();
            requestSession();
            loggedIn.set(true);
            connectionBroadcaster.broadcast(new XMPPConnectionEvent(this, XMPPConnectionEvent.Type.CONNECTED));
        } catch (IOException e) {
            connectionBroadcaster.broadcast(new XMPPConnectionEvent(this, XMPPConnectionEvent.Type.CONNECT_FAILED, e));
            throw new FriendException(e);
        } catch (JSONException e) {
            connectionBroadcaster.broadcast(new XMPPConnectionEvent(this, XMPPConnectionEvent.Type.CONNECT_FAILED, e));
            throw new FriendException(e);
        } finally {
            loggingIn.set(false);
        }
    }

    private void loginToFacebook() throws IOException {        
        HttpGet loginGet = new HttpGet(FACEBOOK_LOGIN_GET_URL);
        loginGet.addHeader("User-Agent", USER_AGENT_HEADER);
        HttpResponse response = httpClient.execute(loginGet);
        HttpEntity entity = response.getEntity();

        if (entity != null) {
            entity.consumeContent();
        }
        
        HttpPost httpost = new HttpPost(FACEBOOK_LOGIN_POST_ACTION_URL + "version=1.0" + "&auth_token=" + authToken + 
                                        "&api_key=");

        httpost.addHeader("User-Agent", USER_AGENT_HEADER);
        List <NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("email", configuration.getUserInputLocalID()));
        nvps.add(new BasicNameValuePair("pass", configuration.getPassword()));

        httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));

        HttpResponse responsePost = httpClient.execute(httpost);
        entity = responsePost.getEntity();

        if (entity != null) {
            //System.out.println(EntityUtils.toString(entity));
            entity.consumeContent();
        }
        
//        List<Cookie> finalCookies = ((DefaultHttpClient) httpClient).getCookieStore().getCookies();
//        for (Cookie finalCooky : finalCookies) {
//            if (finalCooky.getName().equals("c_user")) {
//                uid = finalCooky.getValue();
//            }
//        }
    }

    private void requestSession() throws IOException, JSONException {
        HttpGet sessionRequest = new HttpGet(FACEBOOK_GET_SESSION_URL + authToken + "/");
        HttpResponse response = httpClient.execute(sessionRequest);
        parseSessionResponse(response);
    }

    private void parseSessionResponse(HttpResponse response) throws IOException, JSONException {
        String responseBody = EntityUtils.toString(response.getEntity());        
		if (responseBody.matches( "[\\{\\[].*[\\}\\]]")) {
            JSONObject json = null;
            if (responseBody.matches( "\\{.*\\}")) {
                json = new JSONObject(responseBody);
            } else {
                //json = new JSONArray(responseBody);
            }
            session = json.getString("session_key");
            secret = json.getString("secret");
            uid = json.getString("uid");
		}
    }

    public synchronized String httpGET(String url) throws IOException {
        System.out.println("GET " + url);
        HttpGet loginGet = new HttpGet(url);
        loginGet.addHeader("User-Agent", USER_AGENT_HEADER);
        loginGet.addHeader("Connection", "close");
        HttpResponse response = httpClient.execute(loginGet);
        HttpEntity entity = response.getEntity();
        
        String responseStr = null;
        if (entity != null) {
            responseStr = EntityUtils.toString(entity);
            entity.consumeContent();
        }
        return responseStr;    
    }
    
    public synchronized String httpPOST(String host, String urlPostfix, List <NameValuePair> nvps) throws IOException {
        System.out.println("GET " + host + urlPostfix);
        String responseStr = null;
        HttpPost httpost = new HttpPost(host + urlPostfix);
        httpost.addHeader("Connection", "close");
        httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));

        HttpResponse postResponse = httpClient.execute(httpost);
        HttpEntity entity = postResponse.getEntity();

        if (entity != null) {
            responseStr = EntityUtils.toString(entity);
            entity.consumeContent();
        }
        return responseStr;
    }

    @Override
    public FriendConnectionConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public ListeningFuture<Void> setMode(FriendPresence.Mode mode) {
        return null;
    }

    @Override
    public ListeningFuture<Void> addUser(String id, String name) {
        return null;
    }

    @Override
    public ListeningFuture<Void> removeUser(String id) {
        return null;
    }

    @Override
    public Friend getUser(String id) {
        return friends.get(id);
    }

    void userAvailable(String id, FacebookFriend friend) {
        this.friends.put(id, friend);
    }

    void userUnavailable(String id) {
        this.friends.remove(id);
    }

    @Override
    public Collection<Friend> getUsers() {
        synchronized (friends) {
            return new ArrayList<Friend>(friends.values());
        }
    }

    public String getAuthToken() {
        return authToken;
    }
    
    public String getSession() {
        return session;
    }
    
    public String getUID() {
        return uid;    
    }
    
    public String getSecret() {
        return secret;
    }
    
    private class AuthTokenInterceptingHttpClient extends DefaultHttpClient  {
        @Override
        protected RedirectHandler createRedirectHandler() {
            return new AuthTokenInterceptingRediectHandler(super.createRedirectHandler());
        }
    }
    
    private class AuthTokenInterceptingRediectHandler implements RedirectHandler {

        private final RedirectHandler parent;
        
        AuthTokenInterceptingRediectHandler(RedirectHandler parent) {
            this.parent = parent;
        }
        
        @Override
        public boolean isRedirectRequested(HttpResponse response, HttpContext context) {
            boolean isRedirect = parent.isRedirectRequested(response, context);
            Header [] locations = response.getHeaders("location");
            if(locations != null && locations.length > 0) {
                String location = locations[0].getValue();
                int authTokenIndex = location.indexOf("auth_token=");
                if(authTokenIndex > -1) {
                    // TODO concurrency
                    authToken = location.substring(authTokenIndex + "auth_token=".length());
                    int nextParamIndex = authToken.indexOf('&');
                    if(nextParamIndex > -1) {
                        authToken = authToken.substring(0, nextParamIndex);
                    }
                }
            }
            return isRedirect;
        }

        @Override
        public URI getLocationURI(HttpResponse response, HttpContext context) throws ProtocolException {
            return parent.getLocationURI(response, context);
        }
    }
}
