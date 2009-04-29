package org.limewire.facebook.service;

import com.google.code.facebookapi.FacebookJsonRestClient;
import com.google.code.facebookapi.FacebookException;
import com.google.inject.Inject;

public class LocalAuthTokenProvider implements AuthTokenFactory {

    @Inject
    LocalAuthTokenProvider(){
    }
    
    @Override
    public String getAuthToken(String apiKey) {
        FacebookJsonRestClient client = new FacebookJsonRestClient(apiKey, "");
        try {
            return client.auth_createToken();
        } catch (FacebookException e) {
            throw new RuntimeException(e);
        }
    }
}
