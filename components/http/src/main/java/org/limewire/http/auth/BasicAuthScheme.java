package org.limewire.http.auth;

import java.util.StringTokenizer;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.auth.AUTH;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.message.BasicHeader;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class BasicAuthScheme implements ServerAuthScheme {
    
    private final UserStore userStore;
    private boolean complete;

    @Inject
    public BasicAuthScheme(UserStore userStore) {
        this.userStore = userStore;
    }

    public void setComplete() {
        complete = true;
    }

    public boolean isComplete() {
        return complete;
    }

    public Credentials authenticate(HttpRequest request) {
        Header authHeader = request.getFirstHeader(AUTH.WWW_AUTH_RESP);
        if(authHeader != null) {
            StringTokenizer st = new StringTokenizer(authHeader.getValue(), ":");
            if(st.hasMoreTokens()) {
                if(st.nextToken().trim().equalsIgnoreCase("Basic")) {
                    if(st.hasMoreTokens()) {
                        byte [] userNamePassword = Base64.decodeBase64(st.nextToken().trim().getBytes());
                        Credentials credentials = new UsernamePasswordCredentials(StringUtils.getUTF8String(userNamePassword));
                        if(userStore.authenticate(credentials)) {
                            return credentials;
                        }
                    }
                }
            } 
        }
        return null;
    }

    public Header createChallenge() {
        return new BasicHeader(AUTH.WWW_AUTH, "Basic");
    }
}
