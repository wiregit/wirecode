package com.limegroup.gnutella.uploader.authentication;


import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpContext;

import com.limegroup.gnutella.library.FileList;
import com.limegroup.gnutella.uploader.HttpException;

/**
 * Implemented by classes that that lookup a file lists for an {@link HttpRequest}.
 */
public interface HttpRequestFileListProvider {

    /**
     * @return iterable of file lists
     * @throws HttpException if the file list for the request was not found or
     * the request is not authorized
     */
    Iterable<FileList> getFileLists(HttpRequest request, HttpContext httpContext) throws HttpException;
}
