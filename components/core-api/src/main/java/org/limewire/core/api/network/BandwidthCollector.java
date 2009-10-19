package org.limewire.core.api.network;

/**
 * Provides insight into current bandwidth usage per second, and the maximum
 * known bandwidth to be used for upload and downloads in the last two weeks.
 */
public interface BandwidthCollector {
    /**
     * Returns the maximum measured downstream bandwidth usage in kilobytes per
     * second.
     */
    public int getMaxMeasuredDownloadBandwidth();

    /**
     * Returns the maximum measured upstream bandwidth usage in kilobytes per
     * second.
     */
    public int getMaxMeasuredUploadBandwidth();

    /**
     * Returns the current downstream bandwidth usage in kilobytes per second.
     */
    public int getCurrentDownloadBandwidth();

    /**
     * Returns the current upstream bandwidth usage in kilobytes per second.
     */
    public int getCurrentUploadBandwidth();

}
