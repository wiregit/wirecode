package org.limewire.ui.swing.menu;

import javax.swing.JMenu;

import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.ui.swing.library.nav.FriendSelectEvent;
import org.limewire.ui.swing.menu.actions.ChatAction;
import org.limewire.ui.swing.menu.actions.SignInOutAction;
import org.limewire.ui.swing.menu.actions.SwitchUserAction;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class FriendMenu extends JMenu {
    @Inject
    public FriendMenu(SwitchUserAction switchUserAction, SignInOutAction signInOutAction, ChatAction chatAction, @Named("friendSelection") ListenerSupport<FriendSelectEvent> friendSelectListenerSupport) {
        super(I18n.tr("Friend"));
        friendSelectListenerSupport.addListener(new EventListener<FriendSelectEvent>() {
           @Override
            public void handleEvent(FriendSelectEvent event) {
               
               System.out.println("Friend Selected: " + event.getSelectedFriend());
            } 
        });
        
        add(chatAction);
        addSeparator();
        add(switchUserAction);
        add(signInOutAction);
    }
}