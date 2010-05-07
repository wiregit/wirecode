package org.limewire.core.impl.library;

import junit.framework.Test;

import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.impl.tests.CoreGlueTestUtils;
import org.limewire.friend.api.Friend;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.limegroup.gnutella.LifecycleManager;

public class SharedFileListManagerImplTest extends LimeTestCase {
    
    @Inject private Injector injector;
    @Inject private SharedFileListManagerImpl listManager;

    public SharedFileListManagerImplTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(SharedFileListManagerImplTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        CoreGlueTestUtils.createInjectorAndStart(LimeTestUtils.createModule(this));
    }
    
    @Override
    protected void tearDown() throws Exception {
        injector.getInstance(LifecycleManager.class).shutdown();
    }
    
    public void testContainsDefaultCollection() throws Exception {
        assertEquals(listManager.getModel().toString(), 1, listManager.getModel().size());
        SharedFileList list = listManager.getModel().get(0);
        assertEquals(1, list.getFriendIds().size());
        assertEquals(Friend.P2P_FRIEND_ID, list.getFriendIds().get(0));
        assertEquals(0, list.size());
        assertEquals("Public Shared", list.getCollectionName());
    }
}
