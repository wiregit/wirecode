package com.limegroup.gnutella.downloader;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.limewire.io.InvalidDataException;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.bittorrent.BTDownloader;
import com.limegroup.bittorrent.BTMetaInfo;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SaveLocationException;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.downloader.serial.BTDownloadMemento;
import com.limegroup.gnutella.downloader.serial.DownloadMemento;
import com.limegroup.gnutella.downloader.serial.GnutellaDownloadMemento;
import com.limegroup.gnutella.downloader.serial.InNetworkMemento;
import com.limegroup.gnutella.downloader.serial.RemoteHostMemento;
import com.limegroup.gnutella.version.DownloadInformation;

@Singleton
public class CoreDownloaderFactoryImpl implements CoreDownloaderFactory {

    private final Provider<ManagedDownloader> managedDownloaderFactory;
    private final Provider<MagnetDownloader> magnetDownloaderFactory;
    private final Provider<InNetworkDownloader> inNetworkDownloaderFactory;
    private final Provider<ResumeDownloader> resumeDownloaderFactory;
    private final Provider<StoreDownloader> storeDownloaderFactory;
    private final Provider<BTDownloader> btDownloaderFactory;
    private final RemoteFileDescFactory remoteFileDescFactory;
    
    @Inject
    public CoreDownloaderFactoryImpl(Provider<ManagedDownloader> managedDownloaderFactory,
            Provider<MagnetDownloader> magnetDownloaderFactory,
            Provider<InNetworkDownloader> inNetworkDownloaderFactory,
            Provider<ResumeDownloader> resumeDownloaderFactory,
            Provider<StoreDownloader> storeDownloaderFactory,
            Provider<BTDownloader> btDownloaderFactory,
            RemoteFileDescFactory remoteFileDescFactory) {
        this.managedDownloaderFactory = managedDownloaderFactory;
        this.magnetDownloaderFactory = magnetDownloaderFactory;
        this.inNetworkDownloaderFactory = inNetworkDownloaderFactory;
        this.resumeDownloaderFactory = resumeDownloaderFactory;
        this.storeDownloaderFactory = storeDownloaderFactory;
        this.btDownloaderFactory = btDownloaderFactory;
        this.remoteFileDescFactory = remoteFileDescFactory;
    }

    public ManagedDownloader createManagedDownloader(RemoteFileDesc[] files,
            GUID originalQueryGUID, File saveDirectory, String fileName, boolean overwrite)
            throws SaveLocationException {
        ManagedDownloader md = managedDownloaderFactory.get();
        md.addInitialSources(Arrays.asList(files), fileName);
        md.setQueryGuid(originalQueryGUID);
        md.setSaveFile(saveDirectory, fileName, overwrite);
        return md;
    }

    public MagnetDownloader createMagnetDownloader(MagnetOptions magnet, boolean overwrite,
            File saveDirectory, String fileName) throws SaveLocationException {
        if (!magnet.isDownloadable())
            throw new IllegalArgumentException("magnet not downloadable");
        if (fileName == null)
            fileName = magnet.getFileNameForSaving();

        MagnetDownloader md = magnetDownloaderFactory.get();
        md.addInitialSources(null, fileName);
        md.setSaveFile(saveDirectory, fileName, overwrite);
        md.initMagnet(magnet);
        return md;
    }

    public InNetworkDownloader createInNetworkDownloader(
            DownloadInformation info, File dir, long startTime)
            throws SaveLocationException {
        InNetworkDownloader id = inNetworkDownloaderFactory.get();
        id.addInitialSources(null, info.getUpdateFileName());
        id.setSaveFile(dir, info.getUpdateFileName(), true);
        id.initDownloadInformation(info, startTime);
        return id;
    }

    public ResumeDownloader createResumeDownloader(File incompleteFile,
            String name, long size) throws SaveLocationException {
        ResumeDownloader rd = resumeDownloaderFactory.get();
        rd.addInitialSources(null, name);
        rd.setSaveFile(null, name, false);
        rd.initIncompleteFile(incompleteFile, name, size);
        return rd;
    }
    
