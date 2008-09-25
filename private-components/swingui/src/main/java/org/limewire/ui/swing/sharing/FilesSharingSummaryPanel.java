package org.limewire.ui.swing.sharing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.font.TextLayout;
import java.util.Map;

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
import org.limewire.core.api.library.LocalFileList;
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
    private final JToggleButton buddyButton;
  
    @Resource private Icon gnutellaIcon;    
    @Resource private Icon buddiesIcon;     
    @Resource private Font iconOverlayFont;
    @Resource private Color iconOverlayColor;
    @Resource private Color highLightColor;
        
    @Inject
    FilesSharingSummaryPanel(LibraryManager libraryManager, GnutellaSharePanel gnutellaSharePanel, 
            BuddySharePanel buddySharePanel, Navigator navigator) {
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
                            buddyButton.repaint();
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
        gnutellaButton.setIcon(new NumberIcon(libraryManager.getGnutellaList(), gnutellaIcon));
        gnutellaButton.setUI(new HighlightToggleButtonUI(highLightColor));
        new ShareDropTarget(gnutellaButton, libraryManager.getGnutellaList());
        
        NavItem buddyNav = navigator.createNavItem(NavCategory.SHARING, BuddySharePanel.NAME, buddySharePanel);
        buddyButton = new JToggleButton(NavigatorUtils.getNavAction(buddyNav));
        buddyButton.setHideActionText(true);
        buddyButton.setName("FilesSharingSummaryPanel.buddies");
		buddyButton.setIcon(new NumberIcon(libraryManager.getAllBuddyLists(), buddiesIcon));
		buddyButton.setUI(new HighlightToggleButtonUI(highLightColor));   
		
		setLayout(new MigLayout("insets 0 0 0 0", "", ""));

        add(title, "span, gapbottom 10, wrap");
        add(gnutellaButton);
        add(buddyButton);
    }
    
    private class NumberIcon implements Icon {
        private final FileList fileList;
        private final Map<String, LocalFileList> fileLists;
        private final Icon delegateIcon;
        
        public NumberIcon(FileList fileList, Icon icon) {
            this.fileList = fileList;
            this.delegateIcon = icon;
            this.fileLists = null;
        }
        
        public NumberIcon(Map<String, LocalFileList> fileLists, Icon icon) {
            this.fileList = null;
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
            if(fileList != null) {
                return fileList.size();
            } else {
                long x = 0;
                for(FileList list : fileLists.values()) {
                    x += list.size();
                }
                return x;
            }
            
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
