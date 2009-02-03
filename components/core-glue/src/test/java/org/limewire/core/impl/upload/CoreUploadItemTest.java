package org.limewire.core.impl.upload;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.api.upload.UploadItem.BrowseType;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.CategoryConverter;
import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.Uploader.UploadStatus;
import com.limegroup.gnutella.uploader.UploadType;

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
        final PropertyChangeListener changeListener = context.mock(PropertyChangeListener.class);
 
        context.checking(new Expectations() {
            {
                exactly(1).of(uploader).stop();
                
                atLeast(1).of(uploader).getState();
                will(returnValue(UploadStatus.CANCELLED));
                
                exactly(1).of(changeListener).propertyChange(with(any(PropertyChangeEvent.class)));
            }});
        
        CoreUploadItem upload = new CoreUploadItem(uploader);
        
        upload.addPropertyChangeListener(changeListener);
                
        upload.cancel();
                
        context.assertIsSatisfied();
    }
    
    /**
     * equals() is overridden to check if uploaders are equal
     */
    public void testEquals() {
        Mockery context = new Mockery();
        
        final Uploader uploader1 = context.mock(Uploader.class);
        final Uploader uploader2 = context.mock(Uploader.class);
        
        context.checking(new Expectations() {
            { 
                allowing(uploader1);
                allowing(uploader2);
            }});
        
        CoreUploadItem upload1 = new CoreUploadItem(uploader1);
        CoreUploadItem upload2 = new CoreUploadItem(uploader2);
        CoreUploadItem upload3 = new CoreUploadItem(null);
        
        assertTrue(upload1.equals(upload1));
        assertFalse(upload1.equals(upload2));
        assertFalse(upload1.equals(upload3));
        assertFalse(upload2.equals(upload1));
        assertTrue(upload2.equals(upload2));
        assertFalse(upload2.equals(upload3));
        assertFalse(upload3.equals(upload1));
        assertFalse(upload3.equals(upload2));
        assertTrue(upload3.equals(upload3));

        assertFalse(upload1.equals("not equals"));
                
        context.assertIsSatisfied();
    }
    
    public void testRemovePropertyChangeListener() {
        Mockery context = new Mockery();
        
        final Uploader uploader = context.mock(Uploader.class);
        final PropertyChangeListener changeListener = context.mock(PropertyChangeListener.class);
 
        context.checking(new Expectations() {
            {
                exactly(1).of(uploader).stop();
                
                atLeast(1).of(uploader).getState();
                will(returnValue(UploadStatus.CANCELLED));
                
                never(changeListener).propertyChange(with(any(PropertyChangeEvent.class)));
            }});
        
        CoreUploadItem upload = new CoreUploadItem(uploader);
        
        upload.addPropertyChangeListener(changeListener);
        upload.removePropertyChangeListener(changeListener);
                
        upload.cancel();
                
        context.assertIsSatisfied();
    }

    private void testGetHostWithAnyBrowseAndFileName(final UploadStatus type) {
        Mockery context = new Mockery();
        
        final Uploader uploader = context.mock(Uploader.class);
 
        final String fileName = "hello.file";
        
        context.checking(new Expectations() {
            {
                atLeast(1).of(uploader).getState();
                will(returnValue(type));
                atLeast(1).of(uploader).getFileName();
                will(returnValue(fileName));
                
                allowing(uploader).getUploadType();
                will(returnValue(UploadType.BROWSE_HOST));
            }});
        
        CoreUploadItem upload = new CoreUploadItem(uploader);
        
        assertEquals(fileName, upload.getHost());
   
        context.assertIsSatisfied();
    }
    
    public void testGetHostWithBrowseAndFileName() {
        testGetHostWithAnyBrowseAndFileName(UploadStatus.BROWSE_HOST);
    }
    
    public void testGetHostWithDoneBrowseAndFileName() {
        testGetHostWithAnyBrowseAndFileName(UploadStatus.COMPLETE);
    }
    
    // TODO: This needs to be tested but getHost is doing some
    //        whacky stuff and HTTPUploader is not an interface
    /*
    public void testGetHostWithoutFileName() {
        Mockery context = new Mockery();
        
        final Uploader uploader = context.mock(HTTPUploader.class);
 
        final String fileName = "";
        
        context.checking(new Expectations() {
            {
                atLeast(1).of(uploader).getState();
                will(returnValue(UploadStatus.BROWSE_HOST));
                atLeast(1).of(uploader).getFileName();
                will(returnValue(fileName));
                
                allowing(uploader).getUploadType();
                will(returnValue(UploadType.BROWSE_HOST));
            }});
        
        CoreUploadItem upload = new CoreUploadItem(uploader);
        
        System.out.println(upload.getHost());
   
        context.assertIsSatisfied();
    }
    */
    
    public void testGetHostNormal() {
        Mockery context = new Mockery();
        
        final Uploader uploader = context.mock(Uploader.class);
 
        final String host = "www";
        
        context.checking(new Expectations() {
            {
                atLeast(1).of(uploader).getState();
                will(returnValue(UploadStatus.UPLOADING));
                exactly(1).of(uploader).getHost();
                will(returnValue(host));
            }});
        
        CoreUploadItem upload = new CoreUploadItem(uploader);
        
        assertEquals(host, upload.getHost());
   
        context.assertIsSatisfied();
    }
    
    private void testGetBrowseType(final UploadStatus state, final String fileName,
            final BrowseType type) {
        
        Mockery context = new Mockery();
        
        final Uploader uploader = context.mock(Uploader.class);
        
        context.checking(new Expectations() {
            {
                atLeast(1).of(uploader).getState();
                will(returnValue(state));
                allowing(uploader).getFileName();
                will(returnValue(fileName));
               
                if (state == UploadStatus.COMPLETE) {
                    allowing(uploader).getUploadType();
                    will(returnValue(UploadType.BROWSE_HOST));
                }
            }});
        
        CoreUploadItem upload = new CoreUploadItem(uploader);
        
        assertEquals(type, upload.getBrowseType());
   
        context.assertIsSatisfied();        
    }
    
    public void testGetBrowseTypeWithBrowse() {
        testGetBrowseType(UploadStatus.BROWSE_HOST, "asd", BrowseType.FRIEND);
    }
    public void testGetBrowseTypeWithDoneBrowse() {
        testGetBrowseType(UploadStatus.COMPLETE, "asd", BrowseType.FRIEND);
    }
    public void testGetBrowseTypeWithBrowseAndNoFile() {
        testGetBrowseType(UploadStatus.BROWSE_HOST, "", BrowseType.P2P);
    }    
    public void testGetBrowseTypeWithDoneBrowseAndNoFile() {
        testGetBrowseType(UploadStatus.COMPLETE, "", BrowseType.P2P);
    }
    public void testGetBrowseTypeWithOther() {
        testGetBrowseType(UploadStatus.CANCELLED, "", BrowseType.NONE);
        testGetBrowseType(UploadStatus.FILE_NOT_FOUND, "fde", BrowseType.NONE);
        testGetBrowseType(UploadStatus.INTERRUPTED, "", BrowseType.NONE);
        testGetBrowseType(UploadStatus.CONNECTING, "", BrowseType.NONE);
        testGetBrowseType(UploadStatus.THEX_REQUEST, "asd", BrowseType.NONE);
        testGetBrowseType(UploadStatus.LIMIT_REACHED, "asds", BrowseType.NONE);
    }
    
    
}
