package org.limewire.nio;

public class NIOTestUtils {

    /**
     * Waits for the NIO dispatcher to complete a processing pending events.
     */
    public static void waitForNIO() throws InterruptedException {
        NIODispatcher.instance().invokeAndWait(new Runnable() {
            public void run() {
            }
        });
        // the runnable is run at the beginning of the processing cycle so 
        // we need a second runnable to make sure the cycle has been completed
        NIODispatcher.instance().invokeAndWait(new Runnable() {
            public void run() {
            }
        });
    }

}
