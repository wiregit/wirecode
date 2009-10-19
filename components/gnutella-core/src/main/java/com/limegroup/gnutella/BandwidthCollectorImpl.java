package com.limegroup.gnutella;

import java.util.concurrent.atomic.AtomicInteger;

import org.limewire.core.settings.DownloadSettings;
import org.limewire.core.settings.UploadSettings;
import org.limewire.inject.EagerSingleton;
import org.limewire.inspection.InspectionHistogram;
import org.limewire.inspection.InspectionPoint;
import org.limewire.statistic.BasicKilobytesStatistic;
import org.limewire.statistic.Statistic;
import org.limewire.statistic.StatisticAccumulator;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

@EagerSingleton
public class BandwidthCollectorImpl implements BandwidthCollectorDriver {

    private final Provider<ConnectionManager> connectionManager;
    private final Provider<BandwidthTracker> uploadTracker;
    private final Provider<BandwidthTracker> downloadTracker;

    // these inspections include:
    // gnutella downloads and uploads, torrents,
    // gnutella messaging and mozilla downloads.
    // missing gnutella dht, missing torrent dht, missing other connection types

    /**
     * 200 measurements are saved, 1 per second.
     */
    @InspectionPoint("upstream bandwidth history")
    private final Statistic uploadStat; //

    /**
     * 200 measurements are saved, 1 per second.
     */
    @InspectionPoint("downstream bandwidth history")
    private final Statistic downloadStat;

    @InspectionPoint("upload bandwidth histogram")
    private final InspectionHistogram<Integer> uploadHistogram;

    @InspectionPoint("download bandwidth histogram")
    private final InspectionHistogram<Integer> downloadHistogram;

    private final AtomicInteger currentUploadBandwidthKiloBytes = new AtomicInteger(0);
    private final AtomicInteger currentDownloadBandwidthKiloBytes = new AtomicInteger(0);

    @Inject
    public BandwidthCollectorImpl(@Named("uploadTracker") Provider<BandwidthTracker> uploadTracker,
            @Named("downloadTracker") Provider<BandwidthTracker> downloadTracker,
            Provider<ConnectionManager> connectionManager, StatisticAccumulator statisticAccumulator) {
        this.uploadTracker = uploadTracker;
        this.downloadTracker = downloadTracker;
        this.connectionManager = connectionManager;
        this.uploadStat = new BandwidthStat(statisticAccumulator);
        this.downloadStat = new BandwidthStat(statisticAccumulator);
        this.downloadHistogram = new InspectionHistogram<Integer>();
        this.uploadHistogram = new InspectionHistogram<Integer>();

    }

    @Override
    public int getMaxMeasuredDownloadBandwidth() {
        return DownloadSettings.MAX_MEASURED_DOWNLOAD_KBPS.getValue();
    }

    @Override
    public int getMaxMeasuredUploadBandwidth() {
        return UploadSettings.MAX_MEASURED_UPLOAD_KBPS.getValue();
    }

    @Override
    public int getCurrentDownloadBandwidth() {
        return currentDownloadBandwidthKiloBytes.get();
    }

    @Override
    public int getCurrentUploadBandwidth() {
        return currentUploadBandwidthKiloBytes.get();
    }

    /**
     * Collects data on the bandwidth that has been used for file uploads and
     * downloads.
     */
    @Override
    public void collectBandwidthData() {
        uploadTracker.get().measureBandwidth();
        downloadTracker.get().measureBandwidth();
        connectionManager.get().measureBandwidth();
        float bandwidth;
        try {
            bandwidth = uploadTracker.get().getMeasuredBandwidth();
        } catch (InsufficientDataException ide) {
            bandwidth = 0;
        }
        int newUpstreamKiloBytesPerSec = (int) bandwidth
                + (int) connectionManager.get().getMeasuredUpstreamBandwidth();
        uploadStat.addData(newUpstreamKiloBytesPerSec);
        uploadHistogram.count(newUpstreamKiloBytesPerSec);
        try {
            bandwidth = downloadTracker.get().getMeasuredBandwidth();
        } catch (InsufficientDataException ide) {
            bandwidth = 0;
        }
        int newDownstreamKiloBytesPerSec = (int) bandwidth
                + (int) connectionManager.get().getMeasuredDownstreamBandwidth();
        downloadStat.addData(newDownstreamKiloBytesPerSec);
        downloadHistogram.count(newDownstreamKiloBytesPerSec);

        int maxUpstreamKiloBytesPerSec = getMaxMeasuredUploadBandwidth();
        if (newUpstreamKiloBytesPerSec > maxUpstreamKiloBytesPerSec) {
            maxUpstreamKiloBytesPerSec = newUpstreamKiloBytesPerSec;
            UploadSettings.MAX_MEASURED_UPLOAD_KBPS.setValue(maxUpstreamKiloBytesPerSec);
        }

        int maxDownstreamKiloBytesPerSec = getMaxMeasuredDownloadBandwidth();
        if (newDownstreamKiloBytesPerSec > maxDownstreamKiloBytesPerSec) {
            maxDownstreamKiloBytesPerSec = newDownstreamKiloBytesPerSec;
            DownloadSettings.MAX_MEASURED_DOWNLOAD_KBPS.setValue(maxDownstreamKiloBytesPerSec);
        }

        currentDownloadBandwidthKiloBytes.set(newDownstreamKiloBytesPerSec);
        currentUploadBandwidthKiloBytes.set(newUpstreamKiloBytesPerSec);
    }

    private class BandwidthStat extends BasicKilobytesStatistic {
        public BandwidthStat(StatisticAccumulator statisticAccumulator) {
            super(statisticAccumulator);
        }
    }
}
