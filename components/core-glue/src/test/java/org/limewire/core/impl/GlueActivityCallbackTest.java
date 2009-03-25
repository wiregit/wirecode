package org.limewire.core.impl;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.api.callback.GuiCallback;
import org.limewire.core.impl.download.DownloadListener;
import org.limewire.core.impl.upload.UploadListener;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.Uploader;

/**
 * Tests methods in {@link GlueActivityCallback} for functionality and linkage.
 */
public class GlueActivityCallbackTest extends BaseTestCase {

    public GlueActivityCallbackTest(String name) {
        super(name);
    }
    
    /**
     * Adds and removes an {@link UploadListener} ensures events are and are not fired
     *  according to the situation.
     */
    public void testUploadListenerLinkage() {
        Mockery context = new Mockery();
        
        final UploadListener listener1 = context.mock(UploadListener.class);
        final UploadListener listener2 = context.mock(UploadListener.class);
        final UploadListener listener3 = context.mock(UploadListener.class);
        
        final Uploader uploaderA = context.mock(Uploader.class);
        
        GlueActivityCallback activityCallback = new GlueActivityCallback(null, null);
        
        context.checking(new Expectations() {{
            exactly(1).of(listener1).uploadAdded(uploaderA);
            exactly(1).of(listener2).uploadAdded(uploaderA);
            exactly(1).of(listener3).uploadAdded(uploaderA);
            
            exactly(1).of(listener1).uploadRemoved(uploaderA);
            
            exactly(1).of(listener1).uploadsCompleted();
            exactly(1).of(listener2).uploadsCompleted();
        }});
        
        activityCallback.addUploadListener(listener1);
        activityCallback.addUploadListener(listener2);
        activityCallback.addUploadListener(listener3);
        
        activityCallback.addUpload(uploaderA);
        
        activityCallback.removeUploadListener(listener2);
        activityCallback.removeUploadListener(listener3);
        
        activityCallback.removeUpload(uploaderA);
        
        activityCallback.addUploadListener(listener2);
            
        activityCallback.uploadsComplete();
        
        context.assertIsSatisfied();
    }
    

    /**
     * Adds and removes an {@link DownloadListener} ensures events are and are not fired
     *  according to the situation.
     */
    public void testDownloadListenerLinkage() {
        Mockery context = new Mockery();
        
        final DownloadListener listener1 = context.mock(DownloadListener.class);
        final DownloadListener listener2 = context.mock(DownloadListener.class);
        final DownloadListener listener3 = context.mock(DownloadListener.class);
        
        final Downloader downloaderA = context.mock(Downloader.class);
        
        GlueActivityCallback activityCallback = new GlueActivityCallback(null, null);
        
        context.checking(new Expectations() {{
            exactly(1).of(listener1).downloadAdded(downloaderA);
            exactly(1).of(listener2).downloadAdded(downloaderA);
            exactly(1).of(listener3).downloadAdded(downloaderA);
            
            exactly(1).of(listener1).downloadRemoved(downloaderA);
            
            exactly(1).of(listener1).downloadsCompleted();
            exactly(1).of(listener2).downloadsCompleted();
        }});
        
        activityCallback.addDownloadListener(listener1);
        activityCallback.addDownloadListener(listener2);
        activityCallback.addDownloadListener(listener3);
        
        activityCallback.addDownload(downloaderA);
        
        activityCallback.removeDownloadListener(listener2);
        activityCallback.removeDownloadListener(listener3);
        
        activityCallback.removeDownload(downloaderA);
        
        activityCallback.addDownloadListener(listener2);
            
        activityCallback.downloadsComplete();
        
        context.assertIsSatisfied();
    }
    
    /**
     * Test the restoreApplication method with and without the callback set.
     */
    public void testRestoreApplication() {
        Mockery context = new Mockery();
        
        final GuiCallback callback = context.mock(GuiCallback.class);
        
        GlueActivityCallback activityCallback = new GlueActivityCallback(null, null);
        
        context.checking(new Expectations() {{
            exactly(1).of(callback).restoreApplication();
        }});
        
        activityCallback.restoreApplication();
        activityCallback.setGuiCallback(callback);
        activityCallback.restoreApplication();
        
        context.assertIsSatisfied();
    }
    
    /**
     * Test the dangerousDownloadDeleted method with and without the callback set.
     */
    public void testDangeroudDownloadDeleted() {
        Mockery context = new Mockery();
        
        final GuiCallback callback = context.mock(GuiCallback.class);
        
        GlueActivityCallback activityCallback = new GlueActivityCallback(null, null);
        
        context.checking(new Expectations() {{
            exactly(1).of(callback).dangerousDownloadDeleted("file");
        }});
        
        activityCallback.dangerousDownloadDeleted("file");
        activityCallback.setGuiCallback(callback);
        activityCallback.dangerousDownloadDeleted("file");
        
        context.assertIsSatisfied();
    }
    
}
