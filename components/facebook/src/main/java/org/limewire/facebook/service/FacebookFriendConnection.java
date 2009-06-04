package org.limewire.facebook.service;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Random;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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
import org.limewire.core.api.friend.client.FriendConnection;
import org.limewire.core.api.friend.client.FriendConnectionConfiguration;
import org.limewire.core.api.friend.client.FriendConnectionEvent;
import org.limewire.core.api.friend.client.FriendException;
import org.limewire.core.api.friend.client.IncomingChatListener;
import org.limewire.core.api.friend.client.MessageWriter;
import org.limewire.core.api.friend.client.MessageReader;
import org.limewire.core.api.friend.client.ChatState;
import org.limewire.core.api.friend.feature.FeatureEvent;
import org.limewire.core.api.friend.feature.features.AuthToken;
import org.limewire.core.api.friend.feature.features.LibraryChangedNotifier;
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

import com.google.code.facebookapi.FacebookException;
import com.google.code.facebookapi.FacebookJsonRestClient;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;

@Singleton
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
    private volatile String logoutURL;
    private final ChatManager chatManager;

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
            if(friend.hasLimeWireAppInstalled() && friend.isSignedIn()) { // TODO check for LW feature
                Map<String, String> message = new HashMap<String, String>();
                message.put("from", getPresenceId());
                message.put("type", "unavailable");
                sendLiveMessage(Long.parseLong(friend.getId()), "presence", message);
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
                presenceListenerFuture = executorService.scheduleAtFixedRate(presenceListener, 0, 90, TimeUnit.SECONDS);
                loggedIn.set(true);
                connectionBroadcaster.broadcast(new FriendConnectionEvent(this, FriendConnectionEvent.Type.CONNECTED));
            } catch (IOException e) {
                connectionBroadcaster.broadcast(new FriendConnectionEvent(this, FriendConnectionEvent.Type.CONNECT_FAILED, e));
                throw new FriendException(e);
            } catch (JSONException e) {
                connectionBroadcaster.broadcast(new FriendConnectionEvent(this, FriendConnectionEvent.Type.CONNECT_FAILED, e));
                throw new FriendException(e);
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
        } catch (JSONException e) {
            LOG.debug("json error", e);
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
            throw new RuntimeException(e);
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
        entity = responsePost.getEntity();

        if (entity != null) {
            entity.consumeContent();
        }
    }

    private void requestSession() throws IOException, JSONException {
        LOG.debugf("requesting session from {0}...", FACEBOOK_GET_SESSION_URL + authToken + "/");
        HttpGet sessionRequest = new HttpGet(FACEBOOK_GET_SESSION_URL + authToken + "/");
        HttpClient httpClient = createHttpClient();
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
                LOG.debugf("body doesn't match inner regex: {0}", responseBody);
                //json = new JSONArray(responseBody);
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
    public ListeningFuture<Void> setMode(FriendPresence.Mode mode) {
        return null;
    }

    @Override
    public ListeningFuture<Void> addNewFriend(String id, String name) {
        return null;
    }

    @Override
    public ListeningFuture<Void> removeFriend(String id) {
        return null;
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
        return uid + "/" + configuration.getResource();
    }
    
    public String getSecret() {
        return secret;
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

    public void sendLiveMessage(FriendPresence presence, String type, Map<String, ?> messageMap) {
        sendLiveMessage(Long.parseLong(presence.getFriend().getId()), type, messageMap);
    }
    
    public void sendLiveMessage(final Long userId, final String type, Map<String, ?> messageMap) {
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

    void sendChatMessage(String userId, String message) throws FriendException {
        List <NameValuePair> nvps = new ArrayList <NameValuePair>();
        nvps.add(new BasicNameValuePair("msg_text", (message == null)? "":message));
        nvps.add(new BasicNameValuePair("msg_id", new Random().nextInt(999999999) + ""));
        nvps.add(new BasicNameValuePair("client_time", new Date().getTime() + ""));
        nvps.add(new BasicNameValuePair("to", userId));

        String post_form_id = postFormID.get();
        if(post_form_id != null) {
            nvps.add(new BasicNameValuePair("post_form_id", post_form_id));
        }
        try {
            String resp = httpPOST("http://www.facebook.com", "/ajax/chat/send.php", nvps);
        } catch (IOException e) {
            throw new FriendException(e);    
        }
    }

    void sendChatStateUpdate(String userId, ChatState state) throws FriendException {

        List <NameValuePair> nvps = new ArrayList <NameValuePair>();
        nvps.add(new BasicNameValuePair("typ", (state == ChatState.composing)? "1" : "0"));
        nvps.add(new BasicNameValuePair("to", userId));

        String post_form_id = postFormID.get();
        if(post_form_id != null) {
            nvps.add(new BasicNameValuePair("post_form_id", post_form_id));
        }
        try {
            httpPOST("http://www.facebook.com", "/ajax/chat/typ.php", nvps);
        } catch (IOException e) {
            throw new FriendException(e);
        }
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
                    boolean firstPresence = presences.size() == 0;
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
                }
            }
        }     
    }

    void removeAllPresences(FacebookFriend friend) {
        // non-LW presenes are NOT removed b/c that introduces race-conditions between
        // buddy-list polling and disco-info on-demand presence creation
        LOG.debugf("removing all non-limewire presences for {0}", friend.getId());
        synchronized (presenceLock) {
            Map<String, FriendPresence> presenceMap = friend.getPresences();
            for(FriendPresence presence : presenceMap.values()) {
                String resource = StringUtils.parseResource(presence.getPresenceId());
                if(resource.length() == 0) { // do no remove limewire presences; those are maintained by presence messages
                    LOG.debugf("removing presence {0}", presence.getPresenceId());
                    friend.removePresence((FacebookFriendPresence)presence);
                    friendPresenceBroadcaster.broadcast(new FriendPresenceEvent(presence, FriendPresenceEvent.Type.REMOVED));
                } else {
                    LOG.debugf("ignoring remove presence for {0}", presence.getPresenceId());
                }
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