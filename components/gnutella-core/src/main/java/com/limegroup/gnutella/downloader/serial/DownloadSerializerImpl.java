package com.limegroup.gnutella.downloader.serial;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.IOUtils;
import org.limewire.io.InvalidDataException;
import org.limewire.util.GenericsUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.downloader.CoreDownloader;
import com.limegroup.gnutella.downloader.CoreDownloaderFactory;

@Singleton
public class DownloadSerializerImpl implements DownloadSerializer {
    
    private static final Log LOG = LogFactory.getLog(DownloadSerializerImpl.class);
    
    private final DownloadSerializeSettings downloadSerializeSettings;
    private final CoreDownloaderFactory coreDownloaderFactory;
    
    @Inject
    public DownloadSerializerImpl(CoreDownloaderFactory coreDownloaderFactory,
            DownloadSerializeSettings downloadSerializeSettings) {
        this.downloadSerializeSettings = downloadSerializeSettings;
        this.coreDownloaderFactory = coreDownloaderFactory;
    }
    
    public List<SavedDownloadInfo> readFromDisk() {
        List<DownloadMemento> mementos = readMementos();
        List<SavedDownloadInfo> replies = convertMementosToDownloads(mementos);
        return replies;
    }
    
    
    public void writeToDisk(List<? extends SavedDownloadInfo> downloadsList) {
        // TODO Auto-generated method stub
        
    }
    
    List<SavedDownloadInfo> convertMementosToDownloads(List<? extends DownloadMemento> mementos) {
        List<SavedDownloadInfo> savedDownloads = new ArrayList<SavedDownloadInfo>(mementos.size());
        for(DownloadMemento memento : mementos) {
            CoreDownloader coreDownloader;
            try {
                coreDownloader = coreDownloaderFactory.createFromMemento(memento);
                savedDownloads.add(new SavedDownloadInfo(coreDownloader, memento.getRanges(), memento.getIncompleteFile()));
            } catch (InvalidDataException e) {
                LOG.warn("Unable to create downloader from memento: " + memento, e);
            }
        }
        return savedDownloads;
    }
    
