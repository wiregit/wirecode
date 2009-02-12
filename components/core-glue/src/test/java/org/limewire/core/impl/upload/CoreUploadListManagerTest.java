package org.limewire.core.impl.upload;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.util.BaseTestCase;

public class CoreUploadListManagerTest extends BaseTestCase {

    public CoreUploadListManagerTest(String name) {
        super(name);
    }

    /**
     * Ensure the periodic refresher is registered and the manager is
     *  registered as a listener to the upload listeners list.
     */
    public void testRegister() {
        
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        
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
    
}
