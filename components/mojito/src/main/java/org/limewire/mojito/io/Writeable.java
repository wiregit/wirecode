package org.limewire.mojito.io;

import java.io.IOException;
import java.io.OutputStream;

public interface Writeable {

    public int write(OutputStream out) throws IOException;
}
