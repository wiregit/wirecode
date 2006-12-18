package com.limegroup.gnutella.auth;

import com.limegroup.gnutella.URN;

public interface ContentAuhorityResponseObserver {
	void handleResponse(ContentAuthority authority, URN urn, ContentResponseData response);
}
