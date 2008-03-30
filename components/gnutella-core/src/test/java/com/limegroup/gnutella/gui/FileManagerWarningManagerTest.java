package com.limegroup.gnutella.gui;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

import junit.framework.Test;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.setting.BooleanSetting;

import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.FileManagerEvent;
import com.limegroup.gnutella.gui.notify.Notification;
import com.limegroup.gnutella.gui.notify.NotifyUser;
import com.limegroup.gnutella.settings.SharingSettings;

public class FileManagerWarningManagerTest extends GUIBaseTestCase {
    
    private Mockery context;
    
    private FileManagerWarningManager fileManagerWarningManager;
    private NotifyUser notifier;
    private FileManager fileManager;
    
    private FileManagerEvent addEvent;
    private FileManagerEvent removeEvent;
    private FileManagerEvent loadedEvent;
    private FileManagerEvent randomEvent;

    private BooleanSetting numberSwitch;
    private BooleanSetting depthSwitch;
    
    public FileManagerWarningManagerTest(String name) {
        super(name);
    }
    
    public static Test suite() { 
        return buildTestSuite(FileManagerWarningManagerTest.class); 
    }
    
    @Override
    public void setUp() throws Exception{
        // Force start the swing thread
        SwingUtilities.invokeAndWait(new Runnable() { public void run() {} });
        
        context      = new Mockery();
        notifier     = context.mock(NotifyUser.class);
        fileManager  = context.mock(FileManager.class);
        
        numberSwitch = context.mock(BooleanSetting.class);
        depthSwitch  = context.mock(BooleanSetting.class);
        
        fileManagerWarningManager = new FileManagerWarningManager(notifier, numberSwitch, depthSwitch);
        
        addEvent = new FileManagerEvent(fileManager, FileManagerEvent.Type.ADD_FILE);
        removeEvent = new FileManagerEvent(fileManager, FileManagerEvent.Type.REMOVE_FILE);
        loadedEvent =  new FileManagerEvent(fileManager, FileManagerEvent.Type.ADD_FILE);
        randomEvent =  new FileManagerEvent(fileManager, FileManagerEvent.Type.REMOVE_STORE_FOLDER);
    }
    
    
    public void testNoWarning() {
        context.checking(new Expectations() {{
            allowing(numberSwitch).getValue();
            will(returnValue(false));
            allowing(depthSwitch).getValue();
            will(returnValue(false));
            never(notifier);
            exactly(4).of(fileManager).getNumFiles();
            will(returnValue(5));
        }});
            
        fileManagerWarningManager.handleFileEvent(addEvent);    // getNumFiles()  
        fileManagerWarningManager.handleFileEvent(randomEvent);
        fileManagerWarningManager.handleFileEvent(randomEvent);
        fileManagerWarningManager.handleFileEvent(addEvent);    // getNumFiles()
        fileManagerWarningManager.handleFileEvent(loadedEvent); // getNumFiles()
        fileManagerWarningManager.handleFileEvent(addEvent);    // getNumFiles()
        
        context.assertIsSatisfied();
    }
    
    private class NotificationMatcher extends BaseMatcher<Notification> {

        CountDownLatch latch;
        
        public NotificationMatcher(int count) {
            this.latch = new CountDownLatch(count);
        }
        
        public boolean matches(Object item) {
            this.latch.countDown();
            return true;
        }

        public void describeTo(Description description) {
        }

    }
        
    public void testCountWarningNormal() throws Exception {
        final NotificationMatcher matcher = new NotificationMatcher(1);
        context.checking(new Expectations() {{
            allowing(numberSwitch).getValue();
            will(returnValue(false));
            allowing(depthSwitch).getValue();
            will(returnValue(false));
            one(fileManager).getNumFiles();
            will(returnValue(SharingSettings.FILES_FOR_WARNING.getValue()));
            one(notifier).showMessage(with(matcher));
        }});
            
        fileManagerWarningManager.handleFileEvent(addEvent);  
        
        assertTrue("Notification Time Out!", matcher.latch.await(2, TimeUnit.SECONDS));
        
        // TODO: Resolve intermittent failures with expectation assertion
        // context.assertIsSatisfied();
    }
    
    public void testCountWarningInterrupted() throws Exception {
        final NotificationMatcher matcher = new NotificationMatcher(1);
        context.checking(new Expectations() {{
            allowing(numberSwitch).getValue();
            will(returnValue(false));
            allowing(depthSwitch).getValue();
            will(returnValue(false));
            one(fileManager).getNumFiles();
            will(returnValue(5));
            one(fileManager).getNumFiles();
            will(returnValue(SharingSettings.FILES_FOR_WARNING.getValue()));
            exactly(1).of(notifier).showMessage(with(matcher));
        }});
        
        fileManagerWarningManager.handleFileEvent(removeEvent);
        fileManagerWarningManager.handleFileEvent(addEvent);
        fileManagerWarningManager.handleFileEvent(removeEvent);
        fileManagerWarningManager.handleFileEvent(addEvent);
        
        assertTrue("Notification Time Out!", matcher.latch.await(2, TimeUnit.SECONDS));
        
        context.assertIsSatisfied();
    }
    
