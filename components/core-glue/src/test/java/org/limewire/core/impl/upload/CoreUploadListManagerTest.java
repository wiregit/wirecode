package org.limewire.core.impl.upload;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.settings.SharingSettings;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.Uploader.UploadStatus;
import com.limegroup.gnutella.uploader.UploadType;

public class CoreUploadListManagerTest extends BaseTestCase {

    public CoreUploadListManagerTest(String name) {
        super(name);
    }

    /**
     * Ensure the periodic refresher is registered and the manager is
     *  registered as a listener to the upload listeners list.
     */
    public void testRegister() {
        Mockery context = new Mockery();
        
        final UploadListenerList uploadListenerList = context.mock(UploadListenerList.class);
        final ScheduledExecutorService backgroundExecutor = context.mock(ScheduledExecutorService.class);
        
        final CoreUploadListManager manager = new CoreUploadListManager(null);
        
        context.checking(new Expectations() {
            {   exactly(1).of(backgroundExecutor).scheduleAtFixedRate(with(any(Runnable.class)), 
                    with(any(Integer.class)), with(any(Integer.class)), with(any(TimeUnit.class)));
            
                exactly(1).of(uploadListenerList).addUploadListener(manager);
            }
        });
        
        manager.register(uploadListenerList, backgroundExecutor);
    }
    
    /** 
     * Add a browse host upload to the manager, ensure it is accessible,
     *  signal completion, and ensure it is removed
     */
    public void testUploaderAddAndRemove() {
        
        Mockery context = new Mockery();
        
        final Uploader uploader = context.mock(Uploader.class);
        
        final long testSize = 777;
        
        final CoreUploadListManager manager = new CoreUploadListManager(null);
                
        context.checking(new Expectations() {
            {   allowing(uploader).getUploadType();
                will(returnValue(UploadType.BROWSE_HOST));
                
                allowing(uploader).getState();
                will(returnValue(UploadStatus.COMPLETE));
                
                // Assertions
                exactly(1).of(uploader).getFileSize();
                will(returnValue(testSize));
            }
        });
        
        List<UploadItem> items = manager.getUploadItems();
        
        manager.uploadAdded(uploader);
        assertEquals(testSize, items.get(0).getFileSize());
        manager.uploadRemoved(uploader);
        assertEmpty(items);
        
        context.assertIsSatisfied();
        
    }
    
    /**
     * Test various conditions where adding and removing an uploader will have no effect.
     * 
     * Add an internal uploader, remove an incomplete upload, remove an upload with the 
     *  setting disabled. 
     */
    public void testUploaderAddAndRemoveBlocked() {
        
        Mockery context = new Mockery();
        
        final Uploader uploaderInternal = context.mock(Uploader.class);
        final Uploader uploaderNotComplete = context.mock(Uploader.class);
        final Uploader uploaderSettingDisabled = context.mock(Uploader.class);
        
        final CoreUploadListManager manager = new CoreUploadListManager(null);
                
        context.checking(new Expectations() {
            {   
                allowing(uploaderInternal).getUploadType();
                will(returnValue(UploadType.MALFORMED_REQUEST));
                
                allowing(uploaderNotComplete).getState();
                will(returnValue(UploadStatus.UPLOADING));
                
                allowing(uploaderSettingDisabled).getState();
                will(returnValue(UploadStatus.COMPLETE));
                
                allowing(uploaderSettingDisabled).getUploadType();
                will(returnValue(UploadType.BROWSE_HOST));
                
            }});
     
        List<UploadItem> items = manager.getUploadItems();
        
        manager.uploadAdded(uploaderInternal);
        assertEmpty(items);
        
        items.add(new CoreUploadItem(uploaderNotComplete));
        manager.uploadRemoved(uploaderNotComplete);
        assertGreaterThan(-1, items.size());   // Can't use assertNotEmpty() since it calls toString() on elements!
        items.clear();
        
        boolean clearUploadOriginal = SharingSettings.CLEAR_UPLOAD.getValue();
        SharingSettings.CLEAR_UPLOAD.setValue(false);
        items.add(new CoreUploadItem(uploaderSettingDisabled));
        manager.uploadRemoved(uploaderSettingDisabled);
        SharingSettings.CLEAR_UPLOAD.setValue(clearUploadOriginal);
        assertGreaterThan(-1, items.size());   // Can't use assertNotEmpty() since it calls toString() on elements!
        
        context.assertIsSatisfied();
    }
    
    /**
     * Add a property change listener and trigger an event then verify it is handled, remove the 
     * listener then verify that it is not notified.
     */
    public void testPropertyChangeListenerUpdates() {
        
        Mockery context = new Mockery();
        
        final PropertyChangeListener listener1 = context.mock(PropertyChangeListener.class);
        final PropertyChangeListener listener2 = context.mock(PropertyChangeListener.class);
        
        final CoreUploadListManager manager = new CoreUploadListManager(null);
        
        context.checking(new Expectations() {
            {   
                exactly(1).of(listener1).propertyChange(with(any(PropertyChangeEvent.class)));
                exactly(2).of(listener2).propertyChange(with(any(PropertyChangeEvent.class)));
                
            }});
        
        manager.addPropertyChangeListener(listener1);
        manager.addPropertyChangeListener(listener2);
        manager.uploadsCompleted();
        
        manager.removePropertyChangeListener(listener1);
        manager.uploadsCompleted();
    }
}
