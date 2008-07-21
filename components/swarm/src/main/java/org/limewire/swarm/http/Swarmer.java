package org.limewire.swarm.http;

import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponseInterceptor;



public interface Swarmer {
    
    void addSource(SwarmSource source);
    
    void addSource(SwarmSource source, SourceEventListener sourceEventListener);
    
    void start();
    
    void shutdown();
    
    public void addHeaderInterceptor(HttpRequestInterceptor requestInterceptor);
    
    public void addHeaderInterceptor(HttpResponseInterceptor responseInterceptor);
    
    public boolean finished();

}
