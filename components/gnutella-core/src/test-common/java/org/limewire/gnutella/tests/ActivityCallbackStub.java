package org.limewire.gnutella.tests;

import java.io.File;
import java.util.Set;

import org.limewire.bittorrent.Torrent;
import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.DownloadException;
import org.limewire.io.GUID;

import com.google.inject.Singleton;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;

/**
 * A stub for ActivityCallback.  Does nothing.
 */
@Singleton
public class ActivityCallbackStub implements ActivityCallback {

    @Override
    public void installationCorrupted() {}

    @Override
    public void handleQueryResult(RemoteFileDesc rfd, 
            QueryReply queryReply,
            Set alts) {}

    @Override
    public void handleQuery(QueryRequest query, String address, int port) { }

    @Override
    public void addDownload(Downloader d) { }

    @Override
    public void removeDownload(Downloader d) { }

    @Override
    public void addUpload(Uploader u) { }

    @Override
    public void uploadComplete(Uploader u) { }

    @Override
    public void downloadsComplete() { }

    @Override
    public void uploadsComplete() { }

    @Override
    public void promptAboutUnscannedPreview(Downloader dloader) {
        dloader.discardUnscannedPreview(false);
    }

    @Override
    public void restoreApplication() {}

    @Override
    public void handleSharedFileUpdate(File file) { }

    @Override
    public boolean isQueryAlive(GUID guid) {
        return false;
    }

    @Override
    public void handleMagnets(final MagnetOptions[] magnets) { }

    @Override
    public void handleTorrent(File torrentFile) { }

    @Override
    public String translate(String s) {
        return s;
    }

    @Override
    public void handleDownloadException(DownloadAction downLoadAction,
            DownloadException e, boolean supportsNewSaveDir) {
    }

    @Override
    public boolean promptTorrentFilePriorities(Torrent torrent) {
        return true;
    }

    @Override
    public boolean promptAboutTorrentWithBannedExtensions(Torrent torrent, Set<String> bannedExtensions) {
        return true;
    }

    @Override
    public boolean promptAboutTorrentDownloadWithFailedScan() {
        return true;
    }
}
