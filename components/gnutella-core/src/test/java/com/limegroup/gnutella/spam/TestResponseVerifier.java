package com.limegroup.gnutella.spam;

import java.util.Arrays;

import org.limewire.core.api.search.SearchCategory;

import com.google.inject.Singleton;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.ResponseVerifier;
import com.limegroup.gnutella.messages.QueryRequest;

@Singleton
class TestResponseVerifier implements ResponseVerifier {
    String query = null;
    byte[] guid = null;

    @Override
    public void record(QueryRequest qr) {
    }

    @Override
    public void record(QueryRequest qr, SearchCategory type) {
    }

    @Override
    public boolean matchesQuery(byte [] guid, Response response) {
        return true;
    }

    @Override
    public boolean matchesType(byte[] guid, Response response) {
        return true;
    }

    @Override
    public boolean isMandragoreWorm(byte[] guid, Response response) {
        return false;
    }

    @Override
    public String getQueryString(byte[] guid) {
        if(this.guid == null || !Arrays.equals(guid, this.guid))
            return null;
        return query;
    }
}