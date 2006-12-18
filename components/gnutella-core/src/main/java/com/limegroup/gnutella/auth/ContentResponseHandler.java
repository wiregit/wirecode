/**
 * 
 */
package com.limegroup.gnutella.auth;

import java.net.InetSocketAddress;

import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messagehandlers.MessageHandler;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.SecureMessage;
import com.limegroup.gnutella.messages.SecureMessageCallback;
import com.limegroup.gnutella.messages.vendor.ContentResponse;
import com.limegroup.gnutella.settings.ContentSettings;

class ContentResponseHandler implements MessageHandler {

	private ContentAuhorityResponseObserver observer;
	private ContentAuthority authority;
	
	public ContentResponseHandler(ContentAuthority authority, ContentAuhorityResponseObserver observer) {
		this.authority = authority;
		this.observer = observer;
		if (authority == null) {
			throw new NullPointerException("authority must not be null");
		}
		if (observer == null) {
			throw new NullPointerException("observer must not be null");
		}
	}

	public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
		final ContentResponse response = (ContentResponse)msg;
		if (ContentSettings.ONLY_SECURE_CONTENT_RESPONSES.getValue()) {
            if (response.hasSecureSignature()) {
                SecureMessageCallback callback = new SecureMessageCallback() {
                    public void handleSecureMessage(SecureMessage sm, boolean passed) {
                        if (passed) {
                        	handleContentResponse(response);
                        }
                    }
                };
                RouterService.getSecureMessageVerifier().verify(response, callback);
            }
        }
		else {
			handleContentResponse(response);
        }
	}
	
	private void handleContentResponse(ContentResponse response) {
		URN urn = response.getURN();
		if (urn != null) {
			observer.handleResponse(authority, urn, new ContentResponseData(response));
		}
	}
	
}