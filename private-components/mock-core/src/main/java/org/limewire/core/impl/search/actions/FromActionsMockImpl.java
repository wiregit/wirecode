package org.limewire.core.impl.search.actions;

import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.actions.FromActions;

/**
 * This class provides a mock implementation of the FromActions interface.
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class FromActionsMockImpl implements FromActions {
    @Override
    public void chatWith(RemoteHost person) {
        System.out.println("starting chat with " + person);
    }

    @Override
    public void showFilesSharedBy(RemoteHost person) {
        System.out.println("showing files shared by " + person);
    }

    @Override
    public void viewLibraryOf(RemoteHost person) {
        System.out.println("viewing library of " + person);
    }
}