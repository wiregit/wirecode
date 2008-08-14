package org.limewire.swarm.http.listener;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.nio.entity.ContentListener;

public interface ResponseContentListener extends ContentListener {

    void initialize(HttpResponse response) throws IOException;
    
}
