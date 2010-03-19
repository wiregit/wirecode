package org.limewire.rest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.core.api.URN;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.library.URNFactory;
import org.limewire.core.api.magnet.MagnetFactory;
import org.limewire.core.api.magnet.MagnetLink;
import org.limewire.core.api.search.GroupedSearchResult;
import org.limewire.core.api.search.SearchManager;
import org.limewire.core.api.search.SearchResultList;
import org.limewire.util.URIUtils;

import com.google.inject.Inject;

/**
 * Request handler for Download services.
 */
class DownloadRequestHandler extends AbstractRestRequestHandler {

    private static final String ALL = "";
    private static final String START = "";
    
    private final DownloadListManager downloadListManager;
    private final SearchManager searchManager;
    private final MagnetFactory magnetFactory;
    private final URNFactory urnFactory;
    
    @Inject
    public DownloadRequestHandler(DownloadListManager downloadListManager,
            SearchManager searchManager,
            MagnetFactory magnetFactory,
            URNFactory urnFactory) {
        this.downloadListManager = downloadListManager;
        this.searchManager = searchManager;
        this.magnetFactory = magnetFactory;
        this.urnFactory = urnFactory;
    }
    
    @Override
    public void handle(HttpRequest request, HttpResponse response, HttpContext context)
            throws HttpException, IOException {
        
        // Get request method and uri target.
        String method = request.getRequestLine().getMethod();
        String uriTarget = RestUtils.getUriTarget(request, RestPrefix.DOWNLOAD.pattern());
        Map<String, String> queryParams = RestUtils.getQueryParams(request);
        
        if (RestUtils.GET.equals(method) && ALL.equals(uriTarget)) {
            // Get data for all downloads.
            doGetAll(uriTarget, queryParams, response);
            
        } else if (RestUtils.GET.equals(method) && (uriTarget.length() > 0)) {
            // Get progress for single download.
            doGetProgress(uriTarget, queryParams, response);
                
        } else if (RestUtils.POST.equals(method) && START.equals(uriTarget)) {
            // Start download.
            doStart(uriTarget, queryParams, response);
                
        } else if (RestUtils.DELETE.equals(method) && (uriTarget.length() > 0)) {
            // Cancel download.
            doCancel(uriTarget, response);
                
        } else {
            response.setStatusCode(HttpStatus.SC_NOT_IMPLEMENTED);
        }
    }

    /**
     * Processes request to retrieve data for all downloads.
     */
    private void doGetAll(String uriTarget, Map<String, String> queryParams,
            HttpResponse response) throws IOException {
        
        List<DownloadItem> downloadItems = downloadListManager.getDownloads();
        
        try {
            // Create JSON result.
            JSONArray jsonArr = new JSONArray();
            for (DownloadItem downloadItem : downloadItems) {
                jsonArr.put(createDownloadDescription(downloadItem));
            }

            // Set response entity and status.
            HttpEntity entity = RestUtils.createStringEntity(jsonArr.toString());
            response.setEntity(entity);
            response.setStatusCode(HttpStatus.SC_OK);
            
        } catch (JSONException ex) {
            throw new IOException(ex);
        }
    }
    
    /**
     * Processes request to retrieve progress for a single download.
     */
    private void doGetProgress(String uriTarget, Map<String, String> queryParams,
            HttpResponse response) throws IOException {
        
        // Create URN using ID string.
        String id = parseId(uriTarget);
        URN urn = urnFactory.createSHA1Urn(id);
        
        // Get download item.
        DownloadItem downloadItem = downloadListManager.getDownloadItem(urn);
        
        try {
            if (downloadItem != null) {
                // Return download data.
                JSONObject jsonObj = createDownloadDescription(downloadItem);
                HttpEntity entity = RestUtils.createStringEntity(jsonObj.toString());
                response.setEntity(entity);
                response.setStatusCode(HttpStatus.SC_OK);
            } else {
                response.setStatusCode(HttpStatus.SC_NOT_FOUND);
            }
            
        } catch (JSONException ex) {
            throw new IOException(ex);
        }
    }
    
    /**
     * Starts a new download using the specified uri target and query parameters.
     */
    private void doStart(String uriTarget, Map<String, String> queryParams, 
            HttpResponse response) throws IOException {
        
        // Get parameters to start download.
        String fileId = queryParams.get("id");
        String searchId = queryParams.get("searchId");
        String magnet = queryParams.get("magnet");
        String torrent = queryParams.get("torrent");
        
        // Start download.
        DownloadItem[] downloadItems = null;
        if ((fileId != null) && (searchId != null)) {
            downloadItems = startResultDownload(fileId, searchId);
        } else if (magnet != null) {
            downloadItems = startMagnetDownload(magnet);
        } else if (torrent != null) {
            downloadItems = startTorrentDownload(torrent);
        }
        
        // Return not found if download not started.
        if (downloadItems == null || downloadItems.length == 0) {
            response.setStatusCode(HttpStatus.SC_NOT_FOUND);
            return;
        }

        try {
            // Return download data.
            JSONArray jsonArr = new JSONArray();
            for (DownloadItem downloadItem : downloadItems) {
                jsonArr.put(createDownloadDescription(downloadItem));
            }
            HttpEntity entity = RestUtils.createStringEntity(jsonArr.toString());
            response.setEntity(entity);
            response.setStatusCode(HttpStatus.SC_OK);
        } catch (JSONException ex) {
            throw new IOException(ex);
        }
    }
    
