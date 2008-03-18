package org.limewire.promotion;

import java.io.IOException;
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
 * Instances of this class make POST requests to the URL given for containers,
 * and also add the data given in the {@link UserQueryEvent}.
 */
@Singleton
public abstract class AbstractPromotionBinderRequestor implements PromotionBinderRequestor {
    
    private final PromotionBinderFactory binderFactory;

    @Inject
    public AbstractPromotionBinderRequestor(PromotionBinderFactory binderFactory) {
        this.binderFactory = binderFactory;
    }
    
    protected abstract void error(Exception e);

    protected abstract String getUserAgent();
    
    protected abstract void makeRequest(PostMethod request, ByteArrayCallback callback) throws HttpException, IOException;    

    /**
     * The main entry point. This will create a <code>POST</code> request to
     * <code>url</code> and include the proper information we want to store.
     */
    public void request(String url, long id, Set<UserQueryEvent> queries,
            final PromotionBinderCallback callback) {
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
