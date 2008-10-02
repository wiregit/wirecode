package com.limegroup.gnutella.stubs;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;

import org.limewire.util.FileUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.CreationTimeCache;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileEventListener;
import com.limegroup.gnutella.FileList;
import com.limegroup.gnutella.FileManagerEvent;
import com.limegroup.gnutella.FileManagerImpl;
import com.limegroup.gnutella.IncompleteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnCache;
import com.limegroup.gnutella.FileManagerEvent.Type;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.auth.ContentManager;
import com.limegroup.gnutella.downloader.VerifyingFile;
import com.limegroup.gnutella.simpp.SimppManager;

/**
 * A simple FileManager that shares one file of (near) infinite length.
 */
@Singleton
public class FileManagerStub extends FileManagerImpl {

    private FileListStub fileListStub;
    
    private FileDescStub fdStub = new FileDescStub();

    private Map<URN,FileDesc> urns = new HashMap<URN,FileDesc>();
    
    
    public final static URN NOT_HAVE;
        
    static {
        try {
            NOT_HAVE = URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZZZZZZZZZZ");
        } catch(IOException ignored){
            throw new RuntimeException(ignored);    
        }
    }
    
    @Inject
    public FileManagerStub(Provider<SimppManager> simppManager,
            Provider<UrnCache> urnCache,
            Provider<CreationTimeCache> creationTimeCache,
            Provider<ContentManager> contentManager,
            Provider<AltLocManager> altLocManager,
            Provider<ActivityCallback> activityCallback,
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
            CopyOnWriteArrayList<FileEventListener> listeners) {
        super(simppManager, urnCache, creationTimeCache, contentManager, altLocManager, activityCallback, backgroundExecutor, listeners);
        
        fileListStub = new FileListStub(this, _data.SPECIAL_FILES_TO_SHARE, _data.FILES_NOT_TO_SHARE);
        
        super.resetVariables();
    }

    @Override
    protected void resetVariables()  {

    }
    
    @Override
    public FileList getSharedFileList() {
        return fileListStub;
    }
    
    public FileDesc get(int i) {
        if (i < files.size())
            return files.get(i);
        return fdStub;
    }
    
    @Override
    public FileDesc getFileDesc(URN urn) {
        if(urn.toString().equals(FileDescStub.DEFAULT_URN))
            return fdStub;
        else if (urn.equals(NOT_HAVE))
            return null;
        else if (urns.containsKey(urn))
            return urns.get(urn);
        else
            return new FileDescStub("other.txt");
    }
    
    @Override
    public FileDesc getFileDesc(File f) {
        if (files==null || !fileToFileDescMap.containsKey(f))
            return fdStub;
        return fileToFileDescMap.get(f);
    }
    
    @Override
    public synchronized void addIncompleteFile(File incompleteFile,
            Set<? extends URN> urns,
            String name,
            long size,
            VerifyingFile vf) {
        try {
            incompleteFile = FileUtils.getCanonicalFile(incompleteFile);
        } catch(IOException ioe) {
        //invalid file?... don't add incomplete file.
            return;
        }

        // no indices were found for any URN associated with this
        // IncompleteFileDesc... add it.
        int fileIndex = size();
        
        IncompleteFileDesc ifd = new IncompleteFileDesc(
        incompleteFile, urns, fileIndex, name, size, vf);
        getIncompleteFileList().add(ifd);
        fileURNSUpdated(ifd);
        
        files.add(ifd);
        fileToFileDescMap.put(incompleteFile, ifd);
        
        dispatchFileEvent(new FileManagerEvent(this, Type.ADD_FILE, ifd));
    }
        
    public void dispatchEvent(FileManagerEvent.Type type, FileDesc fileDesc) {
        dispatchFileEvent(new FileManagerEvent(this,type, fileDesc));
    }
    
    public void setUrns(Map<URN,FileDesc> urns) {
        this.urns = urns;
    }
    
    public void setFiles(Map<File,FileDesc> map) {
        fileToFileDescMap = map;
        numFiles += map.size();
    }
    
    public void setFileDesc(List<FileDesc> files) {
        this.files = files;
    }
}

