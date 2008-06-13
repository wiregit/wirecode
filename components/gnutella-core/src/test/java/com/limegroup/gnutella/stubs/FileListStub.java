package com.limegroup.gnutella.stubs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.SharedFileListImpl;
import com.limegroup.gnutella.URN;

public class FileListStub extends SharedFileListImpl {

    private Map<URN,FileDesc> urnMap = new HashMap<URN,FileDesc>();
    
    private FileDescStub fdStub = new FileDescStub();
    
    public final static URN NOT_HAVE;
        
    static {
        try {
            NOT_HAVE = URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZZZZZZZZZZ");
        } catch(IOException ignored){
            throw new RuntimeException(ignored);    
        }
    }
    
    public FileListStub() {
		clear();
    }
    
    public void clear() {
        super.resetVariables();
        files = new ArrayList<FileDesc>();
        numBytes = 0;
        numFiles = 0;
        fileToFileDescMap = new HashMap<File, FileDesc>();
    }
    
    @Override
    public void resetVariables() {
    }

    @Override
    public FileDesc get(int i) { 
        if( i < files.size())
            return files.get(i);
        return fdStub;
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
        if(fileToFileDescMap.containsKey(file))
            return fileToFileDescMap.get(file);
        else
            return fdStub;
    }

    @Override
    public boolean isValidSharedIndex(int i) { 
        return true;
    }
    
    @Override
    public boolean contains(URN urn) {
        return urnMap.containsKey(urn);
    }
    
    @Override
    public void remove(URN urn) {
        urnMap.remove(urn);
    }
  
    //New Accessors
    
    public void setUrns(Map<URN,FileDesc> urns) {
        this.urnMap = urns;
    }

    public void setDescs(List<FileDesc> descs) {
        this.files = descs;
        for(FileDesc fd : descs ) {
            fileToFileDescMap.put(fd.getFile(), fd);
            numBytes += fd.getFile().length();
            numFiles += 1;
        }
    }
    
    public void setFiles(Map<File, FileDesc> fileMap) {
        this.fileToFileDescMap = fileMap;
        Set<File> set = fileMap.keySet();
        for(File f : set) {
            this.files.add(fileMap.get(f));
            numBytes += f.length();
            numFiles += 1;
        }
    }
    
    public void addFileDescForUrn(FileDesc fd, URN urn) {
        if(urnMap == null)
            urnMap = new HashMap<URN,FileDesc>();
        urnMap.put(urn, fd);
    }
}