    public StoreDownloader createStoreDownloader(RemoteFileDesc rfd, 
            File saveDirectory, String fileName, boolean overwrite) throws SaveLocationException {
        StoreDownloader sd = storeDownloaderFactory.get();
        sd.addInitialSources(Collections.singletonList(rfd), fileName);
        sd.setSaveFile(saveDirectory, fileName, overwrite);
        return sd;
    }

    public BTDownloader createBTDownloader(BTMetaInfo info) {
        BTDownloader bd = btDownloaderFactory.get();
        bd.initBtMetaInfo(info);
        return bd;
    }
    
    public CoreDownloader createFromMemento(DownloadMemento memento) throws InvalidDataException {
        try {
            switch(memento.getDownloadType()) {
            case BTDOWNLOADER:
                return btFromMemento((BTDownloadMemento)memento);
            case INNETWORK:
                return inNetworkFromMemento((InNetworkMemento)memento);
            case MAGNET:
                return magnetFromMemento((GnutellaDownloadMemento)memento);
            case MANAGED:
                return managedFromMemento((GnutellaDownloadMemento)memento);
            case STORE:
                return storeFromMemento((GnutellaDownloadMemento)memento);
            case TORRENTFETCHER:
            default:
                throw new InvalidDataException("invalid memento type: " + memento.getDownloadType());
            }
        } catch(ClassCastException cce) {
            throw new InvalidDataException("invalid memento!", cce);
        }
    }

    private StoreDownloader storeFromMemento(GnutellaDownloadMemento memento) throws InvalidDataException {
        StoreDownloader sd = storeDownloaderFactory.get();
        sd.addInitialSources(toRfds(memento.getRemoteHosts()), memento.getDefaultFileName());
        sd.addNewProperties(memento.getProperties());
        return sd;
    }

    private ManagedDownloader managedFromMemento(GnutellaDownloadMemento memento) throws InvalidDataException  {
        ManagedDownloader md = managedDownloaderFactory.get();
        md.addInitialSources(toRfds(memento.getRemoteHosts()), memento.getDefaultFileName());
        md.addNewProperties(memento.getProperties());
        return md;
    }

    private MagnetDownloader magnetFromMemento(GnutellaDownloadMemento memento) throws InvalidDataException  {
        MagnetDownloader md = magnetDownloaderFactory.get();
        md.addInitialSources(toRfds(memento.getRemoteHosts()), memento.getDefaultFileName());
        md.addNewProperties(memento.getProperties());
        return md;
    }

    private InNetworkDownloader inNetworkFromMemento(final InNetworkMemento memento) throws InvalidDataException  {
        InNetworkDownloader id = inNetworkDownloaderFactory.get();
        id.addInitialSources(toRfds(memento.getRemoteHosts()), memento.getDefaultFileName());
        id.addNewProperties(memento.getProperties());
        id.initDownloadInformation(new DownloadInformation() {
            public long getSize() {
                return memento.getSize();
            }
            public String getTTRoot() {
                return memento.getTigerTreeRoot();
            }
            public String getUpdateCommand() {
                throw new UnsupportedOperationException();
            }
            public String getUpdateFileName() {
                throw new UnsupportedOperationException();
            }
            public URN getUpdateURN() {
                return memento.getUrn();
            }
            
        }, memento.getStartTime());
        return id;
    }

    private BTDownloader btFromMemento(BTDownloadMemento memento) throws InvalidDataException  {
        BTDownloader bd = btDownloaderFactory.get();
        bd.addNewProperties(memento.getPropertiesMap());
        return bd;
    }
    
    private Collection<RemoteFileDesc> toRfds(Collection<? extends RemoteHostMemento> mementos) throws InvalidDataException {
        if(mementos == null)
            return Collections.emptyList();
        
        List<RemoteFileDesc> rfds = new ArrayList<RemoteFileDesc>(mementos.size());
        for(RemoteHostMemento memento : mementos) {
            rfds.add(remoteFileDescFactory.createFromMemento(memento));
        }
        return rfds;
    }
    
}