    /** Reads all mementos from disk. */
    private List<DownloadMemento> readMementos() {
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(downloadSerializeSettings.getSaveFile())));
            return GenericsUtils.scanForList(in.readObject(), DownloadMemento.class, GenericsUtils.ScanMode.REMOVE);
        } catch(Throwable ignored) {
            LOG.warn("Error reading normal file.", ignored);
        } finally {
            IOUtils.close(in);
        }
        
        try {
            in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(downloadSerializeSettings.getBackupFile())));
            return GenericsUtils.scanForList(in.readObject(), DownloadMemento.class, GenericsUtils.ScanMode.REMOVE);
        } catch(Throwable ignored) {
            LOG.warn("Error reading normal file.", ignored);
        } finally {
            IOUtils.close(in);
        }
        
        return Collections.emptyList();        
    }

    /*
     * 
    private synchronized void copyBackupToReal() {
        File real = SharingSettings.OLD_DOWNLOAD_SNAPSHOT_FILE.getValue();
        File backup = SharingSettings.OLD_DOWNLOAD_SNAPSHOT_BACKUP_FILE.getValue();        
        real.delete();
        FileUtils.copy(backup, real);
    }
    

    
    


    public boolean writeSnapshot() {
        List<AbstractDownloader> buf;
        synchronized(this) {
            buf = new ArrayList<AbstractDownloader>(active.size() + waiting.size());
            buf.addAll(active);
            buf.addAll(waiting);
        }
        
        File outFile = SharingSettings.OLD_DOWNLOAD_SNAPSHOT_FILE.getValue();
        File backupFile = SharingSettings.OLD_DOWNLOAD_SNAPSHOT_BACKUP_FILE.getValue();
        
        //must delete in order for renameTo to work.
        backupFile.delete();
        outFile.renameTo(backupFile);
        
        // Write list of active and waiting downloaders, then block list in
        //   IncompleteFileManager.
        ObjectOutputStream out = null;
        try {
            out=new ObjectOutputStream(
                    new BufferedOutputStream(
                        new FileOutputStream(outFile)));
            
            out.writeObject(buf);
            //Blocks can be written to incompleteFileManager from other threads
            //while this downloader is being serialized, so lock is needed.
            synchronized (incompleteFileManager) {
                out.writeObject(incompleteFileManager);
            }
            out.flush();
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            IOUtils.close(out);
        }
    }

    public synchronized boolean readAndInitializeSnapshot(File file) {
        List<AbstractDownloader> buf;
        try {
            buf = readSnapshot(file);
        } catch(IOException iox) {
            LOG.warn("Couldn't read snapshot", iox);
            return false;
        }

        //Initialize and start downloaders. This code is a little tricky.  It is
        //important that instruction (3) follow (1) and (2), because we must not
        //pass an uninitialized Downloader to the GUI.  (The call to getFileName
        //will throw NullPointerException.)  I believe the relative order of (1)
        //and (2) does not matter since this' monitor is held.  (The download
        //thread must obtain the monitor to acquire a queue slot.)
        try {
            for (AbstractDownloader downloader : buf) {
                
                waiting.add(downloader);
                downloader.initialize();
                callback(downloader).addDownload(downloader);
            }
            return true;
        } finally {
            // Remove entries that are too old or no longer existent and not actively 
            // downloaded.  
            if (incompleteFileManager.initialPurge(getActiveDownloadFiles(buf)))
                writeSnapshot();
        }
    }
    
    private List<AbstractDownloader> readSnapshot(File file) throws IOException {        
        //Read downloaders from disk.
        List<AbstractDownloader> buf=null;
        ObjectInputStream in = null;
        try {
            in = new ConverterObjectInputStream(
                                    new BufferedInputStream(
                                        new FileInputStream(file)));
            //This does not try to maintain backwards compatibility with older
            //versions of LimeWire, which only wrote the list of downloaders.
            //Note that there is a minor race condition here; if the user has
            //started some downloads before this method is called, the new and
            //old downloads will use different IncompleteFileManager instances.
            //This doesn't really cause an errors, however.
            buf = GenericsUtils.scanForList(in.readObject(), AbstractDownloader.class, ScanMode.REMOVE);
            incompleteFileManager=(IncompleteFileManager)in.readObject();
        } catch(Throwable t) {
            LOG.error("Unable to read download file", t);
            throw (IOException)new IOException().initCause(t);
        } finally {
            IOUtils.close(in);
        }
        
        // Pump the downloaders through a set, to remove duplicate values.
        // This is necessary in case LimeWire got into a state where a
        // downloader was written to disk twice.
        return new LinkedList<AbstractDownloader>(new LinkedHashSet<AbstractDownloader>(buf));
    }
    
    private void read() {

        
        File real = SharingSettings.OLD_DOWNLOAD_SNAPSHOT_FILE.getValue();
        File backup = SharingSettings.OLD_DOWNLOAD_SNAPSHOT_BACKUP_FILE.getValue();
        // Try once with the real file, then with the backup file.
        if( !readAndInitializeSnapshot(real) ) {
            LOG.debug("Reading real downloads.dat failed");
            // if backup succeeded, copy into real.
            if( readAndInitializeSnapshot(backup) ) {
                LOG.debug("Reading backup downloads.bak succeeded.");
                copyBackupToReal();
            // only show the error if the files existed but couldn't be read.
            } else if(backup.exists() || real.exists()) {
                LOG.debug("Reading both downloads files failed.");
                MessageService.showError(I18nMarker.marktr("Sorry, but LimeWire was unable to restart your old downloads."));
            }   
        } else {
            LOG.debug("Reading downloads.dat worked!");
        }
    }
    
    private void write() {

        // If the write failed, move the backup to the real.
        if(!writeSnapshot())
            copyBackupToReal();
    }
*/
}
