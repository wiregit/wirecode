package com.limegroup.gnutella.connection;

import java.io.*;

public interface HeaderWriter {

    boolean write() throws IOException;

    void writeHeader(String header) throws IOException;

    void closeHeaderWriting() throws IOException;

    void setWriteRegistered(boolean registered);
}
