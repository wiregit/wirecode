package org.limewire.facebook.service;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.RedirectHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.jivesoftware.smack.util.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.concurrent.ListeningFuture;
import org.limewire.concurrent.ScheduledListeningExecutorService;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.FriendPresenceEvent;
import org.limewire.core.api.friend.MutableFriendManager;
import org.limewire.core.api.friend.Network;
import org.limewire.core.api.friend.address.FriendAddress;
import org.limewire.core.api.friend.client.ChatState;
import org.limewire.core.api.friend.client.FriendConnection;
import org.limewire.core.api.friend.client.FriendConnectionConfiguration;
import org.limewire.core.api.friend.client.FriendConnectionEvent;
import org.limewire.core.api.friend.client.FriendException;
import org.limewire.core.api.friend.client.IncomingChatListener;
import org.limewire.core.api.friend.client.MessageReader;
import org.limewire.core.api.friend.client.MessageWriter;
import org.limewire.core.api.friend.feature.FeatureEvent;
import org.limewire.core.api.friend.feature.features.AuthToken;
import org.limewire.core.api.friend.feature.features.LibraryChangedNotifier;
import org.limewire.core.api.friend.feature.features.LimewireFeature;
import org.limewire.facebook.service.livemessage.AddressHandler;
import org.limewire.facebook.service.livemessage.AddressHandlerFactory;
import org.limewire.facebook.service.livemessage.AuthTokenHandler;
import org.limewire.facebook.service.livemessage.AuthTokenHandlerFactory;
import org.limewire.facebook.service.livemessage.ConnectBackRequestHandler;
import org.limewire.facebook.service.livemessage.ConnectBackRequestHandlerFactory;
import org.limewire.facebook.service.livemessage.LibraryRefreshHandler;
import org.limewire.facebook.service.livemessage.LibraryRefreshHandlerFactory;
import org.limewire.inject.MutableProvider;
import org.limewire.io.Address;
import org.limewire.listener.EventBroadcaster;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.ConnectBackRequest;
import org.limewire.security.SecurityUtils;

import com.google.code.facebookapi.FacebookException;
import com.google.code.facebookapi.FacebookJsonRestClient;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;

public class FacebookFriendConnection implements FriendConnection {
    
    private static final Log LOG = LogFactory.getLog(FacebookFriendConnection.class);
    
    private final FriendConnectionConfiguration configuration;
    private final Provider<String> apiKey;
    private static final String HOME_PAGE = "http://www.facebook.com/home.php";
    private static final String PRESENCE_POPOUT_PAGE = "http://www.facebook.com/presence/popout.php";
    private static final String FACEBOOK_LOGIN_GET_URL = "http://coelacanth:5555/getlogin/";
    private static final String FACEBOOK_LOGIN_POST_ACTION_URL = "https://login.facebook.com/login.php?";
    private static final String FACEBOOK_GET_SESSION_URL = "http://coelacanth:5555/getsession/";
    private static final String FACEBOOK_CHAT_SETTINGS_URL = "https://www.facebook.com/ajax/chat/settings.php?";
    private static final String INCORRECT_LOGIN_INDICATOR_TEXT = "Incorrect Email/Password Combination";
    private static final String INVALID_LOGIN_HTTP_RESPONSE = "Invalid HTTP login response";
    private String authToken;
    private String session;
    private String uid;
    private String secret;
    private static final String USER_AGENT_HEADER = "User-Agent: Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.0.10) Gecko/2009042316 Firefox/3.0.10";
    private final ChatListenerFactory chatListenerFactory;
    private ScheduledListeningExecutorService executorService;
    private final MutableProvider<String> chatChannel;
    private final AtomicBoolean loggedIn = new AtomicBoolean(false);
    private final AtomicBoolean loggingIn = new AtomicBoolean(false);
    private final EventBroadcaster<FriendConnectionEvent> connectionBroadcaster;
    private final Map<String, FacebookFriend> friends = Collections.synchronizedMap(new TreeMap<String, FacebookFriend>(String.CASE_INSENSITIVE_ORDER));
    private FacebookJsonRestClient facebookClient;
    private final CookieStore cookieStore = new BasicCookieStore();
    private AtomicReference<String> postFormID = new AtomicReference<String>();

    
    /**
     * Lock being held for adding and removing presences from friends.
     */
    private final Object presenceLock = new Object();
    
