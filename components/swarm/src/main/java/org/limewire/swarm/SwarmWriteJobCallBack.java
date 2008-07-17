package org.limewire.swarm;



/**
 * A job for writing data.  The job is expected to asynchronously write data
 * to disk.
 */
public interface SwarmWriteJobCallBack {
   
   public void pause();
   
   public void resume();
   
}
