package org.limewire.ui.swing.library;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.friend.Friend;
import org.limewire.ui.swing.util.I18n;

public class FriendEmptyLibrary extends JPanel implements Disposable {
    
    public FriendEmptyLibrary(Friend friend) {
        setLayout(new MigLayout("fill"));
        if(friend.isAnonymous()) {
            add(new JLabel(I18n.tr("Cannot browse {0}", friend.getRenderName())), "alignx 50%");
        } else 
            add(new JLabel(I18n.tr("{0} is not logged on through LimeWire.", friend.getRenderName())), "alignx 50%");
    }

    @Override
    public void dispose() {
    }
}
