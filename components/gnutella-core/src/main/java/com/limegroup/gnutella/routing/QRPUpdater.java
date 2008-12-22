package com.limegroup.gnutella.routing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.limewire.core.settings.SearchSettings;
import org.limewire.core.settings.SharingSettings;
import org.limewire.inspection.Inspectable;
import org.limewire.lifecycle.Service;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileDescChangeEvent;
import com.limegroup.gnutella.library.FileList;
import com.limegroup.gnutella.library.FileListChangedEvent;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.library.IncompleteFileDesc;
import com.limegroup.gnutella.util.LimeWireUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * Updates the QueryRouteTable. Listens for changes to shared files in the
 * FileManager. When changes occur, a new QRT will lazily be rebuilt.
 */
@Singleton
public class QRPUpdater implements SettingListener, Service, Inspectable {

    /**
     * delay between qrp updates should the simpp words change.
     * Not final for testing.  Betas update faster for experiments.
     */
    private static long QRP_DELAY = (LimeWireUtils.isBetaRelease() ? 1 : 60) * 60 * 1000;

    private final FileManager fileManager;
    private final ScheduledExecutorService backgroundExecutor;
    private final ListenerSupport<FileDescChangeEvent> fileDescListenerSupport;
   
    /**
     * Schedules a delayed rebuild task when simpp changes occur
     */
    private ScheduledFuture<?> scheduledSimppRebuildTimer;

    /**
     * Holds references to all the entries in the current QRP. These are saved
     * to compare against any SIMPP messages to prevent unnecissary rebuilds.
     */
    private final Set<String> qrpWords = new HashSet<String>();

    /**
     * Boolean for checking if the QRT needs to be rebuilt.
     */
    private boolean needRebuild = true;

    /**
     * The QueryRouteTable kept by this.  The QueryRouteTable will be 
     * lazily rebuilt when necessary.
     */
    private QueryRouteTable queryRouteTable;

    @Inject
    public QRPUpdater(FileManager fileManager, 
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
            ListenerSupport<FileDescChangeEvent> fileDescListenerSupport) {
        this.fileManager = fileManager;
        this.backgroundExecutor = backgroundExecutor;
        this.fileDescListenerSupport = fileDescListenerSupport;

        for (String entry : SearchSettings.LIME_QRP_ENTRIES.getValue())
            qrpWords.add(entry);
    }

    public synchronized void settingChanged(SettingEvent evt) {
        // return immediately if we aren't publishing lime keywords
        if (!SearchSettings.PUBLISH_LIME_KEYWORDS.getBoolean()) 
            return;
        
        Set<String> newWords = new HashSet<String>();
        for (String entry : SearchSettings.LIME_QRP_ENTRIES.getValue())
            newWords.add(entry);

        // any change in words?
        if (newWords.containsAll(qrpWords) && qrpWords.containsAll(newWords)) {
            return;
        }

        qrpWords.clear();
        qrpWords.addAll(newWords);

        // if its already schedule to be rebuilt or a build is already needed return
        if( needRebuild || (scheduledSimppRebuildTimer != null && !scheduledSimppRebuildTimer.isDone())) {
            return;
        }

        // schedule a rebuild sometime in the next hour
        scheduledSimppRebuildTimer = backgroundExecutor.schedule(new Runnable() {
            public void run() {
                needRebuild = true;
            }
        }, (int) (Math.random() * QRP_DELAY), TimeUnit.MICROSECONDS);
    }

    /**
     * Returns a new QueryRouteTable. If the QRT is stale, will rebuild the 
     * QRT prior to returning a new QueryRouteTable.
     */
    public synchronized QueryRouteTable getQRT() {
        if (needRebuild) {
            if(scheduledSimppRebuildTimer != null )
                scheduledSimppRebuildTimer.cancel(true);
            buildQRT();
            needRebuild = false;
        }

        QueryRouteTable qrt = new QueryRouteTable(queryRouteTable.getSize());
        qrt.addAll(queryRouteTable);
        return qrt;
    }

    /**
     * Build the qrt.  
     */
    private void buildQRT() {
        queryRouteTable = new QueryRouteTable();
        if (SearchSettings.PUBLISH_LIME_KEYWORDS.getBoolean()) {
            for (String entry : SearchSettings.LIME_QRP_ENTRIES.getValue()) {
                queryRouteTable.addIndivisible(entry);
            }
        }
        
        FileList gnutella = fileManager.getGnutellaFileList();
        gnutella.getReadLock().lock();
        try {
            for (FileDesc fd : gnutella) {
                queryRouteTable.add(fd.getFileName());
                for(LimeXMLDocument doc : fd.getLimeXMLDocuments()) {
                    for(String word : doc.getKeyWords()) {
                        queryRouteTable.add(word);
                    }
                    for(String word : doc.getKeyWordsIndivisible()) {
                        queryRouteTable.addIndivisible(word);
                    }
                }
            }
        } finally {
            gnutella.getReadLock().unlock();
        }
        
        //if partial sharing is allowed, add incomplete file keywords also
        if(SharingSettings.ALLOW_PARTIAL_SHARING.getValue() && SharingSettings.PUBLISH_PARTIAL_QRP.getValue()) {
            FileList incompletes = fileManager.getIncompleteFileList();
            incompletes.getReadLock().lock();
            try {
                for(FileDesc fd : incompletes) {
                    IncompleteFileDesc ifd = (IncompleteFileDesc) fd;
                    if (ifd.hasUrnsAndPartialData()) {
                        queryRouteTable.add(ifd.getFileName());
                    }
                }
            } finally {
                incompletes.getReadLock().unlock();
            }
        }
    }
    
    @Inject
    void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(this);
    }
    
    public String getServiceName() {
        return org.limewire.i18n.I18nMarker.marktr("QRP Updater");
    }

    public void initialize() {
        SearchSettings.PUBLISH_LIME_KEYWORDS.addSettingListener(this);
        SearchSettings.LIME_QRP_ENTRIES.addSettingListener(this);
        fileManager.getGnutellaFileList().addFileListListener(new EventListener<FileListChangedEvent>() {
            @Override
            public void handleEvent(FileListChangedEvent event) {
                switch(event.getType()) {
                case ADDED:
                case REMOVED:
                case CLEAR:
                    needRebuild = true;
					break;
                }
            }
        });
        fileManager.getIncompleteFileList().addFileListListener(new EventListener<FileListChangedEvent>() {
            @Override
            public void handleEvent(FileListChangedEvent event) {
                switch(event.getType()) {
                case ADDED:
                case REMOVED:
                case CLEAR:
                    needRebuild = true;
					break;
                }   
            }
        });
        fileDescListenerSupport.addListener(new EventListener<FileDescChangeEvent>() {
            @Override
            public void handleEvent(FileDescChangeEvent event) {
                switch(event.getType()) {
                case URNS_CHANGED:
                    needRebuild = true;
					break;
                }
            }
        });
    }
    
    public void start() {}

    public void stop() {
        SearchSettings.PUBLISH_LIME_KEYWORDS.removeSettingListener(this);
        SearchSettings.LIME_QRP_ENTRIES.removeSettingListener(this);
    }

    public Object inspect() {
        Map<String, Object> ret = new HashMap<String, Object>();

        synchronized(this) {
            ret.put("qrt",getQRT().getRawDump());
        }
        return ret;
    }
}
