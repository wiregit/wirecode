package com.limegroup.bittorrent;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentEvent;
import org.limewire.bittorrent.TorrentEventType;
import org.limewire.bittorrent.TorrentManager;
import org.limewire.bittorrent.TorrentManagerSettings;
import org.limewire.bittorrent.TorrentStatus;
import org.limewire.bittorrent.TorrentIpFilter;
import org.limewire.core.settings.BittorrentSettings;
import org.limewire.core.settings.SharingSettings;
import org.limewire.inject.EagerSingleton;
import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectionPoint;
import org.limewire.libtorrent.TorrentManagerImpl;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.listener.EventListener;
import org.limewire.util.FileUtils;
import org.limewire.io.IP;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.limegroup.gnutella.filters.IPFilter;

/**
 * Lazy TorrentManager wraps the TorrentManagerImpl and allows holding off
 * initializing against the native libraries until the first time and methods
 * are called on the torrent manager.
 * 
 * It registers itself as a service to still enabled proper shutdown of the
 * Torrent code, but cleanup only needs to be done is the underlying
 * implementation was initialized.
 * 
 */
@EagerSingleton
public class LimeWireTorrentManager implements TorrentManager, Service {
    private final Provider<TorrentManagerImpl> torrentManager;

    private final EventListener<TorrentEvent> torrentListener = new EventListener<TorrentEvent>() {
        @Override
        public void handleEvent(TorrentEvent event) {
            handleTorrentEvent(event);
        }
    };

    private volatile boolean initialized = false;
    private final IpFilterPredicate ipFilterPredicate;

    @SuppressWarnings("unused")
    @InspectionPoint("torrent manager status")
    private final Inspectable torrentManagerStatus = new TorrentManagerStatus();

    @Inject
    public LimeWireTorrentManager(Provider<TorrentManagerImpl> torrentManager,
                                  IPFilter ipFilter) {
        this.torrentManager = torrentManager;
        this.ipFilterPredicate = new IpFilterPredicate(ipFilter);
    }

    private void handleTorrentEvent(TorrentEvent event) {
        if (event.getType() == TorrentEventType.COMPLETED) {
            limitSeedingTorrents();
        } else if (event.getType() == TorrentEventType.STOPPED) {
            event.getTorrent().removeListener(torrentListener);
        }
    }

    @Inject
    public void register(ServiceRegistry serviceRegistry) {
        serviceRegistry.register(this);
    }

