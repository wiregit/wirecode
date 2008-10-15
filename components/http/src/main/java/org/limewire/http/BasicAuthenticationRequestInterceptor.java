package org.limewire.http;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.StringTokenizer;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AUTH;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.LangUtils;
import org.limewire.util.StringUtils;

public class BasicAuthenticationRequestInterceptor implements HttpRequestInterceptor {

    private final UsernamePasswordCredentials credentials;

    public BasicAuthenticationRequestInterceptor(String username, String password) {
        credentials = new UsernamePasswordCredentials(username, password);
        if (username.contains(":")) {
            throw new IllegalArgumentException("username must not contain ':', " + username);
        }
    }
    
    @Override
    public void process(HttpRequest request, HttpContext context)
            throws org.apache.http.HttpException, IOException {
        Header authHeader = request.getFirstHeader(AUTH.WWW_AUTH_RESP);
        if (authHeader == null) {
            throw new HttpException("no credentials");
        }
        UsernamePasswordCredentials receivedCredentials = parseCredentials(authHeader);
        if (!credentials.getUserPrincipal().equals(receivedCredentials.getUserPrincipal())) {
            throw new HttpException("invalid username");
        }
        if (!LangUtils.equals(credentials.getPassword(), receivedCredentials.getPassword())) {
            throw new HttpException("invalid password");
        }
    }
    
    UsernamePasswordCredentials parseCredentials(Header header) throws HttpException {
        String value = header.getValue();
        int lastSpace = value.lastIndexOf(' ');
        if (lastSpace == -1 || lastSpace == value.length() - 1) {
            throw new HttpException("invalid auth value: " + value);
        }
        byte[] bytes = Base64.decodeBase64(value.substring(lastSpace + 1).getBytes(Charset.forName(HTTP.DEFAULT_PROTOCOL_CHARSET)));
        String decoded = StringUtils.getUTF8String(bytes);
        StringTokenizer tokenizer = new StringTokenizer(decoded, ":");
        if (tokenizer.countTokens() != 2) {
            throw new HttpException("invalid username, password tuple"); 
        }
        return new UsernamePasswordCredentials(tokenizer.nextToken(), tokenizer.nextToken());
    }
}
