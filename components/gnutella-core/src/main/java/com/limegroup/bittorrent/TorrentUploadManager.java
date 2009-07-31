package com.limegroup.bittorrent;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentManager;
import org.limewire.core.settings.BittorrentSettings;
import org.limewire.inject.LazySingleton;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.FileUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.limegroup.gnutella.ActivityCallback;

/**
 * This class is responsible for saving a torrent as an upload memento and
 * loading them at startup.
 */
@LazySingleton
public class TorrentUploadManager implements BTUploaderFactory {

    private static final Log LOG = LogFactory.getLog(TorrentUploadManager.class);

    private final Provider<ActivityCallback> activityCallback;
    
    private final Provider<TorrentManager> torrentManager;

    private final Provider<Torrent> torrentProvider;

    @Inject
    public TorrentUploadManager(Provider<TorrentManager> torrentManager,
            Provider<Torrent> torrentProvider, Provider<ActivityCallback> activityCallback) {
        this.torrentManager = torrentManager;
        this.torrentProvider = torrentProvider;
        this.activityCallback = activityCallback;
    }

    /**
     * Iterates through the uploads folder finding saved torrent mementos and
     * starting off the uploads.
     */
    public void loadSavedUploads() {
        File uploadsDirectory = BittorrentSettings.LIBTORRENT_UPLOADS_FOLDER.get();
        if (uploadsDirectory.exists()) {
            File[] uploadMementos = uploadsDirectory.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return "memento".equals(FileUtils.getFileExtension(file));
                }
            });

            if (uploadMementos != null) {
                for (File mementoFile : uploadMementos) {
                    try {
                        Map<String, Object> memento = readMemento(mementoFile);
                        if (memento != null) {
                            Torrent torrent = torrentProvider.get();

                            File torrentFile = (File) memento.get("torrentFile");
                            File fastResumeFile = (File) memento.get("fastResumeFile");
                            File torrentDataFile = (File) memento.get("torrentDataFile");
                            String sha1 = (String)memento.get("sha1");
                            String trackerURL = (String)memento.get("trackerURL");
                            String name = (String)memento.get("name");
                            
                            if (torrentDataFile.exists()) {
                                if (!torrentManager.get().isDownloadingTorrent(mementoFile)) {
                                    torrent.init(name, sha1, -1, trackerURL, null, fastResumeFile,
                                            torrentFile, torrentDataFile, null);
                                    torrentManager.get().registerTorrent(torrent);
                                    createBTUploader(torrent);
                                    torrent.start();
                                }
                            }
                        }
                    } catch (Exception e) {
                        LOG.error("Error resuming upload for: " + mementoFile, e);

                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readMemento(File mementoFile) throws IOException,
            ClassNotFoundException {
        Object mementoObject = FileUtils.readObject(mementoFile);
        Map<String, Object> memento = null;
        if (mementoObject instanceof Map) {
            memento = (Map<String, Object>) mementoObject;
        }
        return memento;
    }

    /**
     * Creates an upload memento from the Torrent and writes it to disk.
     */
    public void writeMemento(Torrent torrent) throws IOException {
        File torrentMomento = getMementoFile(torrent);
        torrentMomento.getParentFile().mkdirs();

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("torrentDataFile", torrent.getTorrentDataFile().getAbsoluteFile());
        map.put("torrentFile", torrent.getTorrentFile().getAbsoluteFile());
        map.put("fastResumeFile", torrent.getFastResumeFile().getAbsoluteFile());
        map.put("sha1", torrent.getSha1());
        map.put("trackerURL", torrent.getTrackerURL());
        map.put("name", torrent.getName());

        FileUtils.writeObject(torrentMomento, map);
    }

    private File getMementoFile(Torrent torrent) {
        File torrentMomento = new File(torrentManager.get().getTorrentSettings()
                .getTorrentUploadsFolder(), torrent.getName() + ".memento");
        return torrentMomento;
    }

    /**
     * Removes any found upload mementos/artifacts for the given torrent from
     * disk.
     */
    public void removeMemento(Torrent torrent) {
        File torrentMomento = getMementoFile(torrent);
        FileUtils.forceDelete(torrentMomento);
        FileUtils.forceDelete(torrent.getTorrentFile());
        FileUtils.forceDelete(torrent.getFastResumeFile());
    }
    
    @Override
    public BTUploader createBTUploader(Torrent torrent) {
        BTUploader btUploader = new BTUploader(torrent, activityCallback.get(), this);
        btUploader.registerTorrentListener();
        activityCallback.get().addUpload(btUploader);
        return btUploader;
    }
}
