package org.limewire.promotion;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.util.Base64;
import org.limewire.promotion.impressions.ByteArrayCallback;
import org.limewire.promotion.impressions.UserQueryEvent;
import org.limewire.promotion.impressions.UserQueryEventData;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Instances of this class make <code>POST</code> requests to the URL given
 * for containers, and also add the data given in the {@link UserQueryEvent}.
 * <p>
 * The {@link PromotionBinder} that is passed in is created by the injected
 * {@link PromotionBinderFactory}.
 */
@Singleton
public abstract class AbstractPromotionBinderRequestor implements PromotionBinderRequestor {

    private final PromotionBinderFactory binderFactory;

    @Inject
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
     *        {@ilnk #request(String, long, Set, PromotionBinderCallback)}
     * @throws HttpException thrown when a protocol error occurs
     * @throws IOException thrown when a protocol I/O occurs
     */
    protected abstract void makeRequest(PostMethod request, ByteArrayCallback callback)
            throws HttpException, IOException;

    /**
     * The main entry point. This will create a <code>POST</code> request to
     * <code>url</code> and include the proper information we want to store.
     */
    public void request(String url, long id, Set<UserQueryEvent> queries,
            final PromotionBinderCallback callback) {
        //
        // This request takes the following parameters
        //  - id: bucket ID for the bucket that will be returned
        //  - for i=1..n
        //      query_<i>: query for the ith UserQueryEvent
        //      data_<i>: data for the ith UserQueryEvent
        //
        final PostMethod request = new PostMethod(alterUrl(url));
        //
        //  This is the id of the bucket we want
        //
        request.addParameter("id", String.valueOf(id));
        int i = 0;
        for (UserQueryEvent e : queries) {
            //
            // The query and data
            //
            UserQueryEventData data = new UserQueryEventData(e);
            String dataStr = new String(Base64.encode(data.getData()));
            request.addParameter("query_" + i, data.getQuery());
            request.addParameter("data_" + i, dataStr);
            i++;
        }
        request.addRequestHeader("User-Agent", getUserAgent());
        try {
            makeRequest(request, new ByteArrayCallback() {
                public void process(byte[] bytes) {
                    callback.process(binderFactory.newBinder(bytes));
                }
            });
        } catch (HttpException e) {
            error(e);
        } catch (IOException e) {
            error(e);
        }
    }

}
