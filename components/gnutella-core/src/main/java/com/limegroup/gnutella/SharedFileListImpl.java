package com.limegroup.gnutella;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.limewire.collection.IntSet;

import com.limegroup.gnutella.library.SharingUtils;

public class SharedFileListImpl extends FileListImpl {
    
    /**
     * The number of files that are forcibly shared over the network.
     * INVARIANT: numFiles >= numForcedFiles.
     */
    private int numForcedFiles;
    
    /**
     * The total number of incomplete files.
     * INVARIANT: numFiles + _numIncompleteFiles == the number of
     *  elements of files that are not null.
     */
    private int numIncompleteFiles;
    
    /**
     * The IntSet for incomplete shared files.
     * 
     * INVARIANT: for all i in _incompletesShared,
     *       files[i]._path == the incomplete directory.
     *       files[i] instanceof IncompleteFileDesc
     *  Likewise, for all i s.t.
     *    files[i] != null and files[i] instanceof IncompleteFileDesc,
     *       incompletesShared.contains(i)
     * 
     * This structure is not strictly needed for correctness, but it allows
     * others to retrieve all the incomplete shared files, which is
     * relatively useful.                                                                                                       
     */
    private IntSet incompletesShared;
    
    @Override
    public void resetVariables() {
        super.resetVariables();
        
        incompletesShared = new IntSet();
        numIncompleteFiles = 0;
        numForcedFiles = 0;   
    }
    
    @Override
    public void addFile(File file, FileDesc fileDesc) {
        super.addFile(file, fileDesc);
        
        //Register this file with its parent directory.
        File parent = file.getParentFile();
        assert parent != null : "Null parent to \""+file+"\"";        
        // files that are forcibly shared over the network
        // aren't counted or shown.
        if(SharingUtils.isForcedShareDirectory(parent))
            numForcedFiles++;
    }
    
    @Override
    public void addIncompleteFile(File incompleteFile, IncompleteFileDesc incompleteFileDesc) {
        files.add(incompleteFileDesc);
        fileToFileDescMap.put(incompleteFile, incompleteFileDesc);
        numIncompleteFiles += 1;
        incompletesShared.add(incompleteFileDesc.getIndex());
    }
    
    @Override
    public void remove(FileDesc fd) {
        super.remove(fd);
        
        if( fd instanceof IncompleteFileDesc) {
            numIncompleteFiles--;
            boolean removed = incompletesShared.remove(fd.getIndex());
            assert removed : "File "+fd.getIndex()+" not found in " + incompletesShared;
            return;
        }
        
        File parent = fd.getFile().getParentFile();
        // files that are forcibly shared over the network aren't counted
        if(SharingUtils.isForcedShareDirectory(parent)) {
            numForcedFiles--;
        }
    }
    
    /**
     * Returns the number of shared files. This number does 
     * NOT include the number of focibly shared files
     */
    @Override 
    public int getNumFiles() {
        return numFiles - numForcedFiles;
    }
    
    /**
     * Returns the number of forcibly shared files. This number
     * is NOT included in the number of shared files
     */
    @Override
    public int getNumForcedFiles() {
        return numForcedFiles;
    }
    
    /**
     * Returns the number of shared incomplete files.
     */
    @Override
    public int getNumIncompleteFiles() {
        return numIncompleteFiles;
    }
    
    @Override
    public Object inspect() {
        Map<String,Object> inspections = new HashMap<String,Object>();
        inspections.put("size of files", Long.valueOf(numBytes));
        inspections.put("num of files", Integer.valueOf(numFiles));
        inspections.put("num forced shared files", Integer.valueOf(numForcedFiles));
        inspections.put("num of incomplete files", Integer.valueOf(numIncompleteFiles));
        inspections.put("num of incomplete files shared", Integer.valueOf(incompletesShared.size()));
        
        return inspections;
    }
}
