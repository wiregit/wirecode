package com.limegroup.gnutella.uploader.authentication;

import java.util.Collections;

import org.apache.http.protocol.HttpContext;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.library.FileView;
import com.limegroup.gnutella.library.FileViewManager;

/**
 * Returns the file list for a public Gnutella browse of the client's shared
 * files.
 */
@Singleton
public class GnutellaBrowseFileViewProvider implements HttpRequestFileViewProvider {

    private final FileViewManager fileViewManager;

    @Inject
    public GnutellaBrowseFileViewProvider(FileViewManager fileManager) {
        this.fileViewManager = fileManager;
    }
    
    /**
     * @return {@link FileManager#getGnutellaCollection()}.
     */
    @Override
    public Iterable<FileView> getFileViews(String userID, HttpContext httpContext) {
        // Broken into two lines so it returns the correct type.
        FileView fileList = fileViewManager.getGnutellaFileView();
        return Collections.singletonList(fileList);
    }

}