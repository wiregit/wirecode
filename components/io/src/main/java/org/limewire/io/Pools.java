package org.limewire.io;

import java.util.TimerTask;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.limewire.concurrent.SimpleTimer;


/**
 * A collection of pools for commonly used objects.
 */
public class Pools {
    
    private static final Log LOG = LogFactory.getLog(Pools.class);
    
    private static volatile ObjectPool<Deflater> deflaterPool;
    private static volatile ObjectPool<Inflater> inflaterPool;
    
    private Pools() {}
    
    /**
     * Returns a shared ObjectPool for Deflaters.
     * The pool will automatically reset the deflater prior to returning it,
     * and end a deflater after it is returned.
     * 
     * @return
     */
    public static ObjectPool<Deflater> getDeflaterPool() {
        // Attempt to return the pool without locking.
        if(deflaterPool != null) {
            return deflaterPool;
        } else {
            synchronized(Pools.class) {
                if(deflaterPool == null)
                    deflaterPool =  new CustomObjectPool<Deflater>(new DeflaterPoolFactory(), config());
                return deflaterPool;
            }
        }
    }
    
    /**
     * Returns a shared ObjectPool for Inflaters.
     * The pool will automatically reset the inflater prior to returning it,
     * and end a deflater after it is returned.
     * 
     * @return
     */
    public static ObjectPool<Inflater> getInflaterPool() {
        // Attempt to return the pool without locking.
        if(inflaterPool != null) {
            return inflaterPool;
        } else {
            synchronized(Pools.class) {
                if(inflaterPool == null)
                    inflaterPool =  new CustomObjectPool<Inflater>(new InflaterPoolFactory(), config());
                return inflaterPool;
            }
        }
    }
    
    /** Generates a basic configuration. */
    private static GenericObjectPool.Config config() {
        GenericObjectPool.Config config = new GenericObjectPool.Config();
        config.maxActive = 8; // the maximum number of objects that can be borrowed from me at one time
        config.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_GROW; // the action to take when the pool is exhausted 
        config.maxWait = 0; //the maximum amount of time to wait for an idle object when the pool is exhausted an and whenExhaustedAction is WHEN_EXHAUSTED_BLOCK (otherwise ignored) 
        config.maxIdle = 2; // the maximum number of idle objects in my pool
        config.minIdle = 0; // the minimum number of idle objects in my pool
        config.testOnBorrow = true; // whether or not to validate objects before they are returned by the borrowObject method 
        config.testOnReturn = true; // whether or not to validate objects after they are returned to the method
        config.timeBetweenEvictionRunsMillis = 5000; // the amount of time (in milliseconds) to sleep between examining idle objects for eviction
        config.numTestsPerEvictionRun = 10; // the number of idle objects to examine per run within the idle object eviction thread (if any)
        config.testWhileIdle = false; // whether or not to validate objects in the idle object eviction thread, if any
        config.softMinEvictableIdleTimeMillis = 2000; // the minimum number of milliseconds an object can sit idle in the pool before it is eligable for evcition with the extra condition that at least "minIdle" amount of object remain in the pool. 
        return config;
    }
    
    /** Factory for deflaters. */
    private static class DeflaterPoolFactory extends BasePoolableObjectFactory<Deflater> {
        @Override
        public void activateObject(Deflater obj) {
            obj.reset();
        }
        
        @Override
        public void passivateObject(Deflater obj) {
            obj.end();
        }
        
        @Override
        public Deflater makeObject() {
            return new Deflater();
        }
    }
    
    /** Factory for inflaters. */
    private static class InflaterPoolFactory extends BasePoolableObjectFactory<Inflater> {
        @Override
        public void activateObject(Inflater obj) {
            obj.reset();
        }
        
        @Override
        public void passivateObject(Inflater obj) {
            obj.end();
        }
        
        @Override
        public Inflater makeObject() {
            return new Inflater();
        }
    }
    
    /** A custom ObjectPool that evicts using SimpleTimer's scheduler. */
    private static class CustomObjectPool<V> extends GenericObjectPool<V> {
        private Evictor _evictor = null;
        
        public CustomObjectPool(PoolableObjectFactory<V> factory, GenericObjectPool.Config config) {
            super(factory, config);
        }

        @Override
        protected synchronized void startEvictor(long delay) {
            if(null != _evictor) {
                _evictor.cancel();
                _evictor = null;
            }
            if(delay > 0) {
                _evictor = new Evictor();
                SimpleTimer.sharedTimer().schedule(_evictor, delay, delay);
            }
        }
        
        private synchronized int calculateDeficit() {
            int objectDeficit = getMinIdle() - getNumIdle();
            if (getMaxActive() > 0) {
                int growLimit = Math.max(0, getMaxActive() - getNumActive() - getNumIdle());
                objectDeficit = Math.min(objectDeficit, growLimit);
            }
            return objectDeficit;
        }
        
        /**
         * Check to see if we are below our minimum number of objects
         * if so enough to bring us back to our minimum.
         */
        private void ensureMinIdle() throws Exception {
            // this method isn't synchronized so the
            // calculateDeficit is done at the beginning
            // as a loop limit and a second time inside the loop
            // to stop when another thread already returned the
            // needed objects
            int objectDeficit = calculateDeficit();
            for ( int j = 0 ; j < objectDeficit && calculateDeficit() > 0 ; j++ ) {
                addObject();
            }
        }
        
        /**
         * The idle object evictor {@link TimerTask}.
         * @see GenericObjectPool#setTimeBetweenEvictionRunsMillis
         */
        private class Evictor extends TimerTask {
            public void run() {
                try {
                    evict();
                } catch(Exception e) {
                    LOG.warn("Exception while evicting", e);
                }
                try {
                    ensureMinIdle();
                } catch(Exception e) {
                    LOG.warn("Exception while adding min idle", e);
                }
            }
        }
        
    }
}
