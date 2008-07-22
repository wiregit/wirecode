package com.limegroup.gnutella;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.limewire.collection.MultiCollection;
import org.limewire.inspection.InspectableForSize;

import com.limegroup.gnutella.library.SharingUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;

public class SharedFileListImpl extends FileListImpl {
    
    /**
     * The number of files that are forcibly shared over the network.
     * INVARIANT: numFiles >= numForcedFiles.
     */
    private int numForcedFiles;
    
    private final Set<File> filesNotToShare;
    
    /**
     * Files that are shared only for this LW session.
     * INVARIANT: no file can be in this and _data.SPECIAL_FILES_TO_SHARE
     * at the same time
     */
    @InspectableForSize("number of transiently shared files")
    private final Set<File> transientSharedFiles;
    
    /**
     * Individual files that are not in a shared folder.
     */
    @InspectableForSize("number of individually shared files") 
    private Collection<File> individualSharedFiles; 
    
    public SharedFileListImpl(String name, FileManager fileManager, Set<File> individualFiles, Set<File> filesNotToShare) {
        super(name, fileManager, individualFiles);
        
        this.filesNotToShare = filesNotToShare;
        transientSharedFiles = new HashSet<File>();
        
        // the transient files and the special files.
        individualSharedFiles = Collections.synchronizedCollection(
                new MultiCollection<File>(transientSharedFiles, individualFiles));
    }
    
    @Override
    public void addFileAlways(File file) {
        addFileAlways(file, LimeXMLDocument.EMPTY_LIST);
    }
    
    /**
     * Always adds this file, regardless if it has a failing properties to make
     * it shareable
     */
    @Override
    public void addFileAlways(File file, List<? extends LimeXMLDocument> list) {
        filesNotToShare.remove(file); 
        if (!isFileAddable(file))
            individualFiles.add(file);
            
        addFile(file, list);
    }
    
    /**
     * Adds this file for the session only
     */
    @Override
    public void addFileForSession(File file) {
        filesNotToShare.remove(file);
        if (!isFileAddable(file))
            transientSharedFiles.add(file); 
        addFile(file);
    }
    
    @Override
    public void addFileDesc(FileDesc fileDesc) {
        super.addFileDesc(fileDesc);
        
        //Register this file with its parent directory.
        File parent = fileDesc.getFile().getParentFile();
        assert parent != null : "Null parent to \""+fileDesc.getFile()+"\"";        
        // files that are forcibly shared over the network
        // aren't counted or shown.
        if(SharingUtils.isForcedShareDirectory(parent))
            numForcedFiles++;
    }
    
    @Override
    public boolean remove(FileDesc fd) {      
        if(!individualSharedFiles.contains(fd.getFile()) && fileManager.isFileInCompletelySharedDirectory(fd.getFile()))
            filesNotToShare.add(fd.getFile());
        
        boolean value = super.remove(fd);

        if(value) {
            File parent = fd.getFile().getParentFile();
            // files that are forcibly shared over the network aren't counted
            if(SharingUtils.isForcedShareDirectory(parent)) {
                numForcedFiles--;
            }
        }
        return value;
    }
    
    @Override
    protected void updateFileDescs(FileDesc oldFileDesc, FileDesc newFileDesc) {     
        if (super.remove(oldFileDesc)) { 
            super.addFileDesc(newFileDesc); }
    }

    //TODO: iterator should ignore specially shared files
    
    @Override
    public int size() {
        return fileDescs.size() - numForcedFiles;
    }
    
    @Override
    public int getNumForcedFiles() {
        return numForcedFiles;
    }
    
    @Override
    public void clear() {
        super.clear();
        if(transientSharedFiles != null)
        transientSharedFiles.clear();
        numForcedFiles = 0;
    }
    
    @Override
    public boolean isFileAddable(File file) {
        if (!SharingUtils.isFilePhysicallyShareable(file))
            return false;
        if (individualSharedFiles.contains(file))
            return true;
        if (filesNotToShare.contains(file))
            return false;
        if (fileManager.isFileInCompletelySharedDirectory(file)) {
            if (file.getName().toUpperCase(Locale.US).startsWith("LIMEWIRE"))
                return true;
            if (!FileManagerImpl.hasShareableExtension(file))
                return false;
            return true;
        }
        return false;
    }
    
    @Override
    public Object inspect() {
        Map<String,Object> inspections = new HashMap<String,Object>();
        inspections.put("size of files", Long.valueOf(numBytes));
        inspections.put("num of files", Integer.valueOf(fileDescs.size()));
        inspections.put("num forced shared files", Integer.valueOf(numForcedFiles));
        
        return inspections;
    }
    
    @Override
    protected void addAsIndividualFile(FileDesc fileDesc) { 
//        if(LimeWireUtils.getMajorVersionNumber() >= 5) {
//            individualFiles.add(fileDesc.getFile());
//        }else {
            if(!fileManager.isFileInCompletelySharedDirectory(fileDesc.getFile()) && !transientSharedFiles.contains(fileDesc.getFile())) {
                individualFiles.add(fileDesc.getFile());
            }
//        }
    }
    
    /**
     * Returns true if this list is allowed to add this FileDesc
     * @param fileDesc - FileDesc to be added
     */
    @Override
    protected boolean isFileAddable(FileDesc fileDesc) {
        if( fileDesc.getLimeXMLDocuments().size() != 0 && 
                isStoreXML(fileDesc.getLimeXMLDocuments().get(0))) {
            return false;
        } else if( !isFileAddable(fileDesc.getFile())){
            return false;
        } 
        return true;
    }
    
    //////////////////////// For Backwards Compatibility /////////////////
    
    @Override
    public File[] getIndividualFiles() { 
        ArrayList<File> files = new ArrayList<File>(individualSharedFiles.size());
        for(File f : individualSharedFiles) {
            if (f.exists())
                files.add(f);
        }
          
        if (files.isEmpty())
            return new File[0];
        else
            return files.toArray(new File[files.size()]);
    }

    @Override
    public int getNumIndividualFiles() {
        return individualSharedFiles.size();
    }

    @Override
    public boolean hasIndividualFiles() {
        return !individualSharedFiles.isEmpty();
    }

    @Override
    public boolean isIndividualFile(File file) {
        return individualFiles.contains(file) && 
                SharingUtils.isFilePhysicallyShareable(file)&& 
                !SharingUtils.isApplicationSpecialShare(file);
    }
}
