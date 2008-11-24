package org.limewire.ui.swing.library;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LibraryState;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class FriendLibraryMediator extends BaseLibraryMediator {

    private final FriendLibraryFactory factory;
    private final SharingLibraryFactory sharingFactory;
    private final LibraryManager libraryManager;
    private final ShareListManager shareListManager;
    private final Friend friend;
    private boolean setLibraryPanel;
    
    @AssistedInject
    public FriendLibraryMediator(@Assisted Friend friend, FriendLibraryFactory factory,  
            SharingLibraryFactory sharingFactory, LibraryManager libraryManager, ShareListManager shareListManager) {
        this.factory = factory;
        this.friend = friend;        
        this.sharingFactory = sharingFactory;
        this.libraryManager = libraryManager;
        this.shareListManager = shareListManager;
        setLibraryCard(new EmptyPanel(false));
    }
    
    public void createLibraryPanel(EventList<RemoteFileItem> eventList, LibraryState libraryState) {
        switch(libraryState) {
        case FAILED_TO_LOAD:
            setLibraryPanel = false;
            setLibraryCard(new EmptyPanel(true));
            showLibraryCard();
            break;
        case LOADED:
        case LOADING:
            if(!setLibraryPanel) {
                setLibraryPanel = true;
                JComponent component = factory.createFriendLibrary(friend, eventList, this);
                setLibraryCard(component);
                showLibraryCard();
            }
            break;
        }
    }        
    
    private class EmptyPanel extends JPanel implements Disposable {        
        public EmptyPanel(boolean failed) {
            setLayout(new MigLayout("fill, wrap, gap 0"));
            if(!friend.isAnonymous() && !failed) {
                add(new JLabel(I18n.tr("{0} is not logged on through LimeWire.", friend.getRenderName())), "alignx 50%, aligny bottom");
            } else {
                if(failed) {                    
                    add(new JLabel(I18n.tr("Cannot browse {0}.", friend.getRenderName())), "alignx 50%, aligny bottom");
                } else {
                    add(new JLabel(I18n.tr("Attempting to browse {0}.", friend.getRenderName())), "alignx 50%, aligny bottom");
                }
            }
            
   	        if (!friend.isAnonymous()) {
                JButton button = new JButton(I18n.tr("View Files I'm Sharing with {0}", friend.getRenderName()));
                button.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        FriendLibraryMediator.this.showSharingCard();
                    }
                });

                add(button, "alignx 50%, gaptop 15, aligny top");
            }
        }

        @Override
        public void dispose() {
        }
    }

    @Override
    public void showSharingCard() {
        if(sharingComponent == null) {
            setSharingCard(sharingFactory.createSharingLibrary(this, friend, 
                    libraryManager.getLibraryManagedList().getSwingModel(),
                    shareListManager.getOrCreateFriendShareList(friend)));
        }
        super.showSharingCard();
    }
}
