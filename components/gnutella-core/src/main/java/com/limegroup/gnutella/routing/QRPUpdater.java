package com.limegroup.gnutella.routing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.FileManagerEvent;
import com.limegroup.gnutella.IncompleteFileDesc;
import com.limegroup.gnutella.util.LimeWireUtils;
import com.limegroup.gnutella.xml.LimeXMLReplyCollection;
import com.limegroup.gnutella.xml.LimeXMLSchemaRepository;
import com.limegroup.gnutella.xml.SchemaReplyCollectionMapper;

/**
 * Updates the QueryRouteTable. Listens for changes to shared files in the
 * FileManager. When changes occur, a new QRT will lazily be rebuilt.
 */
@Singleton
public class QRPUpdater implements EventListener<FileManagerEvent>, SettingListener, Service, Inspectable {

    /**
     * delay between qrp updates should the simpp words change.
     * Not final for testing.  Betas update faster for experiments.
     */
    private static long QRP_DELAY = (LimeWireUtils.isBetaRelease() ? 1 : 60) * 60 * 1000;

    private final FileManager fileManager;
    private final ScheduledExecutorService backgroundExecutor;
    private final Provider<LimeXMLSchemaRepository> limeXMLSchemaRepository;
    private final Provider<SchemaReplyCollectionMapper> schemaReplyCollectionMapper;
   
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
            Provider<SchemaReplyCollectionMapper> schemaReplyCollectionMapper,
            Provider<LimeXMLSchemaRepository> limeXMLSchemaRepository) {
        this.fileManager = fileManager;
        this.backgroundExecutor = backgroundExecutor;
        this.limeXMLSchemaRepository = limeXMLSchemaRepository;
        this.schemaReplyCollectionMapper = schemaReplyCollectionMapper;

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
        if (newWords.containsAll(qrpWords) && qrpWords.containsAll(newWords))
            return;

        qrpWords.clear();
        qrpWords.addAll(newWords);

        // if its already schedule to be rebuilt or a build is already needed return
        if( needRebuild || (scheduledSimppRebuildTimer != null && !scheduledSimppRebuildTimer.isDone()))
            return;

        // schedule a rebuild sometime in the next hour
        scheduledSimppRebuildTimer = backgroundExecutor.schedule( new Runnable(){
            public void run(){
                needRebuild = true;
            }}
            , (int) (Math.random() * QRP_DELAY), TimeUnit.MICROSECONDS);
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
        List<FileDesc> fds = fileManager.getGnutellaSharedFileList().getAllFileDescs();
        for (FileDesc fd : fds) {
            queryRouteTable.add(fd.getPath());
        }
        
        //if partial sharing is allowed, add incomplete file keywords also
        if(SharingSettings.ALLOW_PARTIAL_SHARING.getValue() &&
               SharingSettings.PUBLISH_PARTIAL_QRP.getValue()) {
            List<FileDesc> incompleteFds = fileManager.getIncompleteFileList().getAllFileDescs();
            for(FileDesc fd: incompleteFds) {
                IncompleteFileDesc ifd = (IncompleteFileDesc) fd;
                if (!ifd.hasUrnsAndPartialData())
                    continue;

                queryRouteTable.add(ifd.getFileName());
            }
        }

        for (String string : getXMLKeyWords())
            queryRouteTable.add(string);

        for (String string : getXMLIndivisibleKeyWords())
            queryRouteTable.addIndivisible(string);
    }

    /**
     * Returns a list of all the words in the annotations - leaves out numbers.
     * The list also includes the set of words that is contained in the names of
     * the files.
     */
    private List<String> getXMLKeyWords() {
        List<String> words = new ArrayList<String>();
        // Now get a list of keywords from each of the ReplyCollections
        String[] schemas = limeXMLSchemaRepository.get().getAvailableSchemaURIs();
        LimeXMLReplyCollection collection;
        int len = schemas.length;
        for (int i = 0; i < len; i++) {
            collection = schemaReplyCollectionMapper.get().getReplyCollection(schemas[i]);
            if (collection == null)// not loaded? skip it and keep goin'
                continue;
            words.addAll(collection.getKeyWords());
        }
        return words;
    }

    /**
     * @return A List of KeyWords from the FS that one does NOT want broken upon
     *         hashing into a QRT. Initially being used for schema uri hashing.
     */
    private List<String> getXMLIndivisibleKeyWords() {
        List<String> words = new ArrayList<String>();
        String[] schemas = limeXMLSchemaRepository.get().getAvailableSchemaURIs();
        LimeXMLReplyCollection collection;
        for (int i = 0; i < schemas.length; i++) {
            if (schemas[i] != null)
                words.add(schemas[i]);
            collection = schemaReplyCollectionMapper.get().getReplyCollection(schemas[i]);
            if (collection == null)// not loaded? skip it and keep goin'
                continue;
            words.addAll(collection.getKeyWordsIndivisible());
        }
        return words;
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
    }
    
    public void start() {}

    public void stop() {
        SearchSettings.PUBLISH_LIME_KEYWORDS.removeSettingListener(this);
        SearchSettings.LIME_QRP_ENTRIES.removeSettingListener(this);
    }

    /**
     * Listens to events from FileManager
     */
    public void handleEvent(FileManagerEvent evt) {
        switch(evt.getType()) {
            case ADD_FILE:
            case INCOMPLETE_URN_CHANGE:
            case CHANGE_FILE:
            case REMOVE_FILE:
            case RENAME_FILE:
            case LOAD_FILE:
            case REMOVE_FD:
                needRebuild = true;
        }
    }

    public Object inspect() {
        Map<String, Object> ret = new HashMap<String, Object>();

        synchronized(this) {
            ret.put("qrt",getQRT().getRawDump());
        }
        return ret;
    }
}
