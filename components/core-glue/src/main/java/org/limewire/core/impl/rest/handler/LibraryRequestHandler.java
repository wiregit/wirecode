package org.limewire.core.impl.rest.handler;

import java.io.IOException;
import java.util.Map;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.protocol.HttpContext;
import org.limewire.core.api.library.LibraryFileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;

import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;

/**
 * Request handler for Library services.
 */
class LibraryRequestHandler extends AbstractRestRequestHandler {

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
        if (GET.equals(method)) {
            // Get uri target.
            String uriTarget = getUriTarget(request, RestTarget.LIBRARY.pattern());
            
            // Get query parameters.
            Map<String, String> queryParams = getQueryParams(request);
            
            // Set response.
            process(uriTarget, queryParams, response);
            
        } else {
            response.setStatusCode(HttpStatus.SC_NOT_IMPLEMENTED);
        }
    }
    
    /**
     * Processes the specified uri target and query parameters.
     */
    private void process(String uriTarget, Map<String, String> queryParams, HttpResponse response) 
            throws IOException {
        
        if (METADATA.equals(uriTarget)) {
            // Get library files.
            LibraryFileList fileList = libraryManager.getLibraryManagedList();
            
            // Create result string.
            StringBuilder builder = new StringBuilder();
            builder.append("{name: \"Library\"");
            builder.append(", size: ").append(fileList.size());
            builder.append(", id: \"library\"}");
            
            // Set response entity and status.
            NStringEntity entity = new NStringEntity(builder.toString());
            response.setEntity(entity);
            response.setStatusCode(HttpStatus.SC_OK);
            
        } else if (FILES.equals(uriTarget)) {
            // Get library files.
            LibraryFileList fileList = libraryManager.getLibraryManagedList();
            EventList<LocalFileItem> fileItemList = fileList.getModel();
            
            // Get query parameters.
            String offsetStr = queryParams.get("offset");
            String limitStr = queryParams.get("limit");
            int offset = (offsetStr != null) ? Integer.parseInt(offsetStr) : 0;
            int limit = (limitStr != null) ? Integer.parseInt(limitStr) : fileItemList.size();
            
            // Create result string with requested files.
            StringBuilder builder = new StringBuilder();
            builder.append("[\n");
            for (int i = offset, max = Math.min(offset + limit, fileItemList.size()); i < max; i++) {
                LocalFileItem fileItem = fileItemList.get(i);
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
