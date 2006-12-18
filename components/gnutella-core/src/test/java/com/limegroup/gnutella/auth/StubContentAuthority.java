package com.limegroup.gnutella.auth;

import java.util.LinkedList;
import java.util.List;

import com.limegroup.gnutella.FileDetails;

public class StubContentAuthority implements ContentAuthority {

    private List<FileDetails> sent = new LinkedList<FileDetails>();

    public void initialize() throws Exception {
    
    }

    public List<FileDetails> getSent() {
        return sent;
    }

	public void sendAuthorizationRequest(FileDetails details) {
		sent.add(details);
	}

	public void shutdown() {
		// TODO Auto-generated method stub
		
	}

	public long getTimeout() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void setContentResponseObserver(ContentAuhorityResponseObserver observer) {
		// TODO Auto-generated method stub
		
	}

}
