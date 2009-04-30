package org.limewire.facebook.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.code.facebookapi.FacebookException;
import com.google.code.facebookapi.FacebookJsonRestClient;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class LocalSessionProvider implements SessionFactory {
    private final Provider<String> apiKey;
    private final Map<String, String> secrets = new ConcurrentHashMap<String, String>(); 

    @Inject
    LocalSessionProvider(@Named("facebookApiKey") Provider<String> apiKey){
        this.apiKey = apiKey;
    }
    
    @Override
    public String getSession(String authToken) {
        FacebookJsonRestClient client = new FacebookJsonRestClient(apiKey.get(), "");
        try {
            String session = client.auth_getSession(authToken, true);
            secrets.put(session, client.getSessionSecret());
            return session;
        } catch (FacebookException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getSecret(String session) {
        return secrets.get(session);
    }
}
