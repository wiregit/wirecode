package org.limewire.core.impl.download;

import java.beans.PropertyChangeListener;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.core.api.spam.SpamManager;
import org.limewire.core.impl.download.listener.ItunesDownloadListenerFactory;
import org.limewire.util.BaseTestCase;

import ca.odell.glazedlists.EventList;

import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;

public class CoreDownloadListManagerTest extends BaseTestCase {

    public CoreDownloadListManagerTest(String name) {
        super(name);
    }

    public void testClearFinished() {
        Mockery context = new Mockery();

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
            }
        });

        CoreDownloadListManager coreDownloadListManager = new CoreDownloadListManager(
                downloadManager, listenerList, backgroundExecutor, remoteFileDescFactory,
                activityCallback, spamManager, itunesDownloadListenerFactory);

        final DownloadItem unfinishedItem1 = context.mock(DownloadItem.class);
        final DownloadItem unfinishedItem2 = context.mock(DownloadItem.class);
        final DownloadItem finishedItem1 = context.mock(DownloadItem.class);
        final DownloadItem finishedItem2 = context.mock(DownloadItem.class);

        context.checking(new Expectations() {
            {
                allowing(unfinishedItem1).getState();
                will(returnValue(DownloadState.DOWNLOADING));
                allowing(unfinishedItem2).getState();
                will(returnValue(DownloadState.CONNECTING));
                allowing(finishedItem1).getState();
                will(returnValue(DownloadState.DONE));
                allowing(finishedItem2).getState();
                will(returnValue(DownloadState.DONE));

                allowing(unfinishedItem1).addPropertyChangeListener(
                        with(any(PropertyChangeListener.class)));
                allowing(unfinishedItem2).addPropertyChangeListener(
                        with(any(PropertyChangeListener.class)));
                allowing(finishedItem1).addPropertyChangeListener(
                        with(any(PropertyChangeListener.class)));
                allowing(finishedItem2).addPropertyChangeListener(
                        with(any(PropertyChangeListener.class)));

                allowing(unfinishedItem1).removePropertyChangeListener(
                        with(any(PropertyChangeListener.class)));
                allowing(unfinishedItem2).removePropertyChangeListener(
                        with(any(PropertyChangeListener.class)));
                allowing(finishedItem1).removePropertyChangeListener(
                        with(any(PropertyChangeListener.class)));
                allowing(finishedItem2).removePropertyChangeListener(
                        with(any(PropertyChangeListener.class)));
            }
        });

        EventList<DownloadItem> downloads = coreDownloadListManager.getDownloads();

        downloads.add(unfinishedItem1);
        downloads.add(unfinishedItem2);
        downloads.add(finishedItem1);
        downloads.add(finishedItem2);
        coreDownloadListManager.clearFinished();

        assertEquals(2, downloads.size());
        assertContains(downloads, unfinishedItem1);
        assertContains(downloads, unfinishedItem2);
    }
}
