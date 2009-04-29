package org.limewire.facebook.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.cookie.Cookie;
import org.apache.http.util.EntityUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import com.google.code.facebookapi.FacebookException;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

public class FacebookFriendConnection {
    private final Provider<String> email;
    private final Provider<String> password;
    private final Provider<String> apiKey;
    private final AuthTokenFactory authTokenFactory;
    private final SessionFactory sessionFactory;
    private static final String DUMMY_SECRET = "__";
    private static final String FACEBOOK_LOGIN_URL = "https://www.facebook.com/login.php";
    private String authToken;
    private String session;
    private HttpClient httpClient;
    private String uid;
    private static final String USER_AGENT_HEADER = "User-Agent: Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.0.8) Gecko/2009032609 Firefox/3.0.8";

    @Inject
    public FacebookFriendConnection(@Named("facebookApiKey") Provider<String> apiKey,
                                    @Named("facebookEmail")Provider<String> email,
                                    @Named("facebookPassword") Provider<String> password,
                                    AuthTokenFactory authToken,
                                    SessionFactory sessionFactory) {
        this.email = email;
        this.password = password;
        this.apiKey = apiKey;
        this.authTokenFactory = authToken;
        this.sessionFactory = sessionFactory;
        httpClient = new DefaultHttpClient();
    }
    
    void loginImpl() throws FacebookException, IOException {
        authToken = authTokenFactory.getAuthToken(apiKey.get());
        loginToFacebook(authToken);
        session = sessionFactory.getSession(authToken);
    }

    private void loginToFacebook(String authToken) throws IOException {        
        HttpGet loginGet = new HttpGet(FACEBOOK_LOGIN_URL + "?v=1.0" + "&api_key=" + apiKey.get() + "&auth_token=" + authToken);
        loginGet.addHeader("User-Agent", USER_AGENT_HEADER);
        HttpResponse response = httpClient.execute(loginGet);
        HttpEntity entity = response.getEntity();

        if (entity != null) {
            entity.consumeContent();
        }

        HttpPost httpost = new HttpPost(FACEBOOK_LOGIN_URL + "?v=1.0" + "&api_key=" + apiKey.get() + "&auth_token=" + authToken);

        httpost.addHeader("User-Agent", USER_AGENT_HEADER);
        List <NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("email", email.get()));
        nvps.add(new BasicNameValuePair("pass", password.get()));

        httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));

        HttpResponse responsePost = httpClient.execute(httpost);
        entity = responsePost.getEntity();

        if (entity != null) {
            //System.out.println(EntityUtils.toString(entity));
            entity.consumeContent();
        }
        
        List<Cookie> finalCookies = ((DefaultHttpClient) httpClient).getCookieStore().getCookies();
        for (Cookie finalCooky : finalCookies) {
            if (finalCooky.getName().equals("c_user")) {
                uid = finalCooky.getValue();
            }
        }
    }
    
    public String httpGET(String url) throws IOException {
        HttpGet loginGet = new HttpGet(url);
        loginGet.addHeader("User-Agent", USER_AGENT_HEADER);
        HttpResponse response = httpClient.execute(loginGet);
        HttpEntity entity = response.getEntity();
        
        String responseStr = null;
        if (entity != null) {
            responseStr = EntityUtils.toString(entity);
            entity.consumeContent();
        }
        return responseStr;    
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
}
