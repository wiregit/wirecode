package com.limegroup.gnutella;

import com.sun.java.util.collections.*;
import com.limegroup.gnutella.routing.*;

/**
 * This is the class that goes in the route table when a request is
 * sent whose reply is for me.
 */
public final class ForMeReplyHandler implements ReplyHandler {
	
	private final Set EMPTY_SET = 
		Collections.unmodifiableSet(new HashSet());
		

	public void handlePingReply(PingReply pingReply, ReplyHandler handler) {
        SettingsManager settings = SettingsManager.instance();
        //Kill incoming connections that don't share.  Note that we randomly
        //allow some freeloaders.  (Hopefully they'll get some stuff and then
        //share!)  Note that we only consider killing them on the first ping.
        //(Message 1 is their ping, message 2 is their reply to our ping.)
        if ((pingReply.getHops() <= 1)
			&& (handler.getNumMessagesReceived() <= 2)
			&& (!handler.isOutgoing())
			&& (handler.isKillable())
			&& (pingReply.getFiles() < settings.getFreeloaderFiles())
			&& ((int)(Math.random()*100.f) >
				settings.getFreeloaderAllowed())
			&& (handler instanceof ManagedConnection)) {
			ConnectionManager cm = RouterService.instance().getConnectionManager();
            cm.remove((ManagedConnection)handler);
        }
	}
	
	public void handleQueryReply(QueryReply reply, ReplyHandler handler) {
		if(handler.isPersonalSpam(reply)) return;
			
		RouterService rs = RouterService.instance();
		ActivityCallback callback = rs.getCallback();
		callback.handleQueryReply(reply);

		DownloadManager dm = rs.getDownloadManager();
		dm.handleQueryReply(reply);

        QueryUnicaster.instance().handleQueryReply(reply);
	}

	public void handlePushRequest(PushRequest pushRequest, ReplyHandler handler) {
        //Ignore push request from banned hosts.
        if (handler.isPersonalSpam(pushRequest))
            return;
		
        // Unpack the message
        byte[] ip = pushRequest.getIP();
        StringBuffer buf = new StringBuffer();
        buf.append(ByteOrder.ubyte2int(ip[0])+".");
        buf.append(ByteOrder.ubyte2int(ip[1])+".");
        buf.append(ByteOrder.ubyte2int(ip[2])+".");
        buf.append(ByteOrder.ubyte2int(ip[3])+"");
        String h = buf.toString();
        int port = pushRequest.getPort();
        int index = (int)pushRequest.getIndex();
        String req_guid_hexstring =
            (new GUID(pushRequest.getClientGUID())).toString();

		RouterService rs = RouterService.instance();
        FileDesc desc;
        try {
			FileManager fm = rs.getFileManager();
            desc = fm.get(index);
        }
        catch (IndexOutOfBoundsException e) {
            //You could connect and send 404 file
            //not found....but why bother?
            return;
        }

        String file = desc.getName();

		
        if (!rs.getAcceptor().isBannedIP(h)) {
			
            rs.getUploadManager().acceptPushUpload(file, h, port, 
												   index, req_guid_hexstring);
		}
	}
	
	public boolean isOpen() {
		//I'm always ready to handle replies.
		return true;
	}
	
	public int getNumMessagesReceived() {
		return 0;
	}
	
	
	public void countDroppedMessage() {}
	
	public boolean isSupernodeClientConnection() {
		return false;
	}
	
	public Set getDomains() {
		return EMPTY_SET;
	}
	
	public boolean isPersonalSpam(Message m) {
		return false;
	}
	
	public void updateHorizonStats(PingReply pingReply) {}
	
	public boolean isOutgoing() {
		return false;
	}
	
	public boolean isKillable() {
		return false;
	}
}



