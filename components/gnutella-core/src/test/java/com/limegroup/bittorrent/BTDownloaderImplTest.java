package com.limegroup.bittorrent;

import java.io.File;
import java.io.IOException;

import org.limewire.listener.EventListener;
import org.limewire.util.BaseTestCase;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.limegroup.bittorrent.swarm.BTSwarmCoordinatorTest;
import com.limegroup.gnutella.ActivityCallbackAdapter;
import com.limegroup.gnutella.LimeWireCoreModule;
import com.limegroup.gnutella.downloader.CoreDownloaderFactory;
import com.limegroup.gnutella.downloader.DownloadStatusEvent;

public class BTDownloaderImplTest extends BaseTestCase {

    public BTDownloaderImplTest(String name) {
        super(name);
    }

    public void testBasic() throws Exception {
        File torrentFile = new File(BTMetaInfoTest.TEST_DATA_DIR
                + "/test-single-webseed-single-file.torrent");
        BTDownloader downloader = createBTDownloader(torrentFile);
        downloader.startDownload();

        Thread.sleep(3000);

    }

    private BTDownloader createBTDownloader(File torrentFile) throws IOException {
        final BTMetaInfo metaInfo = BTSwarmCoordinatorTest.createMetaInfo(torrentFile);
        Injector injector = Guice.createInjector(Stage.PRODUCTION, new LimeWireCoreModule(
                ActivityCallbackAdapter.class));

        CoreDownloaderFactory coreDownloaderFactory = injector
                .getInstance(CoreDownloaderFactory.class);
        BTDownloader downloader = coreDownloaderFactory.createBTDownloader(metaInfo);
        downloader.initBtMetaInfo(metaInfo);

        downloader.addListener(new EventListener<DownloadStatusEvent>() {

            public void handleEvent(DownloadStatusEvent event) {
                System.out.println("event: " + event.getType().name() + " - "
                        + event.getSource().getFile());
            }

        });

        return downloader;
    }
}
