package org.limewire.core.impl;

import java.io.IOException;

import org.limewire.core.api.URNFactory;
import org.limewire.core.api.URN;

public class URNFactoryImpl implements URNFactory {
    @Override
    public URN create(String description) throws IOException {
        return com.limegroup.gnutella.URN.createUrnFromString(description);
    }
}
