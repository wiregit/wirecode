package com.limegroup.gnutella.uploader.authentication;

import java.io.IOException;

import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpContext;

import com.google.inject.Inject;
import com.limegroup.gnutella.FileList;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.uploader.HttpException;

public class GnutellaBrowseRequestFileListProvider implements HttpRequestFileListProvider {

    private final FileManager fileManager;

    @Inject
    public GnutellaBrowseRequestFileListProvider(FileManager fileManager) {
        this.fileManager = fileManager;
    }

    @Override
    public FileList getFileList(HttpRequest request, HttpContext httpContext)
            throws HttpException, IOException, org.apache.http.HttpException {
        return fileManager.getSharedFileList();
    }
    
    
}
