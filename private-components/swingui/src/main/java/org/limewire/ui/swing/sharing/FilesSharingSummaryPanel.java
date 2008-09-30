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
import org.limewire.core.api.library.FriendShareListEvent;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.mainframe.SectionHeading;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.nav.NavigatorUtils;
import org.limewire.ui.swing.sharing.dragdrop.ShareDropTarget;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.Inject;

public class FilesSharingSummaryPanel extends JPanel {
    
    private final SectionHeading title;
    
    @Resource private Color topButtonSelectionGradient;
    @Resource private Color bottomButtonSelectionGradient;
    private final ShareButton gnutellaButton;
    private final ShareButton friendButton;
          
    @Inject
    FilesSharingSummaryPanel(final ShareListManager shareListManager, GnutellaSharePanel gnutellaSharePanel, 
            FriendSharePanel friendSharePanel, Navigator navigator, ListenerSupport<FriendShareListEvent> shareSupport) {
        GuiUtils.assignResources(this);
        
        setOpaque(false);
        title = new SectionHeading(I18n.tr("Files I'm Sharing"));
        title.setName("FilesSharingSummaryPanel.title");
        
        NavItem gnutellaNav = navigator.createNavItem(NavCategory.SHARING, GnutellaSharePanel.NAME, gnutellaSharePanel);
        gnutellaButton = new ShareButton(NavigatorUtils.getNavAction(gnutellaNav));
        gnutellaButton.setName("FilesSharingSummaryPanel.gnutella");
        gnutellaButton.setText("0");
        gnutellaButton.setGradients(topButtonSelectionGradient, bottomButtonSelectionGradient);
        new ShareDropTarget(gnutellaButton, shareListManager.getGnutellaShareList());
        
        NavItem friendNav = navigator.createNavItem(NavCategory.SHARING, FriendSharePanel.NAME, friendSharePanel);
        friendButton = new ShareButton(NavigatorUtils.getNavAction(friendNav));
        friendButton.setName("FilesSharingSummaryPanel.friends");   
        friendButton.setText("0");
        friendButton.setGradients(topButtonSelectionGradient, bottomButtonSelectionGradient);
		
		setLayout(new MigLayout("insets 0, gap 0", "", ""));

        add(title, "span, wrap");
        add(gnutellaButton, "alignx left");
        add(friendButton, "alignx right");
        
        shareListManager.getGnutellaShareList().getSwingModel().addListEventListener(new ListEventListener<LocalFileItem>() {
            @Override
            public void listChanged(ListEvent<LocalFileItem> listChanges) {
                gnutellaButton.setText(GuiUtils.toLocalizedInteger(shareListManager.getGnutellaShareList().size()));                
            }
        });
        shareSupport.addListener(new ShareListListener());
    }
    
    // TODO: This is wrong -- it double counts files that are shared with two people.
    private class ShareListListener implements EventListener<FriendShareListEvent>, ListEventListener<LocalFileItem> {
        private int count = 0;
        
        @Override
        @SwingEDTEvent
        public void handleEvent(FriendShareListEvent event) {
            LocalFileList list = event.getFileList();
            EventList<LocalFileItem> model = list.getSwingModel();
            switch(event.getType()) {
            case FRIEND_SHARE_LIST_ADDED:
                updateCount(model.size());
                model.addListEventListener(this);
                break;
            case FRIEND_SHARE_LIST_REMOVED:
                updateCount(-model.size());
                model.removeListEventListener(this);
                break;
            }
        }
        
        @Override
        public void listChanged(ListEvent<LocalFileItem> listChanges) {
            int count = 0;
            if(!listChanges.isReordering()) {
                while(listChanges.next()) {
                    switch(listChanges.getType()) {
                    case ListEvent.INSERT: count++; break;
                    case ListEvent.DELETE: count--; break;
                    }
                }
            }
            updateCount(count);
        }
        
        private void updateCount(int incremenet) {
            count += incremenet;
            friendButton.setText(GuiUtils.toLocalizedInteger(count));
        }
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
