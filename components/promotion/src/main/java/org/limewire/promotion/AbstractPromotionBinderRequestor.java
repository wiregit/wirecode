package org.limewire.promotion;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.limewire.promotion.impressions.InputStreamCallback;
import org.limewire.promotion.impressions.UserQueryEvent;
import org.limewire.promotion.impressions.UserQueryEventData;

/**
 * Instances of this class make <code>POST</code> requests to the URL given
 * for containers, and also add the data given in the {@link UserQueryEvent}.
 * <p>
 * The {@link PromotionBinder} that is passed in is created by the injected
 * {@link PromotionBinderFactory}.
 */
public abstract class AbstractPromotionBinderRequestor implements PromotionBinderRequestor {

    private final PromotionBinderFactory binderFactory;

    public AbstractPromotionBinderRequestor(PromotionBinderFactory binderFactory) {
        this.binderFactory = binderFactory;
    }

    /**
     * Called when an {@link Exception} occurs.
     * 
     * @param e the {@link Exception} that occured.
     */
    protected abstract void error(Exception e);

    /**
     * Returns the <code>User-Agent</code> to send along with the
     * <code>POST</code> request.
     * 
     * @return the <code>User-Agent</code> to send along with the
     *         <code>POST</code> request.
     */
    protected abstract String getUserAgent();

    /**
     * Subclasses should alter the final URL in this method, such as adding
     * version information, etc.
     */
    protected abstract String alterUrl(String url);

    /**
     * Called once the {@link PostMethod} <code>request</code> is constructed.
     * The purpose of the callback is so we can pass a {@link PromotionBindder}
     * created from the {@link PromotionBinderFactory} from the passed in bytes
     * we recieve from the request.
     * 
     * @param request <code>POST</code> to send to a server
     * @param callback this callback will take the passed in bytes and construct
     *        a {@link PromotionBinder} to pass the
     *        {@link PromotionBinderCallback} in
     *        {@link #request(String, long, Set, PromotionBinderCallback)}
     * @throws HttpException thrown when a protocol error occurs
     * @throws IOException thrown when a protocol I/O occurs
     */
    protected abstract void makeRequest(HttpPost request, HttpParams params,
            InputStreamCallback callback) throws HttpException, IOException;

    /**
     * The main entry point. This will create a <code>POST</code> request to
     * <code>url</code> and include the proper information we want to store.
     */
    public void request(String url, long id, Set<? extends UserQueryEvent> queries,
            final PromotionBinderCallback callback) {
        //
        // This request takes the following parameters
        // - id: bucket ID for the bucket that will be returned
        // - for i=1..n
        // query_<i>: query for the ith UserQueryEvent
        // data_<i>: data for the ith UserQueryEvent
        //

        HttpPost tmp = null;
        try {
            tmp = new HttpPost(alterUrl(url));
        } catch (URISyntaxException e1) {
            throw new RuntimeException("URI Syntax exception: Could not parse '" + alterUrl(url)
                    + "'", e1);
        }
        final HttpPost request = tmp;
        HttpParams params = new BasicHttpParams();
        //
        // This is the id of the bucket we want
        //
        params.setParameter("id", String.valueOf(id));
        int i = 0;
        for (UserQueryEvent e : queries) {
            //
            // The query and data
            //
            UserQueryEventData data = new UserQueryEventData(e);
            String dataStr = new String(new Base64().encode(data.getData()));
            params.setParameter("query_" + i, data.getQuery());
            params.setParameter("data_" + i, dataStr);
            i++;
        }
        request.addHeader("User-Agent", getUserAgent());
        try {
            makeRequest(request, params, new InputStreamCallback() {
                public void process(InputStream in) {
                    callback.process(binderFactory.newBinder(in));
                }
            });
        } catch (HttpException e) {
            error(e);
        } catch (IOException e) {
            error(e);
        }
    }

}
