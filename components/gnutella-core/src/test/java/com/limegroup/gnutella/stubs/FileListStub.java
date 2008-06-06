package com.limegroup.gnutella.stubs;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.limewire.collection.IntSet;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.SharedFileListImpl;
import com.limegroup.gnutella.URN;

public class FileListStub extends SharedFileListImpl {

    private Map<File, FileDesc> files = new HashMap<File,FileDesc>();
    
    private Map<URN,FileDesc> urnMap = new HashMap<URN,FileDesc>();
    
//    private List<FileDesc> fd = new ArrayList<FileDesc>();
    
    
    private FileDescStub fdStub = new FileDescStub();
    
    public final static URN NOT_HAVE;
        
    static {
        try {
            NOT_HAVE = URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZZZZZZZZZZ");
        } catch(IOException ignored){
            throw new RuntimeException(ignored);    
        }
    }
    
    @Override
    public void addFile(File file, FileDesc fileDesc) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean contains(File file) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean contains(FileDesc fileDesc) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean contains(URN urn) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public FileDesc get(int i) {
        if( i < files.size())
            return files.get(i);
        return fdStub;
    }

    @Override
    public List<FileDesc> getAllFileDescs() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public FileDesc getFileDesc(URN urn) {
      if(urn.toString().equals(FileDescStub.DEFAULT_URN))
          return fdStub;
      else if (urn.equals(NOT_HAVE))
          return null;
      else if (urnMap.containsKey(urn))
          return urnMap.get(urn);
      else
          return new FileDescStub("other.txt");
    }

    @Override
    public FileDesc getFileDesc(File file) {
        if (files==null)
            return fdStub;
        return files.get(file);
    }

    @Override
    public IntSet getIndicesForUrn(URN urn) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getNumBytes() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getNumFiles() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean isValidSharedIndex(int i) {
        return true;
    }

    @Override
    public void remove(File file) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void remove(FileDesc fileDesc) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void remove(URN urn) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void resetVariables() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int getListLength() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void updateUrnIndex(FileDesc fileDesc) {
        // TODO Auto-generated method stub
        
    }
  
    //New Accessors
    
    public void setUrns(Map<URN,FileDesc> urns) {
        this.urnMap = urns;
    }

    public void setDescs(List<FileDesc> descs) {
//        this.fd = descs;
    }
    
    public void setFiles(Map<File, FileDesc> fileMap) {
        this.files = fileMap;
    }
    
    public void addFileDescForUrn(FileDesc fd, URN urn) {
        if(urnMap == null)
            urnMap = new HashMap<URN,FileDesc>();
        urnMap.put(urn, fd);
    }
}
