package org.limewire.core.impl.download;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.spam.SpamManager;
import org.limewire.core.impl.URNImpl;
import org.limewire.core.impl.download.listener.ItunesDownloadListener;
import org.limewire.core.impl.download.listener.ItunesDownloadListenerFactory;
import org.limewire.listener.EventListener;
import org.limewire.util.AssignParameterAction;
import org.limewire.util.BaseTestCase;
import org.limewire.util.TestPropertyChangeListener;

import ca.odell.glazedlists.EventList;

import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;

public class CoreDownloadListManagerTest extends BaseTestCase {

    public CoreDownloadListManagerTest(String name) {
        super(name);
    }

    /**
     * Tests that downloadRemove events do not remove the download item from the
     * core download list manager list.
     */
    @SuppressWarnings("unchecked")
    public void testCompletedDownloadsNotRemovedFromCoreDownloadManagerListUntilCleared()
            throws Exception {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final DownloadManager downloadManager = context.mock(DownloadManager.class);
        final DownloadListenerList listenerList = context.mock(DownloadListenerList.class);
        final ScheduledExecutorService backgroundExecutor = context
                .mock(ScheduledExecutorService.class);
        final RemoteFileDescFactory remoteFileDescFactory = context
                .mock(RemoteFileDescFactory.class);
        final ActivityCallback activityCallback = context.mock(ActivityCallback.class);
        final SpamManager spamManager = context.mock(SpamManager.class);
        final ItunesDownloadListenerFactory itunesDownloadListenerFactory = context
                .mock(ItunesDownloadListenerFactory.class);

        final AtomicReference<DownloadListener> downloadListener = new AtomicReference<DownloadListener>();

        context.checking(new Expectations() {
            {
                one(listenerList).addDownloadListener(with(any(DownloadListener.class)));
                will(new AssignParameterAction<DownloadListener>(downloadListener, 0));
                one(backgroundExecutor).scheduleAtFixedRate(with(any(Runnable.class)),
                        with(any(long.class)), with(any(long.class)), with(any(TimeUnit.class)));
            }
        });

        CoreDownloadListManager coreDownloadListManager = new CoreDownloadListManager(
                downloadManager, listenerList, backgroundExecutor, remoteFileDescFactory,
                activityCallback, spamManager, itunesDownloadListenerFactory);

        final Downloader unfinishedItem1 = context.mock(Downloader.class);
        final Downloader unfinishedItem2 = context.mock(Downloader.class);
        final Downloader finishedItem1 = context.mock(Downloader.class);
        final Downloader finishedItem2 = context.mock(Downloader.class);

        final URN urn1 = URN.createUrnFromString("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA1");
        final URN urn2 = URN.createUrnFromString("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA2");
        final URN urn3 = URN.createUrnFromString("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA3");
        final URN urn4 = URN.createUrnFromString("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA4");

        final ItunesDownloadListener itunesDownloadListener = context
                .mock(ItunesDownloadListener.class);

        final AtomicReference<DownloadItem> downloadItem1 = new AtomicReference<DownloadItem>();
        final AtomicReference<DownloadItem> downloadItem2 = new AtomicReference<DownloadItem>();
        final AtomicReference<DownloadItem> downloadItem3 = new AtomicReference<DownloadItem>();
        final AtomicReference<DownloadItem> downloadItem4 = new AtomicReference<DownloadItem>();

        context.checking(new Expectations() {
            {
                allowing(itunesDownloadListenerFactory).createListener(with(any(Downloader.class)));
                will(returnValue(itunesDownloadListener));

                allowing(unfinishedItem1).getState();
                will(returnValue(Downloader.DownloadState.DOWNLOADING));
                allowing(unfinishedItem2).getState();
                will(returnValue(Downloader.DownloadState.CONNECTING));
                allowing(finishedItem1).getState();
                will(returnValue(Downloader.DownloadState.COMPLETE));
                allowing(finishedItem2).getState();
                will(returnValue(Downloader.DownloadState.COMPLETE));

                allowing(unfinishedItem1).addListener(with(any(EventListener.class)));
                allowing(unfinishedItem2).addListener(with(any(EventListener.class)));
                allowing(finishedItem1).addListener(with(any(EventListener.class)));
                allowing(finishedItem2).addListener(with(any(EventListener.class)));

                allowing(unfinishedItem1).setAttribute(with(equal(DownloadItem.DOWNLOAD_ITEM)),
                        with(any(DownloadItem.class)), with(equal(false)));
                will(new AssignParameterAction<DownloadItem>(downloadItem1, 1));
                allowing(unfinishedItem2).setAttribute(with(equal(DownloadItem.DOWNLOAD_ITEM)),
                        with(any(DownloadItem.class)), with(equal(false)));
                will(new AssignParameterAction<DownloadItem>(downloadItem2, 1));
                allowing(finishedItem1).setAttribute(with(equal(DownloadItem.DOWNLOAD_ITEM)),
                        with(any(DownloadItem.class)), with(equal(false)));
                will(new AssignParameterAction<DownloadItem>(downloadItem3, 1));
                allowing(finishedItem2).setAttribute(with(equal(DownloadItem.DOWNLOAD_ITEM)),
                        with(any(DownloadItem.class)), with(equal(false)));
                will(new AssignParameterAction<DownloadItem>(downloadItem4, 1));

                allowing(unfinishedItem1).getSha1Urn();
                will(returnValue(urn1));
                allowing(unfinishedItem2).getSha1Urn();
                will(returnValue(urn2));
                allowing(finishedItem1).getSha1Urn();
                will(returnValue(urn3));
                allowing(finishedItem2).getSha1Urn();
                will(returnValue(urn4));
            }
        });

        EventList<DownloadItem> downloads = coreDownloadListManager.getDownloads();

        downloadListener.get().downloadAdded(unfinishedItem1);
        downloadListener.get().downloadAdded(unfinishedItem2);
        downloadListener.get().downloadAdded(finishedItem1);
        downloadListener.get().downloadAdded(finishedItem2);

        assertEquals(4, downloads.size());

        assertTrue(coreDownloadListManager.contains(new URNImpl(urn1)));
        assertTrue(coreDownloadListManager.contains(new URNImpl(urn2)));
        assertTrue(coreDownloadListManager.contains(new URNImpl(urn3)));
        assertTrue(coreDownloadListManager.contains(new URNImpl(urn4)));

        assertNotNull(coreDownloadListManager.getDownloadItem(new URNImpl(urn1)));
        assertNotNull(coreDownloadListManager.getDownloadItem(new URNImpl(urn2)));
        assertNotNull(coreDownloadListManager.getDownloadItem(new URNImpl(urn3)));
        assertNotNull(coreDownloadListManager.getDownloadItem(new URNImpl(urn4)));

        context.checking(new Expectations() {
            {
                allowing(unfinishedItem1).getAttribute(DownloadItem.DOWNLOAD_ITEM);
                will(returnValue(downloadItem1.get()));
                allowing(unfinishedItem2).getAttribute(DownloadItem.DOWNLOAD_ITEM);
                will(returnValue(downloadItem2.get()));
                allowing(finishedItem1).getAttribute(DownloadItem.DOWNLOAD_ITEM);
                will(returnValue(downloadItem3.get()));
                allowing(finishedItem2).getAttribute(DownloadItem.DOWNLOAD_ITEM);
                will(returnValue(downloadItem4.get()));
            }
        });
        downloadListener.get().downloadRemoved(finishedItem1);
        downloadListener.get().downloadRemoved(finishedItem1);

        assertEquals(4, downloads.size());

        assertTrue(coreDownloadListManager.contains(new URNImpl(urn1)));
        assertTrue(coreDownloadListManager.contains(new URNImpl(urn2)));
        assertTrue(coreDownloadListManager.contains(new URNImpl(urn3)));
        assertTrue(coreDownloadListManager.contains(new URNImpl(urn4)));

        assertNotNull(coreDownloadListManager.getDownloadItem(new URNImpl(urn1)));
        assertNotNull(coreDownloadListManager.getDownloadItem(new URNImpl(urn2)));
        assertNotNull(coreDownloadListManager.getDownloadItem(new URNImpl(urn3)));
        assertNotNull(coreDownloadListManager.getDownloadItem(new URNImpl(urn4)));

        coreDownloadListManager.clearFinished();

        assertEquals(2, downloads.size());
        assertTrue(coreDownloadListManager.contains(new URNImpl(urn1)));
        assertTrue(coreDownloadListManager.contains(new URNImpl(urn2)));

        assertFalse(coreDownloadListManager.contains(new URNImpl(urn3)));
        assertFalse(coreDownloadListManager.contains(new URNImpl(urn4)));

        assertNotNull(coreDownloadListManager.getDownloadItem(new URNImpl(urn1)));
        assertNotNull(coreDownloadListManager.getDownloadItem(new URNImpl(urn2)));
        assertNull(coreDownloadListManager.getDownloadItem(new URNImpl(urn3)));
        assertNull(coreDownloadListManager.getDownloadItem(new URNImpl(urn4)));

        context.assertIsSatisfied();
    }

