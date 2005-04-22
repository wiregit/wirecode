package com.limegroup.gnutella.io;

/**
 * Allows the data in this handler to be transfered to another channel.
 */
interface TransferableHandler {
    public void transfer(java.nio.channels.WritableByteChannel to) throws java.io.IOException;
}