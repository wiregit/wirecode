package com.limegroup.gnutella.uploader.authentication;

import java.util.Arrays;

import org.apache.http.protocol.HttpContext;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.library.FileView;

/**
 * Returns the file lists for public Gnutella uploads.
 */
@Singleton
public class GnutellaUploadFileViewProvider implements HttpRequestFileViewProvider {

    private final FileManager fileManager;

    @Inject
    public GnutellaUploadFileViewProvider(FileManager fileManager) {
        this.fileManager = fileManager;
    }

    /**
     * @return {@link FileManager#getGnutellaFileView()} and {@link FileManager#getIncompleteFileCollection()}
     * to enable sharing of incomplete files
     */
    @Override
    public Iterable<FileView> getFileViews(String userID, HttpContext httpContext) {
        return Arrays.asList(fileManager.getGnutellaFileView(), fileManager.getIncompleteFileCollection());
    }
}