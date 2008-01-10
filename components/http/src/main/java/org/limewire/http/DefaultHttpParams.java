package org.limewire.http;

import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.client.params.ClientPNames;

public class DefaultHttpParams extends BasicHttpParams {
    
    /**
     * The amount of time to wait while trying to connect to a specified
     * host via TCP.  If we exceed this value, an IOException is thrown
     * while trying to connect.
     */
    private static final int CONNECTION_TIMEOUT = 5000;
    
    /**
     * The amount of time to wait while receiving data from a specified
     * host.  Used as an SO_TIMEOUT.
     */
    private static final int TIMEOUT = 8000;
    
    /**
     * The maximum number of times to allow redirects from hosts.
     */
    private static final int MAXIMUM_REDIRECTS = 10;
    
    public DefaultHttpParams() {
        HttpConnectionParams.setConnectionTimeout(this, CONNECTION_TIMEOUT);
        HttpConnectionParams.setSoTimeout(this, TIMEOUT);
        //HttpConnectionParams.setStaleCheckingEnabled(this, false);
        HttpClientParams.setRedirecting(this, true);
        setIntParameter(ClientPNames.MAX_REDIRECTS, MAXIMUM_REDIRECTS);    
    }
}
