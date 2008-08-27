package org.limewire.ui.swing.nav;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.TextLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JComponent;
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
import org.limewire.ui.swing.nav.Navigator.NavCategory;
import org.limewire.ui.swing.sharing.BuddySharePanel;
import org.limewire.ui.swing.sharing.GenericSharingPanel;
import org.limewire.ui.swing.sharing.GnutellaSharePanel;
import org.limewire.ui.swing.sharing.SharingNavigator;
import org.limewire.ui.swing.sharing.dragdrop.ShareDropTarget;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.SwingUtils;

import com.google.inject.Inject;

public class FilesSharingSummaryPanel extends JPanel implements SharingNavigator {
    
    private final JLabel title = new JLabel();
    private final JToggleButton gnutellaButton = new JToggleButton();
    private final JToggleButton buddyButton = new JToggleButton();
  
    @Resource private Icon gnutellaIcon;    
    @Resource private Icon buddiesIcon;     
    @Resource private Font iconOverlayFont;
    @Resource private Color iconOverlayColor;
    @Resource private Color highLightColor;
    
    private Navigator navigator;
        
    @Inject
    FilesSharingSummaryPanel(LibraryManager libraryManager, GnutellaSharePanel gnutellaSharePanel, 
            BuddySharePanel buddySharePanel) {
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
        
        //TODO: NumberIcons
        gnutellaButton.setName("FilesSharingSummaryPanel.all");
        gnutellaButton.setIcon(new NumberIcon(libraryManager.getGnutellaList(), gnutellaIcon));
        gnutellaButton.setUI(new HighlightToggleButtonUI(highLightColor));
        new ShareDropTarget(gnutellaButton, libraryManager.getGnutellaList());
        
		buddyButton.setName("FilesSharingSummaryPanel.buddies");
		buddyButton.setIcon(new NumberIcon(libraryManager.getUniqueLists(), buddiesIcon));
		buddyButton.setUI(new HighlightToggleButtonUI(highLightColor));   
		
		setLayout(new MigLayout("insets 0 0 0 0", "", ""));

        add(title, "span, gapbottom 10, wrap");
        add(gnutellaButton);
        add(buddyButton);
     
        addPropertyChangeListener(new StartupListener(this, libraryManager, gnutellaSharePanel, buddySharePanel));
    }
    
    @Inject void setNavigator(Navigator navigator) {
        this.navigator = navigator;
    }
    
    private class NumberIcon implements Icon {
        private final FileList fileList;
        private final Map<String, FileList> fileLists;
        private final Icon delegateIcon;
        
        public NumberIcon(FileList fileList, Icon icon) {
            this.fileList = fileList;
            this.delegateIcon = icon;
            this.fileLists = null;
        }
        
        public NumberIcon(Map<String, FileList> fileLists, Icon icon) {
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
        
    //TODO: call this from guice?
    public void addDefaultNavigableItems(GenericSharingPanel gnutellaShare, BuddySharePanel buddyShare) {
        NavItem item = addSharingItem(GnutellaSharePanel.NAME, gnutellaShare);
        gnutellaButton.addActionListener(new SharingButtonAction(item, gnutellaButton));
        
        item = addSharingItem(BuddySharePanel.NAME, buddyShare);
        buddyButton.addActionListener(new SharingButtonAction(item, buddyButton));
    }

    @Override
    public NavItem addSharingItem(final String title, JComponent sharingPanel) {
        final NavItem item = navigator.addNavigablePanel(NavCategory.SHARING, title, sharingPanel, false);
        return item;
    }
    
    private class SharingButtonAction implements ActionListener {
        private final NavItem item;
        private final JToggleButton button;
        
        public SharingButtonAction(final NavItem item, JToggleButton button) {
            this.item = item;
            this.button = button;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            item.select();
            navigator.addNavListener(new NavSelectionListener(){
                @Override
                public void navItemSelected(NavCategory category, NavItem navItem) {
                    if(navItem != item) {
                        navigator.removeNavListener(this);
                        SwingUtils.invokeLater(new Runnable(){
                            public void run() {
                                //TODO: this is too slow
                                button.setSelected(false);
                            }
                        });
                    }
                }
            });
        }      
    }

    private class StartupListener implements PropertyChangeListener {
        JComponent owner;
        LibraryManager libraryManager;
        GnutellaSharePanel gnutellaSharePanel;
        BuddySharePanel buddySharePanel;
            
        public StartupListener(JComponent owner, LibraryManager libraryManager, GnutellaSharePanel gnutellaSharePanel, 
                BuddySharePanel buddySharePanel) {
            this.owner = owner;
            this.libraryManager = libraryManager;
            this.gnutellaSharePanel = gnutellaSharePanel;
            this.buddySharePanel = buddySharePanel;
        }
        
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            owner.removePropertyChangeListener(this);
            addDefaultNavigableItems(gnutellaSharePanel, 
                            buddySharePanel);
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
