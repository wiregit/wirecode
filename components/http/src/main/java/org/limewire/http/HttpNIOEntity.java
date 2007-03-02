package org.limewire.http;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.nio.ContentEncoder;

public interface HttpNIOEntity extends HttpEntity {

    void produceContent(ContentEncoder encoder) throws IOException;

}
