/*
 * Waits for an ACK regarding a promotion request.
 */
package com.limegroup.gnutella.upelection;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.MessageListener;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.vendor.*;


public class PromotionACKer implements MessageListener {
	
	/**
	 * true = we are candidate, start promotion
	 * false = we are requestor, just ack.
	 */
	private final boolean _promote;
	
	private String _host;
	private int _port;
	
	/**
	 * creates a promotion ACKer which replies with another LimeACK or starts
	 * a promotion.
	 * @param host the address of the candidate
	 * @param port the port
	 * @param promote whether to wait for an ack or start the promotion
	 */
	public PromotionACKer(String host, int port, boolean promote) {
		_promote=promote;
		_host=host;
		_port=port;
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.MessageListener#processMessage(com.limegroup.gnutella.messages.Message)
	 */
	public void processMessage(Message m) {
		//unregister ourselves
		RouterService.getMessageRouter().unregisterMessageListener(new GUID(m.getGUID()));
		
		//did we get the proper type of message?
		if (! (m instanceof LimeACKVendorMessage)) 
			return;
		
		LimeACKVendorMessage lavm = (LimeACKVendorMessage)m;
		
		//acks for this purpose request 0 results.
		if (lavm.getNumResults()!=0)
			return;
		
		
		//if we are just responding to an ack, create a new message
		//and send it.
		if (!_promote) {
			LimeACKVendorMessage reply = new LimeACKVendorMessage(new GUID(m.getGUID()), 0);
			UDPService.instance().send(reply, new Endpoint(_host,_port));
		} 
		else  { //start the promotion process in its own thread
			Promoter promoter = new Promoter(_host, _port);
			promoter.setDaemon(true);
			promoter.start();
		}
	}
}
