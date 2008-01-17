package com.limegroup.gnutella.lws.server;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.util.Base64;
import org.limewire.lws.server.LWSDispatcherSupport;

import com.limegroup.gnutella.settings.LWSSettings;
import com.limegroup.gnutella.util.Tagged;
import com.limegroup.gnutella.util.URLDecoder;

public final class LWSUtil {
    
    private LWSUtil() {}
    
    /**
     * Looks for an argument with <code>name</code> in <code>args</code> and
     * returns a tagged value, where {@link Tagged#isValid()} is
     * <code>true</code> is this value was found. The action is used to make
     * an error message, which is contained in {@link Tagged#getValue()} is not
     * found.
     * 
     * @param args
     * @param name
     * @param action
     * @return
     */
    public static Tagged<String> getArg(Map<String, String> args, String name, String action) {
        String res = args.get(name);
        if (res == null || res.equals("")) {
            //String detail = "Invalid '" + name + "' while " + action;
            return new Tagged<String>(LWSDispatcherSupport.ErrorCodes.MISSING_PARAMETER, false);
        }
        String result = res;
        try {
            result = URLDecoder.decode(res);
        } catch (IOException e) {
            // no the end of the world
        }
        return new Tagged<String>(result, true);
    }

    /**
     * Adds the authentication we need to a request since we are using HTTP authentication
     * during testing.
     * 
     * @param method the method to which the authentication is added
     */
    public static void addAuthentication(HttpMethod method) {
        String username = LWSSettings.LWS_AUTHENTICATION_USERNAME.getValue();
        String password = LWSSettings.LWS_AUTHENTICATION_PASSWORD.getValue();
        String unecrypted = username + ":" + password;
        String encrypted = new String(Base64.encode(unecrypted.getBytes()));
        method.addRequestHeader("Authorization", "Basic " + encrypted);
    }
}
