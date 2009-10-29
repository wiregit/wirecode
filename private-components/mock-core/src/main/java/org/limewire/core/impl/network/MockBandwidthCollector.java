package org.limewire.core.impl.network;

import org.limewire.core.api.network.BandwidthCollector;

/**
 * Implementation of BandwidthCollector for the mock core.
 */
public class MockBandwidthCollector implements BandwidthCollector {

    @Override
    public int getCurrentDownloadBandwidth() {
        return 25;
    }

    @Override
    public int getCurrentUploadBandwidth() {
        return 15;
    }

    @Override
    public int getMaxMeasuredDownloadBandwidth() {
        return 30;
    }

    @Override
    public int getMaxMeasuredUploadBandwidth() {
        return 20;
    }
}