    /**
     * Tests that completed downloads are removed from the
     * CoreDownloadListManager when the clearFinished method is called.
     */
    @SuppressWarnings("unchecked")
    public void testClearFinished() throws Exception {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final DownloadManager downloadManager = context.mock(DownloadManager.class);
        final DownloadListenerList listenerList = context.mock(DownloadListenerList.class);
        final ScheduledExecutorService backgroundExecutor = context
                .mock(ScheduledExecutorService.class);
        final RemoteFileDescFactory remoteFileDescFactory = context
                .mock(RemoteFileDescFactory.class);
        final ActivityCallback activityCallback = context.mock(ActivityCallback.class);
        final SpamManager spamManager = context.mock(SpamManager.class);
        final ItunesDownloadListenerFactory itunesDownloadListenerFactory = context
                .mock(ItunesDownloadListenerFactory.class);

        final AtomicReference<DownloadListener> downloadListener = new AtomicReference<DownloadListener>();

        context.checking(new Expectations() {
            {
                one(listenerList).addDownloadListener(with(any(DownloadListener.class)));
                will(new AssignParameterAction<DownloadListener>(downloadListener, 0));
                one(backgroundExecutor).scheduleAtFixedRate(with(any(Runnable.class)),
                        with(any(long.class)), with(any(long.class)), with(any(TimeUnit.class)));
            }
        });

        CoreDownloadListManager coreDownloadListManager = new CoreDownloadListManager(
                downloadManager, listenerList, backgroundExecutor, remoteFileDescFactory,
                activityCallback, spamManager, itunesDownloadListenerFactory);

        final Downloader unfinishedItem1 = context.mock(Downloader.class);
        final Downloader unfinishedItem2 = context.mock(Downloader.class);
        final Downloader finishedItem1 = context.mock(Downloader.class);
        final Downloader finishedItem2 = context.mock(Downloader.class);

        final URN urn1 = URN.createUrnFromString("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA1");
        final URN urn2 = URN.createUrnFromString("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA2");
        final URN urn3 = URN.createUrnFromString("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA3");
        final URN urn4 = URN.createUrnFromString("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA4");

        final ItunesDownloadListener itunesDownloadListener = context
                .mock(ItunesDownloadListener.class);
        context.checking(new Expectations() {
            {
                allowing(itunesDownloadListenerFactory).createListener(with(any(Downloader.class)));
                will(returnValue(itunesDownloadListener));

                allowing(unfinishedItem1).getState();
                will(returnValue(Downloader.DownloadState.DOWNLOADING));
                allowing(unfinishedItem2).getState();
                will(returnValue(Downloader.DownloadState.CONNECTING));
                allowing(finishedItem1).getState();
                will(returnValue(Downloader.DownloadState.COMPLETE));
                allowing(finishedItem2).getState();
                will(returnValue(Downloader.DownloadState.COMPLETE));

                allowing(unfinishedItem1).addListener(with(any(EventListener.class)));
                allowing(unfinishedItem2).addListener(with(any(EventListener.class)));
                allowing(finishedItem1).addListener(with(any(EventListener.class)));
                allowing(finishedItem2).addListener(with(any(EventListener.class)));

                allowing(unfinishedItem1).setAttribute(with(equal(DownloadItem.DOWNLOAD_ITEM)),
                        with(any(DownloadItem.class)), with(equal(false)));
                allowing(unfinishedItem2).setAttribute(with(equal(DownloadItem.DOWNLOAD_ITEM)),
                        with(any(DownloadItem.class)), with(equal(false)));
                allowing(finishedItem1).setAttribute(with(equal(DownloadItem.DOWNLOAD_ITEM)),
                        with(any(DownloadItem.class)), with(equal(false)));
                allowing(finishedItem2).setAttribute(with(equal(DownloadItem.DOWNLOAD_ITEM)),
                        with(any(DownloadItem.class)), with(equal(false)));

                allowing(unfinishedItem1).getSha1Urn();
                will(returnValue(urn1));
                allowing(unfinishedItem2).getSha1Urn();
                will(returnValue(urn2));
                allowing(finishedItem1).getSha1Urn();
                will(returnValue(urn3));
                allowing(finishedItem2).getSha1Urn();
                will(returnValue(urn4));
            }
        });

        EventList<DownloadItem> downloads = coreDownloadListManager.getDownloads();

        downloadListener.get().downloadAdded(unfinishedItem1);
        downloadListener.get().downloadAdded(unfinishedItem2);
        downloadListener.get().downloadAdded(finishedItem1);
        downloadListener.get().downloadAdded(finishedItem2);
        coreDownloadListManager.clearFinished();

        assertEquals(2, downloads.size());
        assertTrue(coreDownloadListManager.contains(new URNImpl(urn1)));
        assertTrue(coreDownloadListManager.contains(new URNImpl(urn2)));

        assertFalse(coreDownloadListManager.contains(new URNImpl(urn3)));
        assertFalse(coreDownloadListManager.contains(new URNImpl(urn4)));

        assertNotNull(coreDownloadListManager.getDownloadItem(new URNImpl(urn1)));
        assertNotNull(coreDownloadListManager.getDownloadItem(new URNImpl(urn2)));
        assertNull(coreDownloadListManager.getDownloadItem(new URNImpl(urn3)));
        assertNull(coreDownloadListManager.getDownloadItem(new URNImpl(urn4)));

        context.assertIsSatisfied();
    }

