package com.limegroup.gnutella;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.limewire.collection.IntSet;
import org.limewire.inspection.Inspectable;
import org.limewire.util.ByteUtils;
import org.limewire.util.FileUtils;

public class FileListImpl implements FileList, Inspectable {


    /** 
     * The list of complete and incomplete files.  An entry is null if it
     *  is no longer shared.
     * INVARIANT: for all i, files[i]==null, or files[i].index==i and either
     *  files[i]._path is in a shared directory with a shareable extension or
     *  files[i]._path is the incomplete directory if files[i] is an IncompleteFileDesc.
     */
    protected List<FileDesc> files;
    
    /**
     * The total size of all complete files, in bytes.
     * INVARIANT: filesSize=sum of all size of the elements of files,
     *   except IncompleteFileDescs, whose size may change at any time.
     */
    protected long numBytes;
    
    /**
     * The number of complete files.
     * INVARIANT: numFiles==number of elements of files that are not null
     *  and not IncompleteFileDescs.
     */
    protected int numFiles;
    
    /**
     * An index that maps a <tt>File</tt> on disk to the 
     *  <tt>FileDesc</tt> holding it.
     *
     * INVARIANT: For all keys k in _fileToFileDescMap, 
     *  files[_fileToFileDescMap.get(k).getIndex()].getFile().equals(k)
     *
     * Keys must be canonical <tt>File</tt> instances.
     */
    protected Map<File, FileDesc> fileToFileDescMap;
    
    /**
     * A map of appropriately case-normalized URN strings to the
     * indices in files.  Used to make query-by-hash faster.
     * 
     * INVARIANT: for all keys k in urnMap, for all i in urnMap.get(k),
     * files[i].containsUrn(k).  Likewise for all i, for all k in
     *files[i].getUrns(), rnMap.get(k) contains i.
     */
    protected Map<URN, IntSet> urnMap;
    
    public FileListImpl() {
        resetVariables();
    }
    
    public void resetVariables() {
        files = new ArrayList<FileDesc>();
        numBytes = 0;
        numFiles = 0;
        urnMap = new HashMap<URN, IntSet>();
        fileToFileDescMap = new HashMap<File, FileDesc>();
    }
    
    public void addFile(File file, FileDesc fileDesc) { 
        files.add(fileDesc);
        fileToFileDescMap.put(file, fileDesc);
        numBytes += file.length();
        numFiles += 1;
        
        updateUrnIndex(fileDesc);
    }

    public FileDesc get(int i) {
        return files.get(i);
    }

    public List<FileDesc> getAllFileDescs() { 
        return new ArrayList<FileDesc>(fileToFileDescMap.values());
    }

    public FileDesc getFileDesc(File file) {
        return fileToFileDescMap.get(file);
    }

    public FileDesc getFileDesc(URN urn) {
        if (!urn.isSHA1())
          throw new IllegalArgumentException();
        return getFileDescForURN(urn);
    }
    
    private FileDesc getFileDescForURN(URN urn) {
        IntSet indices = urnMap.get(urn);
        if(indices == null) return null;
    
        IntSet.IntSetIterator iter = indices.iterator();
        
        //Pick the first non-null non-Incomplete FileDesc.
        FileDesc ret = null;
        while ( iter.hasNext() 
                   && ( ret == null || ret instanceof IncompleteFileDesc) ) {
            int index = iter.next();
            ret = files.get(index);
        }
        return ret;
    }

    public int getNumBytes() {
        return ByteUtils.long2int(numBytes);
    }
    
    public int getNumFiles() {
        return numFiles;
    }
    
    public int getListLength() {
        return files.size();
    }

    public void remove(File file) {
        FileDesc fd = fileToFileDescMap.get(file);
        if( fd != null )
            remove(fd);
    }
    
    public void remove(FileDesc fileDesc) {
        int index = fileDesc.getIndex();
        assert files.get(index).getFile().equals(fileDesc.getFile()) : "invariant broken!";
        files.set(index, null);
        fileToFileDescMap.remove(fileDesc.getFile());
        numFiles -= 1;
        numBytes -= fileDesc.getFileSize();
    }
    
    public void remove(URN urn) {
        urnMap.remove(urn);
    }

    public IntSet getIndicesForUrn(URN urn) {
        return urnMap.get(urn);
    }

    public boolean isValidSharedIndex(int i) {
        return (i >= 0 && i < files.size());
    }

    public boolean contains(File file) {
        return fileToFileDescMap.containsKey(file);
    }

    public boolean contains(FileDesc fileDesc) {
        return files.contains(fileDesc);
    }
    
    public boolean contains(URN urn) {
        return urnMap.containsKey(urn);
    }
    
    /**
     * Returns a list of all shared file descriptors in the given directory,
     * in any order.
     * 
     * Returns null if directory is not shared, or a zero-length array if it is
     * shared but contains no files.  This method is not recursive; files in 
     * any of the directory's children are not returned.
     * 
     * This operation is <b>not</b> efficient, and should not be done often.
     */  
    public List<FileDesc> getFilesInDirectory(File directory) {
        if (directory == null)
            throw new NullPointerException("null directory");
        
        // a. Remove case, trailing separators, etc.
        try {
            directory = FileUtils.getCanonicalFile(directory);
        } catch (IOException e) { // invalid directory ?
            return Collections.emptyList();
        }

        List<FileDesc> shared = new ArrayList<FileDesc>();

        for(FileDesc fd : files) {//sharedFileList.getAllFileDescs()) {
            if( fd == null)
                continue;
            if(directory.equals(fd.getFile().getParentFile()))
                shared.add(fd);
        }
        
        return shared;
    }
    
    /**
     * Generic method for adding a fileDesc's URNS to a map
     */
    public void updateUrnIndex(FileDesc fileDesc) {
        for(URN urn : fileDesc.getUrns()) {
            if (!urn.isSHA1())
                continue;
            IntSet indices= urnMap.get(urn);
            if (indices==null) {
                indices=new IntSet();
                urnMap.put(urn, indices);
            }
            indices.add(fileDesc.getIndex());
        }
    }

    public Object inspect() {
        Map<String,Object> inspections = new HashMap<String,Object>();
        inspections.put("size of files", Long.valueOf(numBytes));
        inspections.put("num of files", Integer.valueOf(numFiles));
        return inspections;
    }

    public void addIncompleteFile(File incompleteFile, IncompleteFileDesc incompleteFileDesc) {
    }

    public int getNumForcedFiles() {
        return 0;
    }

    public int getNumIncompleteFiles() {
        return 0;
    }
}
