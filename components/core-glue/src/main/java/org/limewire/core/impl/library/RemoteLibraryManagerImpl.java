package org.limewire.core.impl.library;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.inspection.DataCategory;
import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectableContainer;
import org.limewire.inspection.InspectionPoint;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.util.concurrent.LockFactory;
import ca.odell.glazedlists.util.concurrent.ReadWriteLock;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * This class keeps track of all friends libraries. As friend presences are found they are 
 * aggregated into a single friend library per friend. All RemoteFileItems found in the friend libraries
 * are also coalesced into a single FileList.
 */
@Singleton
public class RemoteLibraryManagerImpl implements RemoteLibraryManager {
    
    private final EventList<FriendLibrary> allFriendLibraries;
    private final EventList<FriendLibrary> readOnlyFriendLibraries;

    /**
     * Common lock for all glazed lists in here.
     */
    private final ReadWriteLock listLock = LockFactory.DEFAULT.createReadWriteLock();

    @SuppressWarnings("unused")
    @InspectableContainer
    private class LazyInspectableContainer {
        @InspectionPoint(value = "remote libraries", category = DataCategory.USAGE)
        private final Inspectable inspectable = new Inspectable() {
            @Override
            public Object inspect() {
                Map<String, Object> data = new HashMap<String, Object>();
                listLock.readLock().lock();
                try {
                    List<Integer> sizes = new ArrayList<Integer>(readOnlyFriendLibraries.size());
                    for (FriendLibrary friendLibrary : readOnlyFriendLibraries) {
                        sizes.add(friendLibrary.size());
                    }
                    data.put("sizes", sizes);
                } finally {
                    listLock.readLock().unlock();
                }
                return data;
            }
        };
    }
    
    @Inject
    public RemoteLibraryManagerImpl() {
        allFriendLibraries = GlazedListsFactory.threadSafeList(new BasicEventList<FriendLibrary>(listLock));
        readOnlyFriendLibraries = GlazedListsFactory.readOnlyList(allFriendLibraries); 
    }
    
    @Override
    public EventList<FriendLibrary> getFriendLibraryList() {
        return allFriendLibraries;
    }
}