    private void setupTorrentManager() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    try {
                        this.torrentManager.get().initialize();

                        if (torrentManager.get().isValid()) {
                            torrentManager.get().setIpFilter(ipFilterPredicate);
                            if (BittorrentSettings.TORRENT_USE_UPNP.getValue()) {
                                torrentManager.get().startUPnP();
                            } else {
                                torrentManager.get().stopUPnP();
                            }
                            this.torrentManager.get().start();
                            this.torrentManager.get().scheduleWithFixedDelay(
                                    new TorrentDHTScheduler(this), 1000, 60 * 1000,
                                    TimeUnit.MILLISECONDS);
                            this.torrentManager.get().scheduleWithFixedDelay(
                                    new TorrentResumeDataScheduler(this), 10000, 10000,
                                    TimeUnit.MILLISECONDS);
                        }
                    } finally {
                        initialized = true;
                    }
                }
            }
        }
    }

    @Override
    public String getServiceName() {
        return "TorrentManager";
    }

    @Override
    public TorrentManagerSettings getTorrentManagerSettings() {
        // not calling setup because we don't want to initialize the library
        // here.
        // settings can be gotten without initialization.
        return torrentManager.get().getTorrentManagerSettings();
    }

    @Override
    public void initialize() {
        // handled in setup method.
    }

    @Override
    public void setIpFilter(TorrentIpFilter ipFilterPredicate) {
        setupTorrentManager();
        torrentManager.get().setIpFilter(ipFilterPredicate);    
    }

    @Override
    public boolean isDownloadingTorrent(File torrentFile) {
        if (!initialized) {
            return false;
        }

        setupTorrentManager();
        return torrentManager.get().isDownloadingTorrent(torrentFile);
    }

    @Override
    public Torrent getTorrent(File torrentFile) {
        if (!initialized) {
            return null;
        }

        setupTorrentManager();
        return torrentManager.get().getTorrent(torrentFile);
    }

    @Override
    public Torrent getTorrent(String sha1) {
        if (!initialized) {
            return null;
        }

        setupTorrentManager();
        return torrentManager.get().getTorrent(sha1);
    }

    @Override
    public boolean isValid() {
        setupTorrentManager();
        return torrentManager.get().isValid();
    }

    @Override
    public boolean addTorrent(Torrent torrent) {
        if (!isValid()) {
            return false;
        }
        torrent.getLock().lock();
        try {
            File torrentFile = torrent.getTorrentFile();
            File torrentParent = torrentFile.getParentFile();
            File torrentDownloadFolder = SharingSettings.INCOMPLETE_DIRECTORY.get();
            File torrentUploadFolder = BittorrentSettings.TORRENT_UPLOADS_FOLDER.get();
            if (!torrentParent.equals(torrentDownloadFolder)
                    && !torrentParent.equals(torrentUploadFolder)) {
                // if the torrent file is not located in the incomplete or
                // upload
                // directories it should be copied to the directory the torrent
                // is
                // being downloaded to. This is to prevent the user from
                // deleting
                // the torrent which we need to initiate a download properly.
                torrentDownloadFolder.mkdirs();
                File newTorrentFile = new File(torrentDownloadFolder, torrent.getName()
                        + ".torrent");
                FileUtils.copy(torrentFile, newTorrentFile);
                torrent.setTorrentFile(newTorrentFile);
            }
        } finally {
            torrent.getLock().unlock();
        }
        return torrentManager.get().addTorrent(torrent);
    }

    @Override
    public void removeTorrent(Torrent torrent) {
        setupTorrentManager();
        torrentManager.get().removeTorrent(torrent);
        torrent.removeListener(torrentListener);
    }

    @Override
    public void start() {
        // handled in setup method.
    }

    @Override
    public void stop() {
        synchronized (this) {
            try {
                if (initialized && torrentManager.get().isValid()) {
                    torrentManager.get().stop();
                }
            } finally {
                initialized = true;
            }
        }
    }

    @Override
    public void setTorrentManagerSettings(TorrentManagerSettings settings) {
        setupTorrentManager();
        torrentManager.get().setTorrentManagerSettings(settings);
        limitSeedingTorrents();
    }

    private enum Status {
        NOT_INITIALIZED, LOADED, FAILED
    }
    
    private static class IpFilterPredicate implements TorrentIpFilter {
        
        private final IPFilter ipFilter;
        
        IpFilterPredicate(IPFilter ipFilter) {
            this.ipFilter = ipFilter;    
        }
        
        @Override
        public boolean allow(int ipAddress) {
            return ipFilter.allow(new IP(ipAddress, -1));
        }
    }
            
    private class TorrentManagerStatus implements Inspectable {

        @Override
        public Object inspect() {
            synchronized (LimeWireTorrentManager.this) {
                return initialized ? (torrentManager.get().isValid() ? Status.LOADED.toString()
                        : Status.FAILED.toString()) : Status.NOT_INITIALIZED.toString();
            }
        }
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public List<Torrent> getTorrents() {
        if (!initialized) {
            return Collections.emptyList();
        }

        setupTorrentManager();
        return torrentManager.get().getTorrents();
    }

    @Override
    public Lock getLock() {
        setupTorrentManager();
        return torrentManager.get().getLock();
    }

    @Override
    public boolean isDHTStarted() {
        if (!initialized) {
            return false;
        }
        setupTorrentManager();
        return torrentManager.get().isDHTStarted();
    }

    @Override
    public void startDHT(File dhtStateFile) {
        setupTorrentManager();
        torrentManager.get().startDHT(dhtStateFile);
    }

    @Override
    public void stopDHT() {
        setupTorrentManager();
        torrentManager.get().stopDHT();
    }

    @Override
    public void saveDHTState(File dhtStateFile) {
        if (!initialized) {
            return;
        }
        setupTorrentManager();
        torrentManager.get().saveDHTState(dhtStateFile);
    }

    @Override
    public boolean isUPnPStarted() {
        if (!initialized) {
            return false;
        }
        setupTorrentManager();
        return torrentManager.get().isUPnPStarted();
    }

    @Override
    public void startUPnP() {
        setupTorrentManager();
        torrentManager.get().startUPnP();
    }

    @Override
    public void stopUPnP() {
        setupTorrentManager();
        torrentManager.get().stopUPnP();
    }

    private void limitSeedingTorrents() {
        // Check the number of seeding torrents and stop any long running
        // torrents
        // if there are more there are more than the limit

        torrentManager.get().getLock().lock();

        try {
            int seedingTorrents = 0;
            
            int maxSeedingTorrents = BittorrentSettings.TORRENT_SEEDING_LIMIT.getValue();
            if (BittorrentSettings.UPLOAD_TORRENTS_FOREVER.getValue()) {
                maxSeedingTorrents =  Integer.MAX_VALUE;
            }

            // Cut out early if the limit is infinite
            if (maxSeedingTorrents == Integer.MAX_VALUE) {
                return;
            }

            for (Torrent torrent : torrentManager.get().getTorrents()) {
                if (torrent.isFinished()) {
                    seedingTorrents++;
                }
            }

            if (seedingTorrents <= maxSeedingTorrents) {
                return;
            }

            List<Torrent> ratioSortedTorrents = new ArrayList<Torrent>(torrentManager.get()
                    .getTorrents());
            Collections.sort(ratioSortedTorrents, new Comparator<Torrent>() {
                @Override
                public int compare(Torrent o1, Torrent o2) {
                    // Sort smallest first
                    int compare = Double.compare(o2.getSeedRatio(), o1.getSeedRatio());

                    // Compare by seeding time if seeding ratio is the same
                    // (generally at 0:0)
                    // -- Older values are discarded first. --
                    if (compare == 0) {
                        TorrentStatus status1 = o1.getStatus();
                        TorrentStatus status2 = o2.getStatus();
                        if (status1 != null && status2 != null) {
                            int time1 = status1.getSeedingTime();
                            int time2 = status2.getSeedingTime();
                            if (time1 > time2) {
                                return -1;
                            } else if (time2 > time1) {
                                return 1;
                            } else {
                                return 0;
                            }
                        }
                    }

                    return compare;
                }
            });

            for (int i = 0; i < seedingTorrents - maxSeedingTorrents
                    && ratioSortedTorrents.size() > 0;) {
                Torrent torrent = ratioSortedTorrents.remove(0);

                if (torrent.isFinished()) {
                    torrent.stop();
                    i++;
                }
            }
        } finally {
            torrentManager.get().getLock().unlock();
        }
    }
}