    private final EventBroadcaster<FriendPresenceEvent> friendPresenceBroadcaster;
    private AddressHandler addressHandler;
    private AuthTokenHandler authTokenHandler;
    private final LibraryRefreshHandler libraryRefreshHandler;

    private final EventBroadcaster<FeatureEvent> featureEventBroadcaster;
    

    /**
     * Adapt connection configuration to ensure the facebook user id is returned in
     * {@link Network#getCanonicalizedLocalID()}.
     */
    private final Network network = new Network() {
        @Override
        public String getCanonicalizedLocalID() {
            return getUID();
        }
        @Override
        public String getNetworkName() {
            return getConfiguration().getNetworkName();
        }
        @Override
        public Type getType() {
            return Type.FACEBOOK;
        }
    };

    private final MutableFriendManager friendManager;
    private final PresenceListenerFactory presenceListenerFactory;
    private final FacebookFriendFactory friendFactory;

    private final ConnectBackRequestHandler connectBackRequestHandler;
    private ChatListener chatListener;
    private ScheduledFuture presenceListenerFuture;
    private String logoutURL;
    private final ChatManager chatManager;

    /**
     * Session id as part of the full presence id. Follows the lifetime of
     * the connection object. A new connection object should have a new
     * session id.
     */
    private final String sessionId;
    
    @AssistedInject
    public FacebookFriendConnection(@Assisted FriendConnectionConfiguration configuration,
                                    @Named("facebookApiKey") Provider<String> apiKey,
                                    EventBroadcaster<FriendConnectionEvent> connectionBroadcaster,
                                    EventBroadcaster<FeatureEvent> featureEventBroadcaster,
                                    EventBroadcaster<FriendPresenceEvent> friendPresenceBroadcaster,
                                    MutableFriendManager friendManager,
                                    AddressHandlerFactory addressHandlerFactory,
                                    AuthTokenHandlerFactory authTokenHandlerFactory,
                                    ConnectBackRequestHandlerFactory connectBackRequestHandlerFactory,
                                    LibraryRefreshHandlerFactory libraryRefreshHandlerFactory,
                                    PresenceListenerFactory presenceListenerFactory,
                                    FacebookFriendFactory friendFactory,
                                    ChatListenerFactory chatListenerFactory,
                                    @Named("backgroundExecutor")ScheduledListeningExecutorService executorService,
                                    @ChatChannel MutableProvider<String> chatChannel) {
        this.configuration = configuration;
        this.apiKey = apiKey;
        this.connectionBroadcaster = connectionBroadcaster;
        this.featureEventBroadcaster = featureEventBroadcaster;
        this.friendPresenceBroadcaster = friendPresenceBroadcaster;
        this.friendManager = friendManager;
        this.presenceListenerFactory = presenceListenerFactory;
        this.friendFactory = friendFactory;
        this.chatListenerFactory = chatListenerFactory;
        this.executorService = executorService;
        this.chatChannel = chatChannel;
        this.addressHandler = addressHandlerFactory.create(this);
        this.authTokenHandler = authTokenHandlerFactory.create(this);
        this.connectBackRequestHandler = connectBackRequestHandlerFactory.create(this);
        this.libraryRefreshHandler = libraryRefreshHandlerFactory.create(this);
        this.chatManager = new ChatManager(this);
        this.sessionId = createSessionId();
    }
    
    private static String createSessionId() {
        byte[] sessionId = new byte[8];
        SecurityUtils.createSecureRandomNoBlock().nextBytes(sessionId);
        return org.limewire.util.StringUtils.getUTF8String(Base64.encodeBase64(sessionId));
    }
    
