package org.limewire.ui.swing.nav;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LibraryListEventType;
import org.limewire.core.api.library.LibraryListListener;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.ui.swing.nav.Navigator.NavCategory;
import org.limewire.ui.swing.sharing.BuddySharePanel;
import org.limewire.ui.swing.sharing.GenericSharingPanel;
import org.limewire.ui.swing.sharing.GnutellaSharePanel;
import org.limewire.ui.swing.sharing.IndividualSharePanel;
import org.limewire.ui.swing.sharing.ShareDropTarget;
import org.limewire.ui.swing.sharing.SharingNavigator;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.SwingUtils;

import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;

public class FilesSharingSummaryPanel extends JPanel implements SharingNavigator {
    
    private final JLabel title = new JLabel();
    private final JToggleButton gnutellaButton = new JToggleButton();
    private final JToggleButton buddyButton = new JToggleButton();
    private final JToggleButton individualButton = new JToggleButton();
    
    @Resource private Icon gnutellaIcon;    
    @Resource private Icon buddiesIcon;    
    @Resource private Icon individualBuddiesIcon;    
    @Resource private Font iconOverlayFont;
    @Resource private Color iconOverlayColor;
    @Resource private Color highLightColor;
    
    private Navigator navigator;
        
    @Inject
    FilesSharingSummaryPanel(LibraryManager libraryManager, GnutellaSharePanel gnutellaSharePanel, 
            BuddySharePanel buddySharePanel, IndividualSharePanel individualSharePanel) {
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
                            individualButton.repaint();
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
        gnutellaButton.setIcon(new NumberIcon(libraryManager.getGnutellaList().getModel(), gnutellaIcon));
        gnutellaButton.setUI(new HighlightToggleButtonUI(highLightColor));
        new ShareDropTarget(gnutellaButton, libraryManager.getGnutellaList());
        
		buddyButton.setName("FilesSharingSummaryPanel.buddies");
		buddyButton.setIcon(new NumberIcon(libraryManager.getAllBuddyList().getModel(), buddiesIcon));
		buddyButton.setUI(new HighlightToggleButtonUI(highLightColor));

		individualButton.setName("FilesSharingSummaryPanel.some");
		individualButton.setIcon(new NumberIcon(libraryManager.getUniqueLists(), individualBuddiesIcon));
        individualButton.setUI(new HighlightToggleButtonUI(highLightColor));        
		
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 5, 0);
        add(title, gbc);
        

        gbc.gridheight = GridBagConstraints.REMAINDER;
        gbc.gridwidth = 1;
        add(gnutellaButton, gbc);
        
        gbc.gridheight = GridBagConstraints.RELATIVE;
        add(buddyButton, gbc);
        
        gbc.gridheight = GridBagConstraints.REMAINDER;
        add(individualButton, gbc);
     
        addPropertyChangeListener(new StartupListener(this, libraryManager, gnutellaSharePanel, buddySharePanel, individualSharePanel));
    }
    
    @Inject void setNavigator(Navigator navigator) {
        this.navigator = navigator;
    }
    
    private class NumberIcon implements Icon {
        private final EventList<FileItem> fileList;
        private final Map<String, EventList<FileItem>> fileLists;
        private final Icon delegateIcon;
        
        public NumberIcon(EventList<FileItem> fileList, Icon icon) {
            this.fileList = fileList;
            this.delegateIcon = icon;
            this.fileLists = null;
        }
        
        public NumberIcon(Map<String, EventList<FileItem>> fileLists, Icon icon) {
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
                for(EventList<FileItem> list : fileLists.values()) {
                    x += list.size();
                }
                return x;
            }
            
        }
    }
        
    //TODO: call this from guice?
    public void addDefaultNavigableItems(GenericSharingPanel gnutellaShare, BuddySharePanel buddyShare, 
            IndividualSharePanel individualShare) {
        NavItem item = addSharingItem(GnutellaSharePanel.NAME, gnutellaShare);
        gnutellaButton.addActionListener(new SharingButtonAction(item, gnutellaButton));
        
        item = addSharingItem(BuddySharePanel.NAME, buddyShare);
        buddyButton.addActionListener(new SharingButtonAction(item, buddyButton));
        
        item = addSharingItem(IndividualSharePanel.NAME, individualShare);
        individualButton.addActionListener(new SharingButtonAction(item, individualButton));
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
        IndividualSharePanel individualSharePanel;
            
        public StartupListener(JComponent owner, LibraryManager libraryManager, GnutellaSharePanel gnutellaSharePanel, 
                BuddySharePanel buddySharePanel, IndividualSharePanel individualSharePanel) {
            this.owner = owner;
            this.libraryManager = libraryManager;
            this.gnutellaSharePanel = gnutellaSharePanel;
            this.buddySharePanel = buddySharePanel;
            this.individualSharePanel = individualSharePanel;
        }
        
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            owner.removePropertyChangeListener(this);
            addDefaultNavigableItems(gnutellaSharePanel, 
                            buddySharePanel, 
                            individualSharePanel);
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
