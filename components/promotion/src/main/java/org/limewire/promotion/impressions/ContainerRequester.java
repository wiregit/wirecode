package org.limewire.promotion.impressions;

import java.io.IOException;
import java.util.Set;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.util.Base64;

/**
 * Instances of this class make POST requests to the URL given for containers,
 * and also add the data given in the {@link UserQueryEvent}.
 */
abstract class ContainerRequester {

    /**
     * The main entry point. This will create a <code>POST</code> request to
     * <code>url</code> and include the proper information we want to store.
     */
    public void request(String url, long id, Set<UserQueryEvent> queries) {
        final PostMethod request = new PostMethod(url);
        request.addParameter("id", String.valueOf(id));
        int i = 0;
        for (UserQueryEvent e : queries) {
            UserQueryEventData data = new UserQueryEventData(e);
            String dataStr = new String(Base64.encode(data.getData()));
            request.addParameter("query_" + i, data.getQuery());
            request.addParameter("data_" + i, dataStr);
            i++;      
        }
        request.addRequestHeader("User-Agent", getUserAgent());
            try {
                handle(request);
            } catch (HttpException e) {
                error(e);
            } catch (IOException e) {
                error(e);
            } 
    }
    
    protected abstract void handle(PostMethod request) throws HttpException, IOException;
    
    protected abstract void error(Exception e);
    
    protected abstract String getUserAgent();
}
