package com.limegroup.gnutella;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentFileEntry;
import org.limewire.bittorrent.TorrentInfo;
import org.limewire.core.api.download.DownloadException;
import org.limewire.core.settings.FilterSettings;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.inject.GuiceUtils;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.limegroup.gnutella.downloader.CantResumeException;

public class DownloadManagerImplTest extends LimeTestCase {

    private DownloadManagerImpl downloadManager;
    private Mockery mockery;

    public DownloadManagerImplTest(String name) {
        super(name);
    }

    @Override
    public void setUp() {
        mockery = new Mockery();
        downloadManager = createDownloadManager();
        downloadManager.start();
    }

    @Override
    public void tearDown() {
        downloadManager.stop();
    }

    public void testContains() throws DownloadException, CantResumeException {
        Downloader downloader = downloadManager.download(new File("T-5-"+UUID.randomUUID().toString()));
        assertTrue(downloadManager.contains(downloader));

        Downloader dummyDownloader = createDummyDownloader();
        assertFalse(downloadManager.contains(dummyDownloader));
        downloader.stop();
    }

    public void testGetBannedExtensions() {
        FilterSettings.BANNED_EXTENSIONS.set(new String[] {".wma"});
        final Torrent torrent = mockery.mock(Torrent.class);
        final TorrentInfo torrentInfo = mockery.mock(TorrentInfo.class);
        final TorrentFileEntry wma = mockery.mock(TorrentFileEntry.class);
        final TorrentFileEntry mp3 = mockery.mock(TorrentFileEntry.class);
        final TorrentFileEntry none = mockery.mock(TorrentFileEntry.class);
        final ArrayList<TorrentFileEntry> entries =
            new ArrayList<TorrentFileEntry>();
        entries.add(wma);
        entries.add(mp3);
        entries.add(none);
        mockery.checking(new Expectations() {{
            one(torrent).getTorrentInfo();
            will(returnValue(torrentInfo));
            one(torrentInfo).getTorrentFileEntries();
            will(returnValue(entries));
            one(wma).getPath();
            will(returnValue("foo/bar.wma"));
            one(mp3).getPath();
            will(returnValue("foo/bar.mp3"));
            one(none).getPath();
            will(returnValue("foo/bar"));
        }});
        Set<String> banned = downloadManager.getBannedExtensions(torrent);
        assertEquals(banned, Collections.singleton("wma"));
        mockery.assertIsSatisfied();
    }

    private Downloader createDummyDownloader() {
        Downloader downloader = mockery.mock(Downloader.class);
        return downloader;
    }

    private DownloadManagerImpl createDownloadManager() {
        Injector injector = Guice.createInjector(Stage.DEVELOPMENT,
                new LimeWireCoreModule(ActivityCallbackAdapter.class));
        GuiceUtils.loadEagerSingletons(injector);
        return injector.getInstance(DownloadManagerImpl.class);
    }
}
