package org.limewire.core.impl.network;

import org.limewire.core.api.network.BandwidthCollector;

/**
 * Implementation of BandwidthCollector for the mock core.
 */
public class MockBandwidthCollector implements BandwidthCollector {

    @Override
    public int getCurrentTotalDownloadBandwidth() {
        return 25;
    }

    @Override
    public int getCurrentTotalUploadBandwidth() {
        return 15;
    }

    @Override
    public int getMaxMeasuredTotalDownloadBandwidth() {
        return 30;
    }

    @Override
    public int getMaxMeasuredTotalUploadBandwidth() {
        return 20;
    }

    @Override
    public int getCurrentDownloaderBandwidth() {
        return 30;
    }

    @Override
    public int getCurrentUploaderBandwidth() {
        return 20;
    }
}
