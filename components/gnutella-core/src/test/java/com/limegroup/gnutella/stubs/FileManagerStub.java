package com.limegroup.gnutella.stubs;

import java.io.File;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.messages.QueryRequest;
import com.sun.java.util.collections.*;

/**
 * A simple FileManager that shares one file of (near) infinite length.
 */
public class FileManagerStub extends FileManager {

	Map _urns;
	List _descs;
    FileDescStub fdStub = new FileDescStub();

    public FileDesc get(int i) {
    	if (i < _descs.size())
    		return (FileDesc)_descs.get(i);
        return fdStub;
    }
    
    public boolean isValidIndex(int i) {
        return true;
    }
    
    public FileDesc getFileDescForUrn(URN urn) {
    	

        if(urn.toString().equals(FileDescStub.urn))
            return fdStub;
        else if (_urns.containsKey(urn))
        	return (FileDesc)_urns.get(urn);
        else
            return new FileDescStub("other.txt");
    }
    
    public boolean shouldIncludeXMLInResponse(QueryRequest qr) {
        return false;
    }
    
    public void addXMLToResponse(Response r, FileDesc fd) {
        ;
    }
    
    public FileManagerStub(Map urns,List descs) {
    	super();
    	_urns = urns;
    	_descs = descs;
    }
    
    public FileManagerStub(){
    	super();
    	_urns = new HashMap();
    	_descs = new Vector();
    }
    
    public FileDesc addFileIfShared(File f, List docs) {
        return addFileIfShared(f);
    }
}

