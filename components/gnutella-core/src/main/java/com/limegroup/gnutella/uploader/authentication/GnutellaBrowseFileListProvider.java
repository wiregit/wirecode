package com.limegroup.gnutella.uploader.authentication;

import java.util.Collections;

import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpContext;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.library.FileList;
import com.limegroup.gnutella.library.FileManager;

/**
 * Returns the file list for a public Gnutella browse of the client's shared
 * files.
 */
@Singleton
public class GnutellaBrowseFileListProvider implements HttpRequestFileListProvider {

    private final FileManager fileManager;

    @Inject
    public GnutellaBrowseFileListProvider(FileManager fileManager) {
        this.fileManager = fileManager;
    }
    
    /**
     * @return {@link FileManager#getGnutellaSharedFileList()}.
     */
    @Override
    public Iterable<FileList> getFileLists(HttpRequest request, HttpContext httpContext) {
        return Collections.singletonList((FileList)fileManager.getGnutellaSharedFileList());
    }

}
