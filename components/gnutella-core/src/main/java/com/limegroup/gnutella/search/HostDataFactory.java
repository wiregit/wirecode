package com.limegroup.gnutella.search;

import com.limegroup.gnutella.messages.QueryReply;

public interface HostDataFactory {

    public HostData createHostData(QueryReply reply);

}