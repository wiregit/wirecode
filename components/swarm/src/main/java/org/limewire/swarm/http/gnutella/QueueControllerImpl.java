package org.limewire.swarm.http.gnutella;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.StringTokenizer;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.http.ProtocolException;
import org.apache.http.nio.IOControl;


public class QueueControllerImpl implements QueueController {
    
    private final ScheduledExecutorService queueService;
    private final List<QInfo> queueList = new ArrayList<QInfo>();
    private int maxQueueCapacity = 10;
    
    private final Object LOCK = new Object();
    
    public QueueControllerImpl(ScheduledExecutorService queueService) {
        this.queueService = queueService;
    }
    
    public void removeFromQueue(QueueInfo queueInfo) {
        if(!(queueInfo instanceof QInfo))
            throw new IllegalArgumentException(queueInfo + " not created from this controller");
        
        synchronized(LOCK) {
            queueList.remove(queueInfo);
        }
    }

    public QueueInfo addToQueue(String queueHeader, IOControl ioctrl) throws ProtocolException {
        Integer pos = null, min = null, max = null;
        StringTokenizer tokenizer = new StringTokenizer(queueHeader, ",");
        while(tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            String[] kv = token.split("=", 2);
            if(kv.length >= 2) {
                kv[0] = kv[0].trim();
                kv[1] = kv[1].trim();
                try {
                    if(kv[0].equals("position")) {
                        pos = Integer.parseInt(kv[1]);
                    } else if(kv[0].equals("pollMin")) {
                        min = Integer.parseInt(kv[1]);
                    } else if(kv[0].equals("pollMax")) {
                        max = Integer.parseInt(kv[1]);
                    }
                } catch(NumberFormatException nfe) {
                    throw new ProtocolException("invalid token: " + token, nfe);
                }
            }
            if(pos != null && min != null && max != null)
                break;
        }
        
        if(pos == null || min == null || max == null)
            throw new ProtocolException("invalid header: " + queueHeader);
        
        final QInfo qInfo = new QInfo(ioctrl, pos);
        if(validateMyselfAndMaybeKillLastQueuer(qInfo)) {
            ScheduledFuture<?> future = queueService.schedule(new Runnable() {
                public void run() {
                    qInfo.dequeue();
                }
            }, Math.min(max, min+1), TimeUnit.SECONDS);
            qInfo.setFuture(future);
            return qInfo;
        } else {
            try {
                ioctrl.shutdown();
            } catch(IOException ignored) {}
            return null;
        }
    }
    
    /**
     * Iterates through the existing queued items and determines
     * if this can be added.  If it can be added (or is replacing an earlier
     * version of itself), this returns true.  If it can't be added,
     * this returns false.
     * 
     * If the maxQueueCapacity is exceeded, this may kill a previously
     * queued connection.
     */
    private boolean validateMyselfAndMaybeKillLastQueuer(QInfo qInfo) {
        QInfo lastQueued = qInfo;
        synchronized(LOCK) {
            for(ListIterator<QInfo> qIter = queueList.listIterator(); qIter.hasNext(); ) {
                QInfo queuee = qIter.next();
                // If we're just updating an existing queue slot, leave it this way.
                if(queuee.isForSamePerson(qInfo)) {
                    qIter.set(qInfo);
                    return true;
                }
                
                if(queuee.getPosition() > lastQueued.getPosition()) {
                    lastQueued = queuee;
                }
            }
            
            if(queueList.size() >= maxQueueCapacity) {
                if(lastQueued == qInfo) {
                    return false;
                } else {
                    lastQueued.kill();
                }
            }
            
            queueList.add(qInfo);
        }
        return true;
    }


    private static class QInfo implements QueueInfo {
        private final IOControl ioctrl;
        private final int position;
        private volatile boolean dequeued = false;
        private volatile ScheduledFuture<?> future;
        
        QInfo(IOControl ioctrl, int position) {
            this.ioctrl = ioctrl;
            this.position = position;
        }
        
        int getPosition() {
            return position;
        }
        
        boolean isForSamePerson(QInfo otherQ) {
            return otherQ.ioctrl == this.ioctrl;
        }
        
        void setFuture(ScheduledFuture<?> future) {
            this.future = future;
        }
        
        void kill() {
            future.cancel(false);
            try {
                ioctrl.shutdown();
            } catch(IOException ignored) {}
        }

        public boolean isQueued() {
            return !dequeued;
        }
        
        /** Suspends output if this hasn't already been dequeued. */
        public void enqueue() {
            if(!dequeued)
                ioctrl.suspendOutput();
        }
        
        /** Requests output. */
        public void dequeue() {
            dequeued = true;
            ioctrl.requestOutput();
        }
        
    }
    

}
