package org.limewire.core.impl.integration;

import java.io.IOException;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.api.URN;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.DownloadState;
import org.limewire.io.GUID;
import org.limewire.rest.RestPrefix;
import org.limewire.ui.swing.search.model.MockURN;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;

/**
 * download integration tests for Rest APIs
 */
public class RestDownloadTest extends AbstractRestIntegrationTestcase {

    private static final String DOWNLOAD_PREFIX = RestPrefix.DOWNLOAD.pattern();;

    private static Mockery context = new Mockery();

    private EventList<DownloadItem> downItems;

    @Inject private DownloadListManager dlistMgr;

    public RestDownloadTest(String name) {
        super(name);
    }

    @Override public void setUp() throws Exception {
        setUpModules(new MockDownloadModule());
        loadMockQueries();
    }

    // -------------------------- tests --------------------------

    public void testBasic() throws Exception {

        // download items are retrievable
        int respSz = listGET(DOWNLOAD_PREFIX, NO_PARAMS).size();
        assertEquals("download size unexpected", downItems.size(), respSz);

        // single items retrievable
        for (DownloadItem item : downItems) {
            String urn = item.getUrn().toString();
            Map<String, String> m = metadataGET(DOWNLOAD_PREFIX+"/"+urn,NO_PARAMS);
            assertEquals("download item unexpected",urn,m.get("id"));
        }
    }

    // ---------------------- private ----------------------

    /**
     * populates mock download results
     */
    private void loadMockQueries() throws Exception {

        downItems = new BasicEventList<DownloadItem>();
        context.checking(new Expectations() {{
                allowing(dlistMgr).getDownloads();
                will(returnValue(downItems));
            }
        });
        addMockDownload(DownloadState.DOWNLOADING, 20);
        addMockDownload(DownloadState.DOWNLOADING, 50);
        addMockDownload(DownloadState.DONE, 100);
        addMockDownload(DownloadState.PAUSED, 19);
        addMockDownload(DownloadState.CANCELLED, 0);
    }

    /**
     * adds mock download file to the download items list
     * 
     * @throws IOException
     */
    private void addMockDownload(final DownloadState state, final int percentComplete)
            throws IOException {

        final DownloadItem mockDownloadItem = context.mock(DownloadItem.class);
        final URN urn = makeURN();
        downItems.add(mockDownloadItem);

        context.checking(new Expectations() {{
                allowing(mockDownloadItem).getFileName();
                will(returnValue("mockDownload" + urn.hashCode()));
                allowing(mockDownloadItem).getUrn();
                will(returnValue(urn));
                allowing(mockDownloadItem).getTotalSize();
                will(returnValue(32768L));
                allowing(mockDownloadItem).getCurrentSize();
                will(returnValue((32768L * percentComplete / 100)));
                allowing(mockDownloadItem).getState();
                will(returnValue(state));
                one(dlistMgr).getDownloadItem(with(any(URN.class)));
                will(returnValue(mockDownloadItem));
            }
        });
    }

    /**
     * create a fake URN
     */
    private URN makeURN() {
        String url = "urn:sha1:" + GUID.toHexString(GUID.makeGuid());
        return new MockURN(url);
    }

    /**
     * mock download list manager
     */
    private static class MockDownloadModule extends AbstractModule {
        @Override protected void configure() {
            bind(DownloadListManager.class).toInstance(context.mock(DownloadListManager.class));
        }
    }

}
