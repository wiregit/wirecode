package com.limegroup.gnutella.downloader;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.limewire.util.AssertComparisons;

import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.Downloader.DownloadState;

public class DownloadTestUtils {
    
    private DownloadTestUtils() {}
    
    /** Waits 1 second for the given state. */
    public static void waitForState(Downloader downloader, DownloadState state) throws Exception {
        waitForState(downloader, state, 1, TimeUnit.SECONDS);
    }
    
    /** Waits the duration for the given state. */
    public static void waitForState(Downloader downloader, DownloadState state, long timeout, TimeUnit unit) throws Exception {
        timeout = unit.toMillis(timeout);
        while(timeout > 0 && downloader.getState() != state) {
            long now = System.currentTimeMillis();
            Thread.sleep(50);
            timeout -= System.currentTimeMillis() - now;
        }
    }
    
    /** Waits for the given state, only allowing the allowed states while waiting. */
    public static void strictWaitForState(Downloader downloader, DownloadState state, DownloadState... allowed) throws Exception {
        strictWaitForState(downloader, state, 1, TimeUnit.SECONDS, allowed);
    }
    
    /** Waits the duration for the given state, only allowing the allowed states while waiting. */
    public static void strictWaitForState(Downloader downloader, DownloadState state, long timeout, TimeUnit unit, DownloadState... allowed) throws Exception {
        timeout = unit.toMillis(timeout);
        while(timeout > 0 && downloader.getState() != state) {
            DownloadState current = downloader.getState();
            boolean match = false;
            for(int i = 0; i < allowed.length; i++) {
                if(allowed[i] == current) {
                    match = true;
                    break;
                }
            }
            if(!match) {
                AssertComparisons.fail("Current state: " + current + ", not in allowed states: " + Arrays.asList(allowed)); 
            }
            
            long now = System.currentTimeMillis();
            Thread.sleep(200);
            timeout -= System.currentTimeMillis() - now;
        }
    }

}
