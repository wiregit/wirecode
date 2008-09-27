package org.limewire.ui.swing.sharing;

import javax.swing.JButton;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.library.FileList;
import org.limewire.core.api.library.LibraryListEventType;
import org.limewire.core.api.library.LibraryListListener;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.mainframe.SectionHeading;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.nav.NavigatorUtils;
import org.limewire.ui.swing.sharing.dragdrop.ShareDropTarget;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SwingUtils;

import com.google.inject.Inject;

public class FilesSharingSummaryPanel extends JPanel {
    
    private final SectionHeading title;
    private final JButton gnutellaButton;
    private final JButton friendButton;
          
    @Inject
    FilesSharingSummaryPanel(final LibraryManager libraryManager, GnutellaSharePanel gnutellaSharePanel, 
            FriendSharePanel friendSharePanel, Navigator navigator) {
        GuiUtils.assignResources(this);
        
        // TODO: This doesn't get events for adding friend shares.
        libraryManager.addLibraryLisListener(new LibraryListListener() {
            @Override
            public void handleLibraryListEvent(LibraryListEventType type) {
                switch(type) {
                case FILE_ADDED:
                case FILE_REMOVED:
                    SwingUtils.invokeLater(new Runnable() {
                        public void run() {
                            gnutellaButton.setText(String.valueOf(libraryManager.getGnutellaShareList().size()));
                            int size = 0;
                            // TODO: This is wrong -- it double counts files shared with two friends
                            for(FileList list : libraryManager.getAllFriendShareLists()) {
                                size += list.size();
                            }
                            friendButton.setText(String.valueOf(size));
                        }
                    });
                    break;
                }
            }
        });
        
        setOpaque(false);
        title = new SectionHeading(I18n.tr("Files I'm Sharing"));
        title.setName("FilesSharingSummaryPanel.title");
        
        NavItem gnutellaNav = navigator.createNavItem(NavCategory.SHARING, GnutellaSharePanel.NAME, gnutellaSharePanel);
        gnutellaButton = new IconButton(NavigatorUtils.getNavAction(gnutellaNav));
        gnutellaButton.setHideActionText(true);
        gnutellaButton.setName("FilesSharingSummaryPanel.gnutella");
        new ShareDropTarget(gnutellaButton, libraryManager.getGnutellaShareList());
        
        NavItem friendNav = navigator.createNavItem(NavCategory.SHARING, FriendSharePanel.NAME, friendSharePanel);
        friendButton = new IconButton(NavigatorUtils.getNavAction(friendNav));
        friendButton.setHideActionText(true);
        friendButton.setName("FilesSharingSummaryPanel.friends");   
		
		setLayout(new MigLayout("insets 0 0 0 0", "[grow]", ""));

        add(title, "span, wrap");
        add(gnutellaButton, "alignx center, aligny center");
        add(friendButton, "alignx center, aligny center");
    }
}
