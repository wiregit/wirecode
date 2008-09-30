package org.limewire.ui.swing.sharing;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.painter.MattePainter;
import org.jdesktop.swingx.painter.Painter;
import org.limewire.core.api.library.FileList;
import org.limewire.core.api.library.LibraryListEventType;
import org.limewire.core.api.library.LibraryListListener;
import org.limewire.core.api.library.ShareListManager;
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
    
    @Resource private Color topButtonSelectionGradient;
    @Resource private Color bottomButtonSelectionGradient;
    private final ShareButton gnutellaButton;
    private final ShareButton friendButton;
          
    @Inject
    FilesSharingSummaryPanel(final ShareListManager libraryManager, GnutellaSharePanel gnutellaSharePanel, 
            FriendSharePanel friendSharePanel, Navigator navigator) {
        GuiUtils.assignResources(this);
        
        // TODO: This doesn't get events for adding friend shares.
        // TODO: This is apparently giving events for new managed files,
        //       not shared files.
        libraryManager.addLibraryLisListener(new LibraryListListener() {
            @Override
            public void handleLibraryListEvent(LibraryListEventType type) {
                switch(type) {
                case FILE_ADDED:
                case FILE_REMOVED:
                    SwingUtils.invokeLater(new Runnable() {
                        public void run() {
                            gnutellaButton.setText(GuiUtils.toLocalizedInteger(libraryManager.getGnutellaShareList().size()));
                            int size = 0;
                            // TODO: This is wrong -- it double counts files shared with two friends
                            for(FileList list : libraryManager.getAllFriendShareLists()) {
                                size += list.size();
                            }
                            friendButton.setText(GuiUtils.toLocalizedInteger(size));
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
        gnutellaButton = new ShareButton(NavigatorUtils.getNavAction(gnutellaNav));
        gnutellaButton.setName("FilesSharingSummaryPanel.gnutella");
        gnutellaButton.setText("0");
        gnutellaButton.setGradients(topButtonSelectionGradient, bottomButtonSelectionGradient);
        new ShareDropTarget(gnutellaButton, libraryManager.getGnutellaShareList());
        
        NavItem friendNav = navigator.createNavItem(NavCategory.SHARING, FriendSharePanel.NAME, friendSharePanel);
        friendButton = new ShareButton(NavigatorUtils.getNavAction(friendNav));
        friendButton.setName("FilesSharingSummaryPanel.friends");   
        friendButton.setText("0");
        friendButton.setGradients(topButtonSelectionGradient, bottomButtonSelectionGradient);
		
		setLayout(new MigLayout("insets 0, gap 0", "", ""));

        add(title, "span, wrap");
        add(gnutellaButton, "alignx left");
        add(friendButton, "alignx right");
    }
    
    /**
     * A button that uses a painter to draw the background if
     * its Action.SELECTED_KEY property is true.
     */
    private static class ShareButton extends IconButton {        
        public ShareButton(Action navAction) {
            super(navAction);
        }
        
        public void setGradients(Color topGradient, Color bottomGradient) {
            getAction().addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if(evt.getPropertyName().equals(Action.SELECTED_KEY)) {
                        repaint();
                    }
                }
            });
            
            final Painter<JXButton> oldPainter = getBackgroundPainter();
            setBackgroundPainter(new MattePainter<JXButton>(
                    new GradientPaint(new Point2D.Double(0, 0), topGradient, 
                            new Point2D.Double(0, 1), bottomGradient,
                            false), true) {
                @Override
                public void doPaint(Graphics2D g, JXButton component, int width,
                        int height) {
                    if(Boolean.TRUE.equals(getAction().getValue(Action.SELECTED_KEY))) {
                        super.doPaint(g, component, width, height);
                    } else {
                        oldPainter.paint(g, component, width, height);
                    }
                }
            });
        }
        
    }
}
