package org.limewire.core.impl.upload;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.CategoryConverter;
import com.limegroup.gnutella.Uploader;

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
    }
}
