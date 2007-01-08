package com.limegroup.gnutella.stubs;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * A simple FileManager that shares one file of (near) infinite length.
 */
@SuppressWarnings("unchecked")
public class FileManagerStub extends FileManager {

	Map _urns,_files;
	List _descs;
    FileDescStub fdStub = new FileDescStub();
    public static URN _notHave =null;
    static {
    	try{
    	_notHave= URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZZZZZZZZZZ");
    	}catch(IOException ignored){}
    }
    List removeRequests = new LinkedList();

    public FileDesc get(int i) {
    	if (i < _descs.size())
    		return (FileDesc)_descs.get(i);
        return fdStub;
    }
    
    public boolean isValidIndex(int i) {
        return true;
    }
    
    public FileDesc getFileDescForUrn(URN urn) {
    	

        if(urn.toString().equals(FileDescStub.DEFAULT_URN))
            return fdStub;
        else if (urn.equals(_notHave))
        	return null;
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
    
    public boolean isValidXMLMatch(Response r, LimeXMLDocument doc) {
        return true;
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
    
    public void setFiles(Map m) {
    	_files = m;
    }
    
    public FileDesc getFileDescForFile(File f) {
        if (_files==null)
            return fdStub;
    	return (FileDesc)_files.get(f);
    }
    
    public void fileChanged(File f) {
        throw new UnsupportedOperationException();
    }
    
    public List getRemoveRequests() {
        return removeRequests;
    }

    protected synchronized FileDesc removeFileIfShared(File f, boolean notify) {
        removeRequests.add(f);
        return super.removeFileIfShared(f, notify);
    }
}

