package com.limegroup.mojito.handler.response;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.exceptions.DHTException;
import com.limegroup.mojito.handler.AbstractResponseHandler;
import com.limegroup.mojito.messages.FindNodeResponse;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;
import com.limegroup.mojito.util.ContactUtils;

/**
 * GetQueryKeyHandler tries to get the QueryKey of a Node
 */
class GetQueryKeyHandler extends AbstractResponseHandler<QueryKey> {
    
    private static final Log LOG = LogFactory.getLog(GetQueryKeyHandler.class);
    
    private Contact node;
    
    GetQueryKeyHandler(Context context, Contact node) {
        super(context);
        
        this.node = node;
    }

    @Override
    protected void start() throws Exception {
        RequestMessage request = context.getMessageHelper()
            .createFindNodeRequest(node.getContactAddress(), node.getNodeID());
        context.getMessageDispatcher().send(node, request, GetQueryKeyHandler.this);
    }

    @Override
    public void handleResponse(ResponseMessage response, long time) throws IOException {
        System.out.println("Response: " + response);
        super.handleResponse(response, time);
    }
    
    protected void response(ResponseMessage message, long time) throws IOException {
        
        FindNodeResponse response = (FindNodeResponse)message;
        
        Collection<Contact> nodes = response.getNodes();
        for(Contact node : nodes) {
            
            if (!ContactUtils.isValidContact(response.getContact(), node)) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Dropping " + node);
                }
                continue;
            }
            
            if (ContactUtils.isLocalContact(context, node, null)) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Dropping " + node);
                }
                continue;
            }
            
            // We did a FIND_NODE lookup use the info
            // to fill/update our routing table
            assert (node.isAlive() == false);
            context.getRouteTable().add(node);
        }
        
        setReturnValue(response.getQueryKey());
    }
    
    protected void timeout(KUID nodeId, SocketAddress dst, RequestMessage message, long time) throws IOException {
        fireTimeoutException(nodeId, dst, message, time);
    }

    public void error(KUID nodeId, SocketAddress dst, RequestMessage message, Exception e) {
        if (LOG.isErrorEnabled()) {
            LOG.error("Getting the QueryKey from " + ContactUtils.toString(nodeId, dst) + " failed", e);
        }
        
        setException(new DHTException(nodeId, dst, message, -1L, e));
    }
}