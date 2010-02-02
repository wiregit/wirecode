package org.limewire.core.impl.rest.handler;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.nio.protocol.SimpleNHttpRequestHandler;
import org.apache.http.protocol.HttpContext;
import org.limewire.core.api.library.LibraryFileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;

import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;

/**
 * Request handler for Library services.
 */
class LibraryRequestHandler extends SimpleNHttpRequestHandler {

    private static final String METADATA = "";
    private static final String FILES = "/files";
    
    private final LibraryManager libraryManager;
    
    @Inject
    public LibraryRequestHandler(LibraryManager libraryManager) {
        this.libraryManager = libraryManager;
    }
    
    @Override
    public void handle(HttpRequest request, HttpResponse response, HttpContext context)
            throws HttpException, IOException {
        
        String method = request.getRequestLine().getMethod();
        if ("GET".equals(method)) {
            try {
                String uriStr = request.getRequestLine().getUri();
                
                // Strip off uri prefix.
                int pos = uriStr.indexOf(RestTarget.LIBRARY.pattern());
                if (pos < 0) return;
                String uriTarget = uriStr.substring(pos + RestTarget.LIBRARY.pattern().length());
                
                // Strip off query parameters.
                pos = uriTarget.indexOf("?");
                uriTarget = (pos < 0) ? uriTarget : uriTarget.substring(0, pos);
                
                // Get query parameters.
                URI uri = new URI(uriStr);
                List<NameValuePair> queryParams = URLEncodedUtils.parse(uri, null);
                
                // Set response.
                process(uriTarget, queryParams, response);
                
            } catch (URISyntaxException e) {
                throw new IOException(e);
            }
            
        } else if ("DELETE".equals(method)){
            // TODO implement
            response.setStatusCode(HttpStatus.SC_NOT_IMPLEMENTED);
            
        } else {
            response.setStatusCode(HttpStatus.SC_NOT_IMPLEMENTED);
        }
    }

    @Override
    public ConsumingNHttpEntity entityRequest(HttpEntityEnclosingRequest request,
            HttpContext context) throws HttpException, IOException {
        return null;
    }
    
    /**
     * Processes the specified uri target and query parameters.
     */
    private void process(String uriTarget, List<NameValuePair> queryParams, HttpResponse response) 
            throws IOException {
        
        if (METADATA.equals(uriTarget)) {
            // Create result string.
            // TODO process query parameters to filter by category
            StringBuilder builder = new StringBuilder();
            builder.append("{name: \"Library\"");
            builder.append(", size: ").append(libraryManager.getLibraryManagedList().size());
            builder.append(", id: \"library\"}");
            
            // Set response entity and status.
            NStringEntity entity = new NStringEntity(builder.toString());
            response.setEntity(entity);
            response.setStatusCode(HttpStatus.SC_OK);
            
        } else if (FILES.equals(uriTarget)) {
            // Get library files.
            LibraryFileList fileList = libraryManager.getLibraryManagedList();
            
            // Create result string.
            // TODO process query parameters to filter by category and apply 
            StringBuilder builder = new StringBuilder();
            builder.append("[\n");
            EventList<LocalFileItem> fileItemList = fileList.getModel();
            for (LocalFileItem fileItem : fileItemList) {
                builder.append(createFileDescription(fileItem)).append("\n");
            }
            builder.append("]");
            
            // Set response entity and status.
            NStringEntity entity = new NStringEntity(builder.toString());
            response.setEntity(entity);
            response.setStatusCode(HttpStatus.SC_OK);
            
        } else {
            response.setStatusCode(HttpStatus.SC_NOT_IMPLEMENTED);
        }
    }
    
    /**
     * Creates the file description string for the specified file item.
     */
    private String createFileDescription(LocalFileItem fileItem) {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append("\"filename\": \"").append(fileItem.getFileName()).append("\"");
        builder.append("}");
        return builder.toString();
    }
}