    /**
     * Cancels a search using the specified uri target.
     */
    private void doCancel(String uriTarget, HttpResponse response) throws IOException {
        // Create URN using ID string.
        String id = parseId(uriTarget);
        URN urn = urnFactory.createSHA1Urn(id);
        
        // Get download item.
        DownloadItem downloadItem = downloadListManager.getDownloadItem(urn);

        if (downloadItem != null) {
            if (downloadItem.getState().isFinished()) {
                // Remove finished download.
                downloadListManager.remove(downloadItem);
            } else {
                // Cancel existing download.
                downloadItem.cancel();
            }
            
            // Set OK status.
            response.setStatusCode(HttpStatus.SC_OK);
            
        } else {
            response.setStatusCode(HttpStatus.SC_NOT_FOUND);
        }
    }
    
    /**
     * Starts a download of the specified search result and returns an array
     * containing the download item.  The method returns an empty array if the
     * download could not be started.
     */
    private DownloadItem[] startResultDownload(String fileUrn, String searchGuid) throws IOException {
        // Get search.
        SearchResultList resultList = searchManager.getSearchResultList(searchGuid);
        if (resultList == null) {
            return null;
        }

        // Find search results for URN.
        URN urn = urnFactory.createSHA1Urn(fileUrn);
        GroupedSearchResult groupedResult = resultList.getGroupedResult(urn);
        if (groupedResult == null) {
            return null;
        }

        // Add download to manager.
        DownloadItem downloadItem = downloadListManager.addDownload(
                resultList.getSearch(), groupedResult.getSearchResults(), null, true);
        return (downloadItem == null) ? new DownloadItem[0] : new DownloadItem[] { downloadItem };
    }
    
    /**
     * Starts a download of the specified magnet link and returns an array of
     * download items.  The method returns an empty array if the uri is not a
     * magnet link.
     */
    private DownloadItem[] startMagnetDownload(String magnetUri) throws IOException {
        try {
            // Create URI.
            URI uri = URIUtils.toURI(magnetUri);

            if (magnetFactory.isMagnetLink(uri)) {
                // Create magnet links
                MagnetLink[] magnetLinks = magnetFactory.parseMagnetLink(uri);
                
                // Add download to manager.
                DownloadItem[] downloadItems = new DownloadItem[magnetLinks.length]; 
                for (int i = 0; i < magnetLinks.length; i++) {
                    downloadItems[i] = downloadListManager.addDownload(magnetLinks[i], null, true);
                }
                return downloadItems;
                
            } else {
                // Return empty array.
                return new DownloadItem[0];
            }
            
        } catch (URISyntaxException ex) {
            throw new IOException(ex);
        }
    }
    
    /**
     * Starts a download of the specified torrent uri and returns an array of
     * download items.  The method returns an empty array if the uri is really
     * a magnet link.
     */
    private DownloadItem[] startTorrentDownload(String torrentUri) throws IOException {
        try {
            // Create URI.
            URI uri = URIUtils.toURI(torrentUri);

            if (!magnetFactory.isMagnetLink(uri)) {
                // Add download to manager.
                DownloadItem downloadItem = downloadListManager.addTorrentDownload(uri, true);
                return (downloadItem == null) ? new DownloadItem[0] : new DownloadItem[] { downloadItem };
                
            } else {
                // Return empty array.
                return new DownloadItem[0];
            }
            
        } catch (URISyntaxException ex) {
            throw new IOException(ex);
        }
    }
    
    /**
     * Creates the JSON description object for the specified download item.
     */
    private JSONObject createDownloadDescription(DownloadItem downloadItem) throws JSONException {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("filename", downloadItem.getFileName());
        jsonObj.put("id", downloadItem.getUrn());
        jsonObj.put("size", downloadItem.getTotalSize());
        jsonObj.put("bytesDownloaded", downloadItem.getCurrentSize());
        jsonObj.put("state", downloadItem.getState());
        return jsonObj;
    }
    
    /**
     * Returns the ID string from the specified URI target.  The ID string
     * should be between the first and second slash characters.
     */
    private String parseId(String uriTarget) {
        int pos1 = uriTarget.indexOf("/");
        String id = (pos1 < 0) ? uriTarget : uriTarget.substring(pos1 + 1);
        
        int pos2 = id.indexOf("/");
        return (pos2 < 0) ? id : id.substring(0, pos2);
    }
}
