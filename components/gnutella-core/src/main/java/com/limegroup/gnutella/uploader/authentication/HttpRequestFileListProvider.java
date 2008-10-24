package com.limegroup.gnutella.uploader.authentication;

import java.io.IOException;

import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpContext;

import com.limegroup.gnutella.library.FileList;
import com.limegroup.gnutella.uploader.HttpException;

public interface HttpRequestFileListProvider {

    Iterable<FileList> getFileList(HttpRequest request, HttpContext httpContext) throws HttpException, IOException, org.apache.http.HttpException;
}
