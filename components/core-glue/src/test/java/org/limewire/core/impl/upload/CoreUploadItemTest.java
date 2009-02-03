package org.limewire.core.impl.upload;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.CategoryConverter;
import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.Uploader.UploadStatus;

public class CoreUploadItemTest extends BaseTestCase {
    
    public CoreUploadItemTest(String name) {
        super(name);
    }

    public void testGetCategory() {
        Mockery context = new Mockery();
        
        final Uploader uploader = context.mock(Uploader.class);
        
        context.checking(new Expectations() {
            {
                allowing(uploader).getFileName();
                will(returnValue("thing.bmp"));
            }});
        
        CoreUploadItem upload = new CoreUploadItem(uploader);
        
        assertEquals(upload.getCategory(), CategoryConverter.categoryForExtension("bmp"));
        context.assertIsSatisfied();
    }
    
    public void testCancel() {
        Mockery context = new Mockery();
        
        final Uploader uploader = context.mock(Uploader.class);
        
        context.checking(new Expectations() {
            {
                exactly(1).of(uploader).stop();
                
                allowing(uploader).getState();
                will(returnValue(UploadStatus.UPLOADING));
            }});
        
        CoreUploadItem upload = new CoreUploadItem(uploader);
        
        upload.cancel();
                
        context.assertIsSatisfied();
    }
    
}
