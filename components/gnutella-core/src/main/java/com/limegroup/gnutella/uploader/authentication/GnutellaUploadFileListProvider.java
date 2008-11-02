package com.limegroup.gnutella.uploader.authentication;

import java.util.Arrays;

import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpContext;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.library.SharedFileList;

/**
 * Returns the file lists for public Gnutella uploads.
 */
@Singleton
public class GnutellaUploadFileListProvider implements HttpRequestFileListProvider {

    private final FileManager fileManager;

    @Inject
    public GnutellaUploadFileListProvider(FileManager fileManager) {
        this.fileManager = fileManager;
    }

    /**
     * @return {@link FileManager#getGnutellaSharedFileList()} and {@link FileManager#getIncompleteFileList()}
     * to enable sharing of incomplete files
     */
    @Override
    public Iterable<SharedFileList> getFileLists(HttpRequest request, HttpContext httpContext) {
        return Arrays.asList(fileManager.getGnutellaSharedFileList(), fileManager.getIncompleteFileList());
    }
}
