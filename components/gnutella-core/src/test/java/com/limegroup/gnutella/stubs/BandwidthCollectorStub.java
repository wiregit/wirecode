package com.limegroup.gnutella.stubs;

import org.limewire.core.settings.DownloadSettings;
import org.limewire.core.settings.UploadSettings;
import org.limewire.inject.EagerSingleton;

import com.limegroup.gnutella.BandwidthCollectorDriver;

@EagerSingleton
public class BandwidthCollectorStub implements BandwidthCollectorDriver {

    private Integer maxMeasuredUploadBandwidth = null;
    private Integer maxMeasuredDownloadBandwidth = null;
    private Integer currentTotalUploadBandwidth = null;
    private Integer currentTotalDownloadBandwidth = null;
    private Integer currentUploaderBandwidth = null;
    private Integer currentDownloaderBandwidth = null;

    @Override
    public int getCurrentTotalDownloadBandwidth() {
        if (currentTotalDownloadBandwidth != null) {
            return currentTotalDownloadBandwidth.intValue();
        }
        return 0;
    }

    @Override
    public int getCurrentTotalUploadBandwidth() {
        if (currentTotalUploadBandwidth != null) {
            return currentTotalUploadBandwidth.intValue();
        }
        return 0;
    }

    @Override
    public int getMaxMeasuredTotalDownloadBandwidth() {
        if (maxMeasuredDownloadBandwidth != null) {
            return maxMeasuredDownloadBandwidth.intValue();
        }
        return DownloadSettings.MAX_MEASURED_DOWNLOAD_KBPS.getValue();
    }

    @Override
    public int getMaxMeasuredTotalUploadBandwidth() {
        if (maxMeasuredUploadBandwidth != null) {
            return maxMeasuredUploadBandwidth.intValue();
        }
        return UploadSettings.MAX_MEASURED_UPLOAD_KBPS.getValue();
    }

    @Override
    public int getCurrentDownloaderBandwidth() {
        if (currentDownloaderBandwidth != null) {
            return currentDownloaderBandwidth.intValue();
        }
        return 0;
    }

    @Override
    public int getCurrentUploaderBandwidth() {
        if (currentUploaderBandwidth != null) {
            return currentUploaderBandwidth.intValue();
        }
        return 0;
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
    public void overrideCurrentTotalDownloadBandwidth(Integer override) {
        this.currentTotalDownloadBandwidth = override;
    }

    /**
     * Overrides the current upload bandwidth until rest with a null value.
     */
    public void overrideCurrentTotalUploadBandwidth(Integer override) {
        this.currentTotalUploadBandwidth = override;
    }

    /**
     * Overrides the current download bandwidth until rest with a null value.
     */
    public void overrideCurrentDownloaderBandwidth(Integer override) {
        this.currentDownloaderBandwidth = override;
    }

    /**
     * Overrides the current upload bandwidth until rest with a null value.
     */
    public void overrideCurrentUploaderBandwidth(Integer override) {
        this.currentUploaderBandwidth = override;
    }
}
