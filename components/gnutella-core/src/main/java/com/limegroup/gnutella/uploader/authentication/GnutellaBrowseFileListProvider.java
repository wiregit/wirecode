package com.limegroup.gnutella.uploader.authentication;

import java.io.IOException;
import java.util.Collections;

import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpContext;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.library.FileList;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.uploader.HttpException;

@Singleton
public class GnutellaBrowseFileListProvider implements HttpRequestFileListProvider {

    private final FileManager fileManager;

    @Inject
    public GnutellaBrowseFileListProvider(FileManager fileManager) {
        this.fileManager = fileManager;
    }
    
    @Override
    public Iterable<FileList> getFileList(HttpRequest request, HttpContext httpContext)
            throws HttpException, IOException, org.apache.http.HttpException {
        return Collections.singletonList(fileManager.getGnutellaSharedFileList());
    }

}
