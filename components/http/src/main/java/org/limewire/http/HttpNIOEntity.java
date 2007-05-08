package org.limewire.http;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;

/**
 * Defines an interface for event based HTTP entities.
 */
public interface HttpNIOEntity extends HttpEntity {

    /**
     * Reads data from <code>decoder</code>. 
     * 
     * @param decoder the decoder for reading input
     * @param ioctrl I/O control for suspending and resuming events 
     * @return the number of bytes read
     * @throws IOException indicates an I/O error which will abort the
     *         connection
     */
    int consumeContent(ContentDecoder decoder, IOControl ioctrl)
            throws IOException;

    /**
     * Writes data to <code>encoder</code>. 
     * 
     * @param encoder the encoder for writing output
     * @param ioctrl I/O control for suspending and resuming events 
     * @throws IOException indicates an I/O error which will abort the
     *         connection
     */
    void produceContent(ContentEncoder encoder, IOControl ioctrl)
            throws IOException;

    /**
     * Invoked when processing has completed. This completes the transfer of the
     * entity and is invoked when processing finishes normally, i.e.
     * {@link ContentEncoder#isCompleted()} or
     * {@link ContentDecoder#isCompleted()} return true, or when an exception
     * occurs.
     */
    void finished();

}
