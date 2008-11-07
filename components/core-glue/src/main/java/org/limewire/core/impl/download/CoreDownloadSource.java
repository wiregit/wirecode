package org.limewire.core.impl.download;

import org.limewire.core.api.download.DownloadSource;

public class CoreDownloadSource implements DownloadSource {
    private final String name;
    private final String address;

    public CoreDownloadSource(String name, String address) {
        this.name = name;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return name;
    }

    public String getAddress() {
        return address;
    }
}
