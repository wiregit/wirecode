package com.limegroup.gnutella.stubs;

import org.limewire.core.settings.DownloadSettings;
import org.limewire.core.settings.UploadSettings;
import org.limewire.inject.EagerSingleton;

import com.limegroup.gnutella.BandwidthCollectorDriver;

@EagerSingleton
public class BandwidthCollectorStub implements BandwidthCollectorDriver {

    private Integer maxMeasuredUploadBandwidth = null;
    private Integer maxMeasuredDownloadBandwidth = null;
    private Integer currentUploadBandwidth = null;
    private Integer currentDownloadBandwidth = null;

    @Override
    public int getCurrentDownloadBandwidth() {
        if (currentDownloadBandwidth != null) {
            return currentDownloadBandwidth.intValue();
        }
        return 0;
    }

    @Override
    public int getCurrentUploadBandwidth() {
        if (currentUploadBandwidth != null) {
            return currentUploadBandwidth.intValue();
        }
        return 0;
    }

    @Override
    public int getMaxMeasuredDownloadBandwidth() {
        if (maxMeasuredDownloadBandwidth != null) {
            return maxMeasuredDownloadBandwidth.intValue();
        }
        return DownloadSettings.MAX_MEASURED_DOWNLOAD_KBPS.getValue();
    }

    @Override
    public int getMaxMeasuredUploadBandwidth() {
        if (maxMeasuredUploadBandwidth != null) {
            return maxMeasuredUploadBandwidth.intValue();
        }
        return UploadSettings.MAX_MEASURED_UPLOAD_KBPS.getValue();
    }

    @Override
    public void collectBandwidthData() {

    }

    /**
     * Overrides the max measured download bandwidth until rest with a null
     * value.
     */
    public void overrideMaxMeasureDownloadBandwidth(Integer override) {
        this.maxMeasuredDownloadBandwidth = override;
    }

    /**
     * Overrides the max measured upload bandwidth until rest with a null value.
     */
    public void overrideMaxMeasureUploadBandwidth(Integer override) {
        this.maxMeasuredUploadBandwidth = override;
    }

    /**
     * Overrides the current download bandwidth until rest with a null value.
     */
    public void overrideCurrentDownloadBandwidth(Integer override) {
        this.currentDownloadBandwidth = override;
    }

    /**
     * Overrides the current upload bandwidth until rest with a null value.
     */
    public void overrideCurrentUploadBandwidth(Integer override) {
        this.currentUploadBandwidth = override;
    }
}
