package com.limegroup.gnutella.auth;

import com.limegroup.gnutella.URN;

public interface ContentAuthorityResponseObserver {
	void handleResponse(ContentAuthority authority, URN urn, ContentResponseData response);
}
