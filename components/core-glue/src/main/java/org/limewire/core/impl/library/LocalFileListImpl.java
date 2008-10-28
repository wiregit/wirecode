/**
 * 
 */
package org.limewire.core.impl.library;

import java.awt.EventQueue;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.TransformedList;

abstract class LocalFileListImpl implements LocalFileList {
    protected final EventList<LocalFileItem> baseList;
    protected final TransformedList<LocalFileItem, LocalFileItem> threadSafeList;
    protected final TransformedList<LocalFileItem, LocalFileItem> readOnlyList;
    protected volatile TransformedList<LocalFileItem, LocalFileItem> swingEventList;
    
    LocalFileListImpl(EventList<LocalFileItem> eventList) {
        this.baseList = eventList;
        this.threadSafeList = GlazedListsFactory.threadSafeList(eventList);
        this.readOnlyList = GlazedListsFactory.readOnlyList(threadSafeList);
    }
    
    @Override
    public EventList<LocalFileItem> getModel() {
        return readOnlyList;
    }
    
    @Override
    public EventList<LocalFileItem> getSwingModel() {
        assert EventQueue.isDispatchThread();
        if(swingEventList == null) {
            swingEventList =  GlazedListsFactory.swingThreadProxyEventList(readOnlyList);
        }
        return swingEventList;
    }
    
    void dispose() {
        if(swingEventList != null) {
            swingEventList.dispose();
        }
        threadSafeList.dispose();
        readOnlyList.dispose();
    }       
    
    
    @Override
    public int size() {
        return threadSafeList.size();
    }
}