    void setPostFormID(String postFormID) {
        this.postFormID.set(postFormID);
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
        return executorService.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                logoutImpl();
                return null;
            }
        });
    }
    
    void logoutImpl() {
        synchronized (this) {
            LOG.debug("logging out from facebook...");
            loggedIn.set(false);
            try {
                sendOfflinePresences();
                synchronized (friends) {
                    for (FacebookFriend friend : friends.values()) {
                        removeAllPresences(friend);
                    }
                    friends.clear();
                }
                logoutFromFacebook();
                expireSession();
                if(chatListener != null) {
                    chatListener.setDone();
                }
                if(presenceListenerFuture != null) {
                    presenceListenerFuture.cancel(false);
                }
                LOG.debug("logged out from facebook.");
            } catch (IOException e) {
                LOG.debug("logout failed", e);
            } catch (FacebookException e) {
                LOG.debug("logout failed", e);
            }
            connectionBroadcaster.broadcast(new FriendConnectionEvent(this, FriendConnectionEvent.Type.DISCONNECTED));  
        }
    }
    
    private void sendOfflinePresences() {
        for(FacebookFriend friend : friends.values()) {
            for (FriendPresence presence : friend.getPresences().values()) {
                if (presence.hasFeatures(LimewireFeature.ID)) {
                    Map<String, Object> message = new HashMap<String, Object>();
                    message.put("type", "unavailable");
                    sendLiveMessage(presence, "presence", message);
                }
            }
        }
    }

    private void expireSession() throws FacebookException {
        synchronized (this) {
            if(facebookClient != null) {
                facebookClient.auth_expireSession();
            }
        }
    }

    private void logoutFromFacebook() throws IOException {
        HttpGet httpGet = new HttpGet(logoutURL);
        httpGet.addHeader("User-Agent", USER_AGENT_HEADER);
        
        HttpClient httpClient = createHttpClient();
        HttpResponse response = httpClient.execute(httpGet);
        HttpEntity entity = response.getEntity();

        if (entity != null) {
            entity.consumeContent();
        }
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
        synchronized (this) {
            try {
                connectionBroadcaster.broadcast(new FriendConnectionEvent(this, FriendConnectionEvent.Type.CONNECTING));
                loggingIn.set(true);
                loginToFacebook();
                requestSession();
                fetchAllFriends();
                readMetadataFromHomePage();
                chatListener = chatListenerFactory.createChatListener(this);
                ThreadExecutor.startThread(chatListener, "chat-listener-thread");
                setVisible();
                PresenceListener presenceListener = presenceListenerFactory.createPresenceListener(this);
                presenceListenerFuture = executorService.scheduleAtFixedRate(presenceListener, 0, 60, TimeUnit.SECONDS);
                loggedIn.set(true);
                connectionBroadcaster.broadcast(new FriendConnectionEvent(this, FriendConnectionEvent.Type.CONNECTED));
            } catch (IOException e) {
                LOG.debug("login error", e);
                connectionBroadcaster.broadcast(new FriendConnectionEvent(this, FriendConnectionEvent.Type.CONNECT_FAILED, e));
                throw new FriendException(e);
            } catch (JSONException e) {
                LOG.debug("login error", e);
                connectionBroadcaster.broadcast(new FriendConnectionEvent(this, FriendConnectionEvent.Type.CONNECT_FAILED, e));
                throw new FriendException(e);
            } catch (RuntimeException e) {
                LOG.debug("unexpected login error; probable bug", e);
                connectionBroadcaster.broadcast(new FriendConnectionEvent(this, FriendConnectionEvent.Type.CONNECT_FAILED, e));
                throw e;
            } finally {
                loggingIn.set(false);
            }
        }
    }

    private void setVisible() throws IOException {
        HttpPost httpPost = new HttpPost(FACEBOOK_CHAT_SETTINGS_URL);
        httpPost.addHeader("User-Agent", USER_AGENT_HEADER);
        
        List <NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("visibility", "true"));
        String post_form_id = postFormID.get();
        if(post_form_id != null) {
            nvps.add(new BasicNameValuePair("post_form_id", post_form_id));
        }
        
        httpPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
        
        HttpClient httpClient = createHttpClient();
        HttpResponse response = httpClient.execute(httpPost);
        HttpEntity entity = response.getEntity();

        if (entity != null) {
            entity.consumeContent();
        }
    }

    /**
     * Fetches all friends and adds them as known friends.
     */
    private void fetchAllFriends() {
        try {
            JSONArray friends = facebookClient.friends_get();

            // friends is null when i have no friends
            if (friends == null) {
                return;
            }
            List<Long> friendIds = new ArrayList<Long>(friends.length());
            for (int i = 0; i < friends.length(); i++) {
                friendIds.add(friends.getLong(i));
            }
            JSONArray users = (JSONArray) facebookClient.users_getInfo(friendIds, new HashSet<CharSequence>(Arrays.asList("uid", "first_name", "name", "status")));
            Set<String> limeWireFriends = fetchLimeWireFriends();
            LOG.debugf("all friends: {0}", users);
            for (int i = 0; i < users.length(); i++) {
                JSONObject user = users.getJSONObject(i);
                String id = user.getString("uid");
                FacebookFriend friend = friendFactory.create(id, user,
                        getNetwork(), limeWireFriends.contains(id), this);
                LOG.debugf("adding {0}", friend);
                addKnownFriend(friend);
            }
        } catch (FacebookException e) {
            LOG.debug("friend error", e);
            throw new RuntimeException("FIX ME!", e);
        } catch (JSONException e) {
            LOG.debug("json error", e);
            throw new RuntimeException("FIX ME!", e);
        }
    }
    
    /**
     * Fetches friend ids that have the LimeWire application installed
     * and marks the existing friends as LimeWire capable.
     */
    private Set<String> fetchLimeWireFriends() throws FacebookException {
        JSONArray limeWireFriendIds;
        try {
            Set<String> limeWireIds = new HashSet<String>();
            Object friends = facebookClient.friends_getAppUsers();
            if(friends instanceof JSONArray) { // is JSONObject when user has no friends with LW installed
                limeWireFriendIds = (JSONArray)friends;
                LOG.debugf("limewire friends: {0}", limeWireFriendIds);
                for (int i = 0; i < limeWireFriendIds.length(); i++) {
                    limeWireIds.add(limeWireFriendIds.getString(i));
                }                
            }
            return limeWireIds;    
        } catch (JSONException e) {
            throw new RuntimeException("FIX ME!",e);
        }
    }

    private void loginToFacebook() throws IOException {
        LOG.debugf("getting facebook login URL from {0}...", FACEBOOK_LOGIN_GET_URL);
        HttpGet loginGet = new HttpGet(FACEBOOK_LOGIN_GET_URL);
        loginGet.addHeader("User-Agent", USER_AGENT_HEADER);
        HttpClient httpClient = createHttpClient();
        HttpResponse response = httpClient.execute(loginGet);
        HttpEntity entity = response.getEntity();

        if (entity != null) {
            entity.consumeContent();
        }

        final String loginURL = FACEBOOK_LOGIN_POST_ACTION_URL + "version=1.0" + "&auth_token=" + authToken +
                "&api_key=" + apiKey.get();
        LOG.debugf("logging into {0}...", loginURL);
        HttpPost httpost = new HttpPost(loginURL);

        httpost.addHeader("User-Agent", USER_AGENT_HEADER);
        List <NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("email", configuration.getUserInputLocalID()));
        nvps.add(new BasicNameValuePair("pass", configuration.getPassword()));
        nvps.add(new BasicNameValuePair("persistent", "1"));
        nvps.add(new BasicNameValuePair("login", "Login"));
        nvps.add(new BasicNameValuePair("visibility", "true"));
        nvps.add(new BasicNameValuePair("charset_test", "€,´,€,´,水,Д,Є"));

        httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));

        HttpResponse responsePost = httpClient.execute(httpost);
        validateLoginResponse(responsePost);
    }

    private void requestSession() throws IOException, JSONException {
        LOG.debugf("requesting session from {0}...", FACEBOOK_GET_SESSION_URL + authToken + "/");
        HttpGet sessionRequest = new HttpGet(FACEBOOK_GET_SESSION_URL + authToken + "/");
        HttpClient httpClient = createHttpClient();
        HttpResponse response = httpClient.execute(sessionRequest);
        parseSessionResponse(response);        
    }

    private void validateLoginResponse(HttpResponse responsePost) throws IOException {
        // validate http response code
        if (responsePost.getStatusLine().getStatusCode() != 200) {
            throw new IOException(INVALID_LOGIN_HTTP_RESPONSE);
        }
        HttpEntity entity = responsePost.getEntity();

        if (entity != null) {
            String response = EntityUtils.toString(entity);
            if (response.contains(INCORRECT_LOGIN_INDICATOR_TEXT)) {
                throw new IOException("authentication failure due to invalid username and/or password.");
            }
            entity.consumeContent();
        }
    }

    private void parseSessionResponse(HttpResponse response) throws IOException, JSONException {
        String responseBody = EntityUtils.toString(response.getEntity());        
		if (responseBody.matches( "[\\{\\[].*[\\}\\]]")) {
            JSONObject json = null;
            if (responseBody.matches( "\\{.*\\}")) {
                json = new JSONObject(responseBody);
            } else {
                LOG.debugf("body doesn't match inner regex: {0}", responseBody);
                throw new IOException(INVALID_LOGIN_HTTP_RESPONSE);
            }
            session = json.getString("session_key");
            secret = json.getString("secret");
            uid = json.getString("uid");
            LOG.debugf("received session {0}, secret {1}, uid: {2}", session, secret, uid);
            facebookClient = new FacebookJsonRestClient(apiKey.get(), secret, session);
            if(LOG.isDebugEnabled()) {
                for(Cookie cookie : cookieStore.getCookies()) {
                    LOG.debugf(cookie.getName() + " = " + cookie.getValue());
                }
            }
		} else {
		    LOG.debugf("body doesn't match regex: {0}", responseBody);
            throw new IOException(INVALID_LOGIN_HTTP_RESPONSE);
		}
    }
    
    public void readMetadataFromHomePage() throws FriendException {
        try {
            String homePage = httpGET(HOME_PAGE);

            if(homePage == null){
                throw new IOException("no response");
            }
            if(uid == null){
                throw new IOException("no uid");
            }

            readLogoutURL(homePage);
            
            String presencePopoutPage = httpGET(PRESENCE_POPOUT_PAGE);
            readChannel(presencePopoutPage);
            readPOSTFormID(presencePopoutPage);
            
        } catch (IOException ioe)  {
            LOG.debug("starting chat failed", ioe);
            throw new FriendException(ioe);
        }
    }

    private void readLogoutURL(String homePage) throws IOException {
        String logoutURLPrefix = "<a href=\"http://www.facebook.com/logout.php?";
        int logoutURLBeginPos = homePage.indexOf(logoutURLPrefix);
        int logoutURLEndPos = homePage.indexOf("\">", logoutURLBeginPos);
        if (logoutURLBeginPos < 0){
            LOG.debugf("logout url not in homepage: {0}", homePage);
            throw new IOException("can't find logout URL");
        }
        else {
            logoutURL = homePage.substring(logoutURLBeginPos + "<a href=\"".length(),
                    logoutURLEndPos);
        }
    }

    private void readPOSTFormID(String homePage) throws IOException {
        String post_form_id;
        String postFormIDPrefix = "<input type=\"hidden\" id=\"post_form_id\" name=\"post_form_id\" value=\"";
        int formIdBeginPos = homePage.indexOf(postFormIDPrefix)
                + postFormIDPrefix.length();
        if (formIdBeginPos < postFormIDPrefix.length()){
            throw new IOException("can't find post form id");
        }
        else {
            post_form_id = homePage.substring(formIdBeginPos,
                    formIdBeginPos + 32);
        }

        setPostFormID(post_form_id);
    }

    private void readChannel(String homePage) throws IOException {
        String channel;
        String channelPrefix = " \"channel";
        int channelBeginPos = homePage.indexOf(channelPrefix)
                + channelPrefix.length();
        if (channelBeginPos < channelPrefix.length()){
            channel = chatChannel.get();
            // no cached value
            if (channel.length() == 0) {
                throw new IOException("can't find channel");
            }
            LOG.debugf("using cached channel: {0}", channel);
        }
        else {
            channel = homePage.substring(channelBeginPos,
                    channelBeginPos + 2);
            chatChannel.set(channel);
        }
    }

    public String httpGET(String url) throws IOException {
        LOG.debugf("facebook GET: {0}", url);
        HttpGet loginGet = new HttpGet(url);
        loginGet.addHeader("User-Agent", USER_AGENT_HEADER);
        loginGet.addHeader("Connection", "close");
        HttpClient httpClient = createHttpClient();
        HttpResponse response = httpClient.execute(loginGet);
        HttpEntity entity = response.getEntity();
        
        String responseStr = null;
        if (entity != null) {
            responseStr = EntityUtils.toString(entity);
            entity.consumeContent();
        }
        return responseStr;    
    }
    
    /**
     * 
     * @return null if there is no response data
     */
    public String httpPOST(String host, String urlPostfix, List <NameValuePair> nvps) throws IOException {
        LOG.debugf("facebook POST: {0} {1}", host, urlPostfix);
        HttpPost httpost = new HttpPost(host + urlPostfix);
        httpost.addHeader("Connection", "close");
        httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));

        HttpClient httpClient = createHttpClient();
        HttpResponse postResponse = httpClient.execute(httpost);
        HttpEntity entity = postResponse.getEntity();

        if (entity != null) {
            String responseStr = EntityUtils.toString(entity);
            entity.consumeContent();
            return responseStr;
        }
        return null;
    }

    @Override
    public FriendConnectionConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public boolean supportsMode() {
        return false;
    }

    @Override
    public ListeningFuture<Void> setMode(FriendPresence.Mode mode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean supportsAddRemoveFriend() {
        return false;
    }

    @Override
    public ListeningFuture<Void> addNewFriend(String id, String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListeningFuture<Void> removeFriend(String id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FacebookFriend getFriend(String id) {
        return friends.get(id);
    }

    @Override
    public Collection<FacebookFriend> getFriends() {
        synchronized (friends) {
            return new ArrayList<FacebookFriend>(friends.values());
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
    
    public String getChannel() {
        return chatChannel.get();
    }

    public String getPresenceId() {
        return uid + "/" + configuration.getResource() + sessionId;
    }
    
    ChatManager getChatManager() {
        return chatManager;
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

    public void sendLiveMessage(FriendPresence presence, String type, Map<String, Object> messageMap) {
        messageMap.put("to", presence.getPresenceId());
        sendLiveMessage(Long.parseLong(presence.getFriend().getId()), type, messageMap);
    }
    
    private void sendLiveMessage(final Long userId, final String type, Map<String, Object> messageMap) {
        messageMap.put("from", getPresenceId());
        final JSONObject message = new JSONObject(messageMap);
        executorService.submit(new Runnable() {
            public void run() {
                synchronized (this) {
                    try {
                        LOG.debugf("live message {0} to {1} : {2}", type, userId, message);
                        facebookClient.liveMessage_send(userId, type, message);
                    }
                    catch (FacebookException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
    }

    void sendChatMessage(String friendId, String message) throws FriendException {
        List <NameValuePair> nvps = new ArrayList <NameValuePair>();
        nvps.add(new BasicNameValuePair("msg_text", (message == null)? "":message));
        nvps.add(new BasicNameValuePair("msg_id", (int)(Math.random() * 1000000000) + ""));
        nvps.add(new BasicNameValuePair("client_time", System.currentTimeMillis() + ""));
        nvps.add(new BasicNameValuePair("to", friendId));

        String post_form_id = postFormID.get();
        if(post_form_id != null) {
            nvps.add(new BasicNameValuePair("post_form_id", post_form_id));
        }
        try {
            String resp = httpPOST("http://www.facebook.com", "/ajax/chat/send.php", nvps);
            handleChatResponseError(friendId, resp);
        } catch (IOException e) {
            throw new FriendException(e);    
        }
    }

    /**
     * Sends a chat state update to a friend.
     * <p>
     * Side effect: If the friend is offline, all presences of the friend are 
     * removed and he's no longer available.
     * 
     * @return true if the friend is online, false otherwise
     */
    boolean sendChatStateUpdate(String friendId, ChatState state) throws FriendException {
        List <NameValuePair> nvps = new ArrayList <NameValuePair>();
        nvps.add(new BasicNameValuePair("typ", (state == ChatState.composing)? "1" : "0"));
        nvps.add(new BasicNameValuePair("to", friendId));

        String post_form_id = postFormID.get();
        if(post_form_id != null) {
            nvps.add(new BasicNameValuePair("post_form_id", post_form_id));
        }
        try {
            String resp = httpPOST("http://www.facebook.com", "/ajax/chat/typ.php", nvps);
            return handleChatResponseError(friendId, resp);
        } catch (IOException e) {
            LOG.debug("error sending chat update", e);
            throw new FriendException(e);
        }
    }
    
    /**
     * Sends a chat state update which causes a friend to be removed from available
     * friends if he's no longer online. This is a blocking call.
     * 
     * @return false if the friend is offline, true otherwise
     */
    public boolean sendFriendIsOnline(String friendId) throws FriendException {
        return sendChatStateUpdate(friendId, ChatState.active);
    }
    
    /**
     * @return false if the friend is offline, otherwise true, also true in other
     * error cases
     */
    private boolean handleChatResponseError(String friendId, String response) {
        String prefix = "for (;;);";
        if (response.startsWith(prefix)) {
            response = response.substring(prefix.length());
        }
        try {
            JSONObject json = new JSONObject(response);
            int error = json.getInt("error");
            if (error == 1356003) {
                LOG.debugf("friend offline: {0}, full response: {1}", friendId, response);
                FacebookFriend friend = getFriend(friendId);
                if (friend != null) {
                    removeAllPresences(friend);
                } else {
                    LOG.debug("friend already removed");
                }
                return false;
            } else if (error != 0) {
                LOG.debugf("unhandled error: {0}", response);
            }
        } catch (JSONException e) {
            LOG.debugf(e, "error parsing chat response {0}", response);
        }
        return true;
    }
    
    public Network getNetwork() {
        return network;
    }
    
    private HttpClient createHttpClient() {
        AuthTokenInterceptingHttpClient httpClient = new AuthTokenInterceptingHttpClient();
        httpClient.setCookieStore(cookieStore);
        return httpClient;
    }
    
    
    /**
     * Adds a friend to connection and friend manager. 
     */
    void addKnownFriend(FacebookFriend friend) {
        friends.put(friend.getId(), friend);
        friendManager.addKnownFriend(friend);
    }
    
    /**
     * Creates a <code>presence</code> for a <code>friend</code> if the friend doesn't have 
     * a presence yet. If that's the case also notifies friend manager that the
     * friend is available now.
     * 
     * @return the old or new presence
     */
    public void addPresence(String presenceId) {
        FacebookFriendPresence newPresence = null;
        synchronized (presenceLock) {
            String friendId = StringUtils.parseBareAddress(presenceId);
            FacebookFriend facebookFriend = getFriend(friendId);
            if(facebookFriend != null) {
                Map<String, FriendPresence> presences = facebookFriend.getPresences();
                if(!presences.containsKey(presenceId)) {
                    // remove old presence with same resource prefix
                    String newResourcePrefix = FriendAddress.parseIdPrefix(presenceId);
                    for (String id : presences.keySet()) {
                        String fullResource = StringUtils.parseResource(id);
                        if (fullResource.startsWith(newResourcePrefix)) {
                            LOG.debugf("found old presence to replace: {0}, new id: {1}", id, presenceId);
                            removePresence(id);
                        }
                    }
                    boolean firstPresence = facebookFriend.getPresences().isEmpty();
                    LOG.debugf("new friend is available: {0}", presenceId);
                    newPresence = new FacebookFriendPresence(presenceId, facebookFriend, featureEventBroadcaster);
                    if (facebookFriend.hasLimeWireAppInstalled()) {
                        addTransports(newPresence);
                    }
                    facebookFriend.addPresence(newPresence); 
                    if(firstPresence) {
                        friendManager.addAvailableFriend(facebookFriend);    
                    }                    
                    friendPresenceBroadcaster.broadcast(new FriendPresenceEvent(newPresence, FriendPresenceEvent.Type.ADDED));
                }
            }
        }                
    } 
    
    private void addTransports(FacebookFriendPresence presence) {
        presence.addTransport(Address.class, addressHandler);
        presence.addTransport(AuthToken.class, authTokenHandler);
        presence.addTransport(ConnectBackRequest.class, connectBackRequestHandler);
        presence.addTransport(LibraryChangedNotifier.class, libraryRefreshHandler);
    }
    
    public void removePresence(String presenceId) {
        synchronized (presenceLock) {
            String friendId = StringUtils.parseBareAddress(presenceId);
            FacebookFriend facebookFriend = getFriend(friendId);
            if(facebookFriend != null) {
                FriendPresence presence = facebookFriend.getPresences().get(presenceId);
                if(presence != null) {
                    LOG.debugf("removing presence {0}", presence.getPresenceId());
                    FacebookFriendPresence facebookFriendPresence = (FacebookFriendPresence)presence;
                    facebookFriend.removePresence(facebookFriendPresence);
                    friendPresenceBroadcaster.broadcast(new FriendPresenceEvent(facebookFriendPresence, FriendPresenceEvent.Type.REMOVED));
                    if(!facebookFriend.isSignedIn()) {
                        friendManager.removeAvailableFriend(facebookFriend);
                    }
                } else {
                    LOG.debugf("remove presence, no presence to remove: {0}", presenceId);
                }
            } else {
                LOG.debugf("remove presence, no friend found for id {0}", presenceId);
            }
        }     
    }

    void removeAllPresences(FacebookFriend friend) {
        synchronized (presenceLock) {
            Map<String, FriendPresence> presenceMap = friend.getPresences();
            LOG.debugf("removing all presences for {0}", friend.getId());
            for(FriendPresence presence : presenceMap.values()) {
                LOG.debugf("removing presence {0}", presence);
                friend.removePresence((FacebookFriendPresence)presence);
                friendPresenceBroadcaster.broadcast(new FriendPresenceEvent(presence, FriendPresenceEvent.Type.REMOVED));
            }
            friendManager.removeAvailableFriend(friend);
        }
    }


    void setIncomingChatListener(String friendId, IncomingChatListener listener) {
        chatManager.setIncomingChatListener(friendId, listener);

    }

    void removeIncomingChatListener(String friendId) {
        chatManager.removeChat(friendId);
    }

    MessageWriter createChat(String friendId, MessageReader reader) {
        return chatManager.addMessageReader(friendId, reader);
    }
}