    /**
     * Tests that the Downloads completed event fires only when there are no
     * downloads in progress.
     */
    public void testUpdateDownloadsCompleted() throws Exception {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final DownloadManager downloadManager = context.mock(DownloadManager.class);
        final DownloadListenerList listenerList = context.mock(DownloadListenerList.class);
        final ScheduledExecutorService backgroundExecutor = context
                .mock(ScheduledExecutorService.class);
        final RemoteFileDescFactory remoteFileDescFactory = context
                .mock(RemoteFileDescFactory.class);
        final ActivityCallback activityCallback = context.mock(ActivityCallback.class);
        final SpamManager spamManager = context.mock(SpamManager.class);
        final ItunesDownloadListenerFactory itunesDownloadListenerFactory = context
                .mock(ItunesDownloadListenerFactory.class);

        context.checking(new Expectations() {
            {
                one(listenerList).addDownloadListener(with(any(DownloadListener.class)));
                one(backgroundExecutor).scheduleAtFixedRate(with(any(Runnable.class)),
                        with(any(long.class)), with(any(long.class)), with(any(TimeUnit.class)));
                one(downloadManager).downloadsInProgress();
                will(returnValue(0));
            }
        });

        CoreDownloadListManager coreDownloadListManager = new CoreDownloadListManager(
                downloadManager, listenerList, backgroundExecutor, remoteFileDescFactory,
                activityCallback, spamManager, itunesDownloadListenerFactory);

        TestPropertyChangeListener listener = new TestPropertyChangeListener();
        coreDownloadListManager.addPropertyChangeListener(listener);
        coreDownloadListManager.updateDownloadsCompleted();

        assertEquals(1, listener.getEventCount());
        assertEquals(CoreDownloadListManager.DOWNLOADS_COMPLETED, listener.getLatestEvent()
                .getPropertyName());
        assertEquals(Boolean.TRUE, listener.getLatestEvent().getNewValue());

        coreDownloadListManager.removePropertyChangeListener(listener);

        context.checking(new Expectations() {
            {
                one(downloadManager).downloadsInProgress();
                will(returnValue(2));
            }
        });

        listener = new TestPropertyChangeListener();
        coreDownloadListManager.addPropertyChangeListener(listener);
        coreDownloadListManager.updateDownloadsCompleted();

        assertEquals(0, listener.getEventCount());
        assertNull(listener.getLatestEvent());
        
        context.assertIsSatisfied();
    }
}
