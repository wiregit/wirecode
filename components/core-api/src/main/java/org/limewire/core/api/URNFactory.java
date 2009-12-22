package org.limewire.core.api;

import java.io.IOException;

public interface URNFactory {
    URN create(String description) throws IOException;
}
