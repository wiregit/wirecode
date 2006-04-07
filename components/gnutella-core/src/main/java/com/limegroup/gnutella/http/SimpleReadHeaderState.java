package com.limegroup.gnutella.http;

import java.io.IOException;
import java.util.Properties;

import com.limegroup.gnutella.statistics.Statistic;

public class SimpleReadHeaderState extends ReadHeadersIOState {
    
    public SimpleReadHeaderState(Statistic stat) {
        super(new HeaderSupport(), stat);
    }

    protected void processConnectLine() throws IOException {
        // Does nothing.
    }

    protected void processHeaders() throws IOException {
        // Does nothing.
    }
    
    public Properties getHeaders() {
        return support.getHeaders();
    }
    
    public String getConnectLine() {
        return connectLine;
    }

}