    public void testCountWarningExtra() throws Exception {
        final NotificationMatcher matcher = new NotificationMatcher(2);
        context.checking(new Expectations() {{
            allowing(numberSwitch).getValue();
            will(returnValue(false));
            allowing(depthSwitch).getValue();
            will(returnValue(false));
            exactly(2).of(fileManager).getNumFiles();
            will(returnValue(SharingSettings.FILES_FOR_WARNING.getValue()));
            exactly(2).of(notifier).showMessage(with(matcher));
        }});
        
        fileManagerWarningManager.handleFileEvent(addEvent);
        fileManagerWarningManager.handleFileEvent(addEvent);
        
        assertTrue("Notification Time Out!", matcher.latch.await(2, TimeUnit.SECONDS));
        
        context.assertIsSatisfied();
    }
    
    public void testCountWarningIgnored() {
        context.checking(new Expectations() {{
            allowing(numberSwitch).getValue();
            will(returnValue(true));
            allowing(depthSwitch).getValue();
            will(returnValue(false));
            exactly(2).of(fileManager).getNumFiles();
            will(returnValue(SharingSettings.FILES_FOR_WARNING.getValue()));
            never(notifier);
        }});
        
        fileManagerWarningManager.handleFileEvent(addEvent);
        fileManagerWarningManager.handleFileEvent(addEvent);
        
        context.assertIsSatisfied();
    }
 
    
    
    
    
    private FileManagerEvent createAddFolderEvent(int depth) {
        return new FileManagerEvent(fileManager,FileManagerEvent.Type.ADD_FOLDER,null,depth);
    }
    
    
    
    public void testDepthWarningNormal() throws Exception {
        final NotificationMatcher matcher = new NotificationMatcher(1);
        context.checking(new Expectations() {{
            allowing(numberSwitch).getValue();
            will(returnValue(false));
            allowing(depthSwitch).getValue();
            will(returnValue(false));
            never(fileManager);
            one(notifier).showMessage(with(matcher));
        }});
            
        fileManagerWarningManager.handleFileEvent(
                createAddFolderEvent(SharingSettings.DEPTH_FOR_WARNING.getValue()));  
        
        assertTrue("Notification Time Out!", matcher.latch.await(2, TimeUnit.SECONDS));
        
        context.assertIsSatisfied();
    }
    
    public void testDepthWarningInterrupted() throws Exception {
        final NotificationMatcher matcher = new NotificationMatcher(1);
        context.checking(new Expectations() {{
            allowing(numberSwitch).getValue();
            will(returnValue(false));
            allowing(depthSwitch).getValue();
            will(returnValue(false));
            never(fileManager);
            exactly(1).of(notifier).showMessage(with(matcher));
        }});
        
        
        fileManagerWarningManager.handleFileEvent(removeEvent);
        fileManagerWarningManager.handleFileEvent(
                createAddFolderEvent(1));
        fileManagerWarningManager.handleFileEvent(randomEvent);
        fileManagerWarningManager.handleFileEvent(
                createAddFolderEvent(SharingSettings.DEPTH_FOR_WARNING.getValue())); 
        
        assertTrue("Notification Time Out!", matcher.latch.await(2, TimeUnit.SECONDS));
        
        context.assertIsSatisfied();
    }
    
    // Only Show one warning
    public void testDepthWarningExtra() throws Exception {
        final NotificationMatcher matcher = new NotificationMatcher(1);
        context.checking(new Expectations() {{
            allowing(numberSwitch).getValue();
            will(returnValue(false));
            allowing(depthSwitch).getValue();
            will(returnValue(false));
            never(fileManager);
            exactly(1).of(notifier).showMessage(with(matcher));
        }});
        
        
        fileManagerWarningManager.handleFileEvent(
                createAddFolderEvent(SharingSettings.DEPTH_FOR_WARNING.getValue())); 
        fileManagerWarningManager.handleFileEvent(
                createAddFolderEvent(SharingSettings.DEPTH_FOR_WARNING.getValue())); 
        
        assertTrue("Notification Time Out!", matcher.latch.await(2, TimeUnit.SECONDS));
        
        context.assertIsSatisfied();
    }
    
    public void testDepthWarningIgnored() {
        context.checking(new Expectations() {{
            allowing(numberSwitch).getValue();
            will(returnValue(false));
            allowing(depthSwitch).getValue();
            will(returnValue(true));
            never(fileManager);
            never(notifier);
        }});
        
        fileManagerWarningManager.handleFileEvent(
                createAddFolderEvent(SharingSettings.DEPTH_FOR_WARNING.getValue())); 
        fileManagerWarningManager.handleFileEvent(
                createAddFolderEvent(SharingSettings.DEPTH_FOR_WARNING.getValue())); 
        
        context.assertIsSatisfied();
    }
    
    
    
    
   

    
    
    
    public void testDepthAndCountWarning() throws Exception {
        final NotificationMatcher matcher = new NotificationMatcher(2);
        context.checking(new Expectations() {{
            allowing(numberSwitch).getValue();
            will(returnValue(false));
            allowing(depthSwitch).getValue();
            will(returnValue(false));
            one(fileManager).getNumFiles();
            will(returnValue(SharingSettings.FILES_FOR_WARNING.getValue()));
            exactly(2).of(notifier).showMessage(with(matcher));
        }});
        
        fileManagerWarningManager.handleFileEvent(addEvent);
        fileManagerWarningManager.handleFileEvent(
                createAddFolderEvent(SharingSettings.DEPTH_FOR_WARNING.getValue())); 
        
        assertTrue("Notification Time Out!", matcher.latch.await(2, TimeUnit.SECONDS));
        
        context.assertIsSatisfied();
    }
    
   
}
