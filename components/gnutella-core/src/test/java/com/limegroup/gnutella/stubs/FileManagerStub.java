package com.limegroup.gnutella.stubs;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManagerController;
import com.limegroup.gnutella.FileManagerImpl;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * A simple FileManager that shares one file of (near) infinite length.
 */
@SuppressWarnings("unchecked")
@Singleton
public class FileManagerStub extends FileManagerImpl {

	private Map _urns,_files;
    private List _descs;
    private FileDescStub fdStub = new FileDescStub();
    
    @Inject
    public FileManagerStub(FileManagerController fileManagerController) {
        super(fileManagerController);
    }
    
    public final static URN NOT_HAVE;
    
    static {
    	try {
    	    NOT_HAVE = URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZZZZZZZZZZ");
    	} catch(IOException ignored){
    	    throw new RuntimeException(ignored);    
        }
    }
    
    private List removeRequests = new LinkedList();

    @Override
    public FileDesc get(int i) {
    	if (i < _descs.size())
    		return (FileDesc)_descs.get(i);
        return fdStub;
    }
    
    @Override
    public boolean isValidSharedIndex(int i) {
        return true;
    }
    
    public void addFileDescForUrn(FileDesc fd, URN urn) {
        if(_urns == null)
            _urns = new HashMap();
        _urns.put(urn, fd);
    }
    
    @Override
    public FileDesc getFileDescForUrn(URN urn) {
        if(urn.toString().equals(FileDescStub.DEFAULT_URN))
            return fdStub;
        else if (urn.equals(NOT_HAVE))
        	return null;
        else if (_urns.containsKey(urn))
        	return (FileDesc)_urns.get(urn);
        else
            return new FileDescStub("other.txt");
    }
    
    @Override
    public synchronized FileDesc getSharedFileDescForUrn(final URN urn) {
        if(urn.toString().equals(FileDescStub.DEFAULT_URN))
            return fdStub;
        else if (urn.equals(NOT_HAVE))
            return null;
        else if (_urns.containsKey(urn))
            return (FileDesc)_urns.get(urn);
        else
            return new FileDescStub("other.txt");
    }
    
    @Override
    public boolean shouldIncludeXMLInResponse(QueryRequest qr) {
        return false;
    }
    
    @Override
    public void addXMLToResponse(Response r, FileDesc fd) {
        ;
    }
    
    @Override
    public boolean isValidXMLMatch(Response r, LimeXMLDocument doc) {
        return true;
    }
    
    public void setUrns(Map urns) {
        this._urns = urns;
    }
    
    public void setDescs(List descs) {
        this._descs = descs;
    }
    
    public void setFiles(Map m) {
    	_files = m;
    }
    
    @Override
    public FileDesc getFileDescForFile(File f) {
        if (_files==null)
            return fdStub;
    	return (FileDesc)_files.get(f);
    }
    
    @Override
    public void fileChanged(File f) {
        throw new UnsupportedOperationException();
    }
    
    public List getRemoveRequests() {
        return removeRequests;
    }
    
    @Override
    public synchronized FileDesc removeFileIfSharedOrStore(File f) {
        removeRequests.add(f);
        return super.removeFileIfSharedOrStore(f);
    }
    
    @Override
    protected synchronized FileDesc removeFileIfSharedOrStore(File f, boolean notify) {
        removeRequests.add(f);
        return super.removeFileIfSharedOrStore(f, notify);
    }

    @Override
    protected synchronized FileDesc removeFileIfShared(File f, boolean notify) {
        removeRequests.add(f);
        return super.removeFileIfShared(f, notify);
    }
}

