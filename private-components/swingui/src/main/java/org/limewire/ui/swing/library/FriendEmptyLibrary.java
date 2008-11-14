package org.limewire.ui.swing.library;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.friend.Friend;
import org.limewire.ui.swing.util.I18n;

public class FriendEmptyLibrary extends JPanel implements Disposable {
    
    private JButton sharingViewButton;
    
    public FriendEmptyLibrary(final Friend friend, final LibraryNavigator navigator) {
        setLayout(new MigLayout("fill, wrap, gap 0"));
        if(friend.isAnonymous()) {
            add(new JLabel(I18n.tr("Cannot browse {0}", friend.getRenderName())), "alignx 50%, aligny bottom");
        } else 
            add(new JLabel(I18n.tr("{0} is not logged on through LimeWire.", friend.getRenderName())), "alignx 50%, aligny bottom");
        
        if(!friend.isAnonymous()) {
            sharingViewButton = new JButton(I18n.tr("View Files I'm Sharing with {0}", friend.getRenderName()));
            sharingViewButton.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent e) {
                    navigator.selectFriendShareList(friend);
                }
             });
            
            add(sharingViewButton, "alignx 50%, gaptop 15, aligny top");
        }
    }

    @Override
    public void dispose() {
    }
}
