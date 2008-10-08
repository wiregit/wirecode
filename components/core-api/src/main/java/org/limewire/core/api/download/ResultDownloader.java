package org.limewire.core.api.download;

import java.util.List;

import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchResult;
import com.limegroup.gnutella.RemoteFileDesc;

public interface ResultDownloader {

    /**
     * Adds a download triggered by the given search results. The search results
     * must all be for the same item, otherwise an
     * {@link IllegalArgumentException} may be thrown.
     * 
     * @param search The search that triggered these results. This may be null.
     * @param coreSearchResults The results for the file that should be
     *        downloaded. A list is used to indicate that multiple sources can
     *        be swarmed from at the same time. The list is not intended to
     *        provide different downloads.
     */
    public DownloadItem addDownload(Search search, List<? extends SearchResult> coreSearchResults)
            throws SaveLocationException;


    /**
     * Adds a download specified by the given remote file desc
     *
     * @param rfd The remote file desc to start downloading
     * @return DownloadItem with which the download can be tracked and controlled
     * @throws SaveLocationException if an error occurs while downloading and saving the
     * file
     */
    public DownloadItem addDownload(RemoteFileDesc rfd) throws SaveLocationException;
}