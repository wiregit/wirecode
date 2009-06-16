package org.limewire.ui.swing.friends;

import org.limewire.inject.LazySingleton;
import org.limewire.ui.swing.nav.NavMediator;

import com.google.inject.Inject;
import com.google.inject.Provider;

@LazySingleton
public class AllFriendsMediator  implements NavMediator<AllFriendsPanel> {
    
    public static final String NAME = "AllFriendsPanel";
    
    private final Provider<AllFriendsPanel> allFriendsPanel;
    private AllFriendsPanel allFriends;
    
    @Inject
    public AllFriendsMediator(Provider<AllFriendsPanel> allFriendsPanel) {
        this.allFriendsPanel = allFriendsPanel;
    }
        
    @Override
    public AllFriendsPanel getComponent() {
        if(allFriends == null)
            allFriends = allFriendsPanel.get();
        return allFriends;
    }
}
