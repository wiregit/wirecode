package org.limewire.mojito2.io;

import java.io.IOException;
import java.io.OutputStream;

public interface Writeable {

    public int write(OutputStream out) throws IOException;
}
