package org.limewire.core.impl.integration;

import java.io.IOException;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.DownloadState;
import org.limewire.io.GUID;
import org.limewire.io.URN;
import org.limewire.rest.RestPrefix;

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

        String nm = getName();
        if (nm.contains("NoMock")) {
            super.setUp(); // no mocks
        } else {
            setUpModules(new MockDownloadModule());
            loadMockDownloads();
        }
    }

    // -------------------------- tests --------------------------

    public void testBasic() throws Exception {

        // all downloads progress metadata
        int size = listGET(DOWNLOAD_PREFIX, NO_PARAMS).size();
        assertEquals("download size unexpected", downItems.size(), size);

        // individual download progress metadata
        for (DownloadItem expectedItem : downItems) {
            assertDownloadMD(expectedItem);
        }
    }

    public void testCancel() throws Exception {

        int statusCode = -1;

        // before cancel
        int size = listGET(DOWNLOAD_PREFIX, NO_PARAMS).size();
        assertEquals("list size unexpected", downItems.size(), size);

        /**
         * delete all: expectations for sequence of calls invoked by delete set
         * in loadMockQueries
         */
        for (DownloadItem item : downItems) {
            statusCode = httpDelete(DOWNLOAD_PREFIX + "/" + item.getUrn(), NO_PARAMS);
            assertEquals("unexpected status", HttpStatus.SC_OK, statusCode);
        }

        // let's ensure an invalid download item
        context.checking(new Expectations() {
            {
                one(dlistMgr).getDownloadItem(with(any(URN.class)));
                will(returnValue(null));
            }
        });

        // try to cancel item again
        URN urn = downItems.get(0).getUrn();
        statusCode = httpDelete(DOWNLOAD_PREFIX + "/" + urn, NO_PARAMS);
        assertEquals("shouldn't be found", HttpStatus.SC_NOT_FOUND, statusCode);
    }

    public void testCancelNegativesNoMocks() throws Exception {

        int statusCode = -1;

        // try to cancel random URN
        statusCode = httpDelete(DOWNLOAD_PREFIX + "/" + generateURN(), NO_PARAMS);
        assertEquals("shouldn't be found", HttpStatus.SC_NOT_FOUND, statusCode);

        // try to cancel a target (i.e. no urn)
        statusCode = httpDelete(DOWNLOAD_PREFIX, NO_PARAMS);
        assertEquals("no urn passed", HttpStatus.SC_NOT_IMPLEMENTED, statusCode);

        // try to cancel garbage URN (LWC-5565)
        // statusCode = httpDelete(DOWNLOAD_PREFIX + "/GARBAGE_NOT_BASE16",NO_PARAMS);
        // assertEquals("garbage urn",HttpStatus.SC_NOT_FOUND,statusCode);
    }

    // TODO: test posts
    public void testPost() throws Exception {    
    }
    
    public void testPostNegativesNoMock() throws Exception {
        
        // some garbage parameters
        String fakeID = "id=" + generateURN();
        String fakeSrch = "searchId=BA8DB600AC11FE2EE3033F5AFF57F500";
        String[] badParams = { NO_PARAMS, "id=", "searchId=", fakeID, fakeSrch,
                fakeID + "&" + fakeSrch, "magnet=", "torrent=", "magnet=blah", "not_a_param=" }; // "torrent=blah"

        // check missing/bad parameters
        for (String param : badParams) {
            int statusCode = httpPost(DOWNLOAD_PREFIX, param);
            assertEquals("post shouldn't succeed", HttpStatus.SC_NOT_FOUND, statusCode);
        }
    }

    // ---------------------- private ----------------------

    /**
     * compare download metadata GET with expected DownloadItem
     */
    private void assertDownloadMD(DownloadItem item) throws Exception {
        Map<String, String> results = metadataGET(DOWNLOAD_PREFIX + "/" + item.getUrn(), NO_PARAMS);
        assertEquals("urn unexpected", item.getUrn().toString(), results.get("id"));
        assertEquals("state unexpected", item.getState().toString(), results.get("state"));
        assertEquals("name unexpected", item.getFileName(), results.get("filename"));
        assertEquals("size unexpected", String.valueOf(item.getTotalSize()), results.get("size"));
        assertEquals("csize unexpected", String.valueOf(item.getCurrentSize()), results
                .get("bytesDownloaded"));
    }

    /**
     * populates mock download results
     */
    private void loadMockDownloads() throws Exception {
        addMockDownload(DownloadState.DOWNLOADING, 20);
        addMockDownload(DownloadState.DOWNLOADING, 50);
        addMockDownload(DownloadState.DONE, 100);
        addMockDownload(DownloadState.PAUSED, 19);
        addMockDownload(DownloadState.CANCELLED, 0);
    }

    /**
     * adds mock download file to the download items list
     */
    private void addMockDownload(final DownloadState state, final int percentComplete)
            throws IOException {

        if (downItems == null) {
            downItems = new BasicEventList<DownloadItem>();
            context.checking(new Expectations() {
                {
                    allowing(dlistMgr).getDownloads();
                    will(returnValue(downItems));
                }
            });
        }
        final URN urn = generateURN();
        final DownloadItem mockDownloadItem = context.mock(DownloadItem.class);
        downItems.add(mockDownloadItem);

        context.checking(new Expectations() {
            {
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

                if (state.equals(DownloadState.DONE)) {
                    exactly(1).of(dlistMgr).remove(mockDownloadItem);
                } else {
                    exactly(1).of(mockDownloadItem).cancel();
                }

            }
        });
    }

    /**
     * generate mock urn
     */
    private URN generateURN() {
        final URN urn = new MockURN("urn:sha1:" + GUID.toHexString(GUID.makeGuid()));
        return urn;
    }

    /**
     * mock download list manager
     */
    protected static class MockDownloadModule extends AbstractModule {
        @Override protected void configure() {
            bind(DownloadListManager.class).toInstance(context.mock(DownloadListManager.class));
        }
    }

}
