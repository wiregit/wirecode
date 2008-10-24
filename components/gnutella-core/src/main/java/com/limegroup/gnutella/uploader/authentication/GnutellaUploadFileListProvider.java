package com.limegroup.gnutella.uploader.authentication;

import java.io.IOException;
import java.util.Arrays;

import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpContext;

import com.limegroup.gnutella.library.FileList;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.uploader.HttpException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class GnutellaUploadFileListProvider implements HttpRequestFileListProvider {

    private final FileManager fileManager;

    @Inject
    public GnutellaUploadFileListProvider(FileManager fileManager) {
        this.fileManager = fileManager;
    }

    @Override
    public Iterable<FileList> getFileList(HttpRequest request, HttpContext httpContext)
            throws HttpException, IOException, org.apache.http.HttpException {
        return Arrays.asList(fileManager.getGnutellaSharedFileList(), fileManager.getIncompleteFileList());
    }
}
