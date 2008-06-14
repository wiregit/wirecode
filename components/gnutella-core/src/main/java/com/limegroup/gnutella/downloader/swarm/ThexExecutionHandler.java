package com.limegroup.gnutella.downloader.swarm;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpStatus;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.nio.entity.ConsumingNHttpEntityTemplate;
import org.apache.http.protocol.HttpContext;
import org.limewire.swarm.file.FileCoordinator;
import org.limewire.swarm.http.handler.ExecutionHandler;
import org.limewire.swarm.http.listener.ResponseContentListener;

import com.limegroup.gnutella.downloader.swarm.HashTreeSwarmVerifier.TreeUpgradeResponse;
import com.limegroup.gnutella.tigertree.HashTree;
import com.limegroup.gnutella.tigertree.HashTreeFactory;

public class ThexExecutionHandler implements ExecutionHandler, HttpResponseInterceptor {

    private static final Log LOG = LogFactory.getLog(ThexExecutionHandler.class);

    private static final String THEX_URI = "swarm.http.thex.uri";
    private static final String ROOT32 = "swarm.http.thex.root32"; 
    private static final String RESPONSE_LISTENER = "swarm.http.thex.responseListener";
    private static final String THEX_TRIED = "swarm.http.thex.tried";

    private final HashTreeSwarmVerifier hashTreeSwarmVerifier;
    private final FileCoordinator fileCoordinator;
    private final HashTreeFactory hashTreeFactory;
    
    /** True if any source is currently requesting a tree. */
    private boolean requestingThex;

    /** The SHA1 to use. */
    private volatile String sha1;
    
    public ThexExecutionHandler(HashTreeSwarmVerifier hashTreeSwarmVerifier,
            FileCoordinator fileCoordinator, HashTreeFactory hashTreeFactory) {
        this.hashTreeSwarmVerifier = hashTreeSwarmVerifier;
        this.fileCoordinator = fileCoordinator;
        this.hashTreeFactory = hashTreeFactory;
    }
    
    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }
    
    public void process(HttpResponse response, HttpContext context) throws HttpException,
            IOException {
        Header header = response.getFirstHeader("X-Thex-URI");
        if(header != null && header.getValue() != null) {
            String[] values = header.getValue().split(";", 3);
            if(values.length == 1) {
                context.setAttribute(THEX_URI, values[0]);
            } else if(values.length >= 2) {
                context.setAttribute(THEX_URI, values[0]);
                context.setAttribute(ROOT32, values[1]);
            } else if(values.length == 0) {
                throw new IllegalStateException("WTF?");
            }
        }
    }
    
    public void finalizeContext(HttpContext context) {
        clearThex(context);
    }
    
    public void handleResponse(HttpResponse response, HttpContext context) throws IOException {
        if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            ThexContentListener listener = (ThexContentListener)context.getAttribute(RESPONSE_LISTENER);
            HashTree newTree = listener.getHashTree();
            TreeUpgradeResponse treeCode = hashTreeSwarmVerifier.setHashTree(newTree, 
                    fileCoordinator.getCompleteFileSize(), fileCoordinator.getAmountVerified());
            if(LOG.isDebugEnabled())
                LOG.debug("Received & set tree: " + newTree + ", with response: " + treeCode);
            switch(treeCode) {
            case NOT_ACCEPTED:
            case UPGRADE:
                break;
            case NEW_TREE:
                fileCoordinator.verify();
                break;
            case REVERIFY:
                fileCoordinator.reverify();
                break;
            default:
                throw new IllegalStateException("Invalid code: " + treeCode);
            }
        }
        clearThex(context);
    }
    
    private void clearThex(HttpContext context) {
        // This is valid because it will only be called if we were the ones submitting the request. 
        requestingThex = false;
        
        // Explicitly close content listener, if it was still set.
        ResponseContentListener contentListener = (ResponseContentListener)context.getAttribute(RESPONSE_LISTENER);
        if(contentListener != null) {
            contentListener.finished();
            context.setAttribute(RESPONSE_LISTENER, null);
        }
    }
    
    public ConsumingNHttpEntity responseEntity(HttpResponse response, HttpContext context)
            throws IOException {
        if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            ResponseContentListener listener =  (ResponseContentListener)context.getAttribute(RESPONSE_LISTENER);
            listener.initialize(response);
            return new ConsumingNHttpEntityTemplate(response.getEntity(), listener);
        } else {
            clearThex(context);
            return null;
        }
    }
    
    public HttpRequest submitRequest(HttpContext context) {
        // Only allow if:
        // a) No one else is currently requesting.
        // b) This host hasn't tried requesting at all.
        // c) We have a SHA1 to ultimately use (TODO: remove requirement)
        // d) We don't have a tree OR the tree we have could be better
        // e) We know how to request from this host
        
        if (requestingThex // a
         || context.getAttribute(THEX_TRIED) != null // b
         || sha1 == null // c
         || (hashTreeSwarmVerifier.getHashTree() != null // d
              && hashTreeSwarmVerifier.getHashTree().isDepthGoodEnough())) { 
            LOG.debug("Ignoring request (for reasons a, b, c, or d)");
            return null;
        }
        
        String thexUri = (String)context.getAttribute(THEX_URI);
        String root32 = (String)context.getAttribute(ROOT32);
        
        // e
        if(thexUri == null || root32 == null) {
            LOG.debug("Ignoring request (for reason e)");
            return null;
        }
        
        LOG.debug("Requesting thex!");
        
        requestingThex = true;
        context.setAttribute(THEX_TRIED, Boolean.TRUE);
        context.setAttribute(RESPONSE_LISTENER,
                             new ThexContentListener(sha1, fileCoordinator.getCompleteFileSize(), root32, hashTreeFactory));
        return new BasicHttpRequest("GET", thexUri);
    }
    
}
