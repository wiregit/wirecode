package com.limegroup.gnutella.auth;

import java.util.LinkedList;
import java.util.List;

import com.limegroup.gnutella.FileDetails;
import com.limegroup.gnutella.messages.Message;

public class StubContentAuthority implements ContentAuthority {

    private List<FileDetails> sent = new LinkedList<FileDetails>();

    public boolean initialize() {
        return false;
    }

    public List<FileDetails> getSent() {
        return sent;
    }

	public void sendAuthorizationRequest(FileDetails details, long timeout) {
		sent.add(details);
	}

	public void setContentResponseObserver(ContentResponseObserver observer) {
		// TODO Auto-generated method stub
		
	}

	public void shutdown() {
		// TODO Auto-generated method stub
		
	}

}
