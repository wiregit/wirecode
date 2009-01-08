package org.limewire.listener;

public class CachingPendingEventMulticaster<E> extends PendingEventMulticasterImpl<E> implements EventBean<E> {
    
    private final EventBean<E> bean;
    
    public CachingPendingEventMulticaster(CachingEventMulticaster<E> cacheMulticaster) {
        super(cacheMulticaster);
        this.bean = cacheMulticaster;
    }

    @Override
    public E getLastEvent() {
        return bean.getLastEvent();
    }

}
