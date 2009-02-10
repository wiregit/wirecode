package org.limewire.core.impl.download;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.atomic.AtomicInteger;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.core.api.download.DownloadState;
import org.limewire.listener.EventListener;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.Downloader;

public class CoreDownloadItemTest extends BaseTestCase {

    public CoreDownloadItemTest(String name) {
        super(name);
    }

    @SuppressWarnings("unchecked")
    public void testCancel() {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final Downloader downloader = context.mock(Downloader.class);
        final QueueTimeCalculator queueTimeCalculator = context.mock(QueueTimeCalculator.class);

        context.checking(new Expectations() {
            {
                one(downloader).addListener(with(any(EventListener.class)));
            }
        });
        CoreDownloadItem coreDownloadItem = new CoreDownloadItem(downloader, queueTimeCalculator);
        TestPropertyChangeListener listener = new TestPropertyChangeListener();
        coreDownloadItem.addPropertyChangeListener(listener);

        context.checking(new Expectations() {
            {
                one(downloader).stop(true);
                one(downloader).getFile();
                will(returnValue(null));
            }
        });

        assertEquals(0, listener.getEventCount());

        coreDownloadItem.cancel();

        assertEquals(1, listener.getEventCount());
        PropertyChangeEvent propertyChangeEvent = listener.getLatestEvent();
        assertEquals("state", propertyChangeEvent.getPropertyName());
        DownloadState downloadState = (DownloadState) propertyChangeEvent.getNewValue();
        assertEquals(DownloadState.CANCELLED, downloadState);
        context.assertIsSatisfied();

    }

    /**
     * Counts all events sent to the PropertyChangeListener. Stores the latest
     * event in the lastestEvent field.
     */
    private final class TestPropertyChangeListener implements PropertyChangeListener {

        private PropertyChangeEvent latestEvent = null;

        private AtomicInteger eventCount = new AtomicInteger(0);

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            this.latestEvent = evt;
            eventCount.incrementAndGet();
        }

        public PropertyChangeEvent getLatestEvent() {
            return latestEvent;
        }

        public int getEventCount() {
            return eventCount.intValue();
        }
    }
}
