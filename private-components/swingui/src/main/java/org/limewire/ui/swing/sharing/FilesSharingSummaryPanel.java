package org.limewire.ui.swing.sharing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.font.TextLayout;
import java.util.Collection;
import java.util.Collections;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.plaf.metal.MetalToggleButtonUI;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.FileList;
import org.limewire.core.api.library.LibraryListEventType;
import org.limewire.core.api.library.LibraryListListener;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.nav.NavigatorUtils;
import org.limewire.ui.swing.sharing.dragdrop.ShareDropTarget;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.SwingUtils;

import com.google.inject.Inject;

public class FilesSharingSummaryPanel extends JPanel {
    
    private final JLabel title = new JLabel();
    private final JToggleButton gnutellaButton;
    private final JToggleButton friendButton;
  
    @Resource private Icon gnutellaIcon;    
    @Resource private Icon friendsIcon;     
    @Resource private Font iconOverlayFont;
    @Resource private Color iconOverlayColor;
    @Resource private Color highLightColor;
        
    @Inject
    FilesSharingSummaryPanel(LibraryManager libraryManager, GnutellaSharePanel gnutellaSharePanel, 
            FriendSharePanel friendSharePanel, Navigator navigator) {
        GuiUtils.assignResources(this);
        
        libraryManager.addLibraryLisListener(new LibraryListListener() {
            @Override
            public void handleLibraryListEvent(LibraryListEventType type) {
                switch(type) {
                case FILE_ADDED:
                case FILE_REMOVED:
                    SwingUtils.invokeLater(new Runnable() {
                        public void run() {
                            gnutellaButton.repaint();
                            friendButton.repaint();
                        }
                    });
                    break;
                }
            }
        });
        
        setOpaque(false);
        title.setName("FilesSharingSummaryPanel.title");
        title.setText("Files I'm Sharing");
        
        NavItem gnutellaNav = navigator.createNavItem(NavCategory.SHARING, GnutellaSharePanel.NAME, gnutellaSharePanel);
        gnutellaButton = new JToggleButton(NavigatorUtils.getNavAction(gnutellaNav));
        gnutellaButton.setHideActionText(true);
        gnutellaButton.setName("FilesSharingSummaryPanel.all");
        gnutellaButton.setIcon(new NumberIcon(Collections.singleton(libraryManager.getGnutellaShareList()), gnutellaIcon));
        gnutellaButton.setUI(new HighlightToggleButtonUI(highLightColor));
        new ShareDropTarget(gnutellaButton, libraryManager.getGnutellaShareList());
        
        NavItem friendNav = navigator.createNavItem(NavCategory.SHARING, FriendSharePanel.NAME, friendSharePanel);
        friendButton = new JToggleButton(NavigatorUtils.getNavAction(friendNav));
        friendButton.setHideActionText(true);
        friendButton.setName("FilesSharingSummaryPanel.friends");
		friendButton.setIcon(new NumberIcon(libraryManager.getAllFriendShareLists(), friendsIcon));
		friendButton.setUI(new HighlightToggleButtonUI(highLightColor));   
		
		setLayout(new MigLayout("insets 0 0 0 0", "", ""));

        add(title, "span, gapbottom 10, wrap");
        add(gnutellaButton);
        add(friendButton);
    }
    
    private class NumberIcon implements Icon {
        private final Collection<? extends FileList> fileLists;
        private final Icon delegateIcon;
        
        public NumberIcon(Collection<? extends FileList> fileLists,  Icon icon) {
            this.delegateIcon = icon;
            this.fileLists = fileLists;
        }
        
        @Override
        public int getIconHeight() {
            return delegateIcon.getIconHeight();
        }
        @Override
        public int getIconWidth() {
            return delegateIcon.getIconWidth();
        }
        
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D)g;
            g2.setFont(iconOverlayFont);
            TextLayout layout = new TextLayout(String.valueOf(getNumber()), g2.getFont(), g2.getFontRenderContext());
            delegateIcon.paintIcon(c, g, x, y);
            Rectangle bounds = layout.getPixelBounds(null, 0, 0);
            g2.setPaint(iconOverlayColor);
            layout.draw(g2, x + getIconWidth() - bounds.width, y + bounds.height);
        }
        
        private long getNumber() {
            long x = 0;
            for(FileList list : fileLists) {
                x += list.size();
            }
            return x;
        }
    }
    
    private class HighlightToggleButtonUI extends MetalToggleButtonUI {
        Color color;
        public HighlightToggleButtonUI(Color color) {
            this.color = color;
        }
        @Override
        public Color getSelectColor() {
            return color;
        }
    }
}
