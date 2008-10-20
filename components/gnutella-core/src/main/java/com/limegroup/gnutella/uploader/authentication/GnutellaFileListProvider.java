package com.limegroup.gnutella.uploader.authentication;

import java.io.IOException;

import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpContext;

import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.FileList;
import com.limegroup.gnutella.uploader.HttpException;
import com.google.inject.Inject;

public class GnutellaFileListProvider implements HttpRequestFileListProvider {

    private final FileManager fileManager;

    @Inject
    public GnutellaFileListProvider(FileManager fileManager) {
        this.fileManager = fileManager;
    }

    @Override
    public FileList getFileList(HttpRequest request, HttpContext httpContext)
            throws HttpException, IOException, org.apache.http.HttpException {
        return fileManager.getGnutellaSharedFileList();
    }
}
