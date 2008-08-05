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
import java.util.HashMap;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LibraryListEventType;
import org.limewire.core.api.library.LibraryListListener;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.ui.swing.nav.Navigator.NavCategory;
import org.limewire.ui.swing.sharing.BuddySharePanel;
import org.limewire.ui.swing.sharing.GnutellaSharePanel;
import org.limewire.ui.swing.sharing.IndividualSharePanel;
import org.limewire.ui.swing.sharing.ShareDropTarget;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.SwingUtils;

import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class FilesSharingSummaryPanel extends JPanel {
    
    private final JLabel title = new JLabel();
    private final JToggleButton all = new JToggleButton();
    private final JToggleButton buddies = new JToggleButton();
    private final JToggleButton some = new JToggleButton();
    
    ButtonGroup buttonGroup = new ButtonGroup();
    
    private Map<String, NavItem> navItems = new HashMap<String, NavItem>();
    
    @Resource private Icon allIcon;    
    @Resource private Icon buddiesIcon;    
    @Resource private Icon someBuddiesIcon;    
    @Resource private Font iconOverlayFont;
    @Resource private Color iconOverlayColor;
    
    private final NavigableTarget navTarget;
        
    @Inject
    FilesSharingSummaryPanel(@Named("MainTarget") NavigableTarget navTarget, LibraryManager libraryManager) {
        GuiUtils.assignResources(this);
        
        this.navTarget = navTarget;
        
        libraryManager.addLibraryLisListener(new LibraryListListener() {
            @Override
            public void handleLibraryListEvent(LibraryListEventType type) {
                switch(type) {
                case FILE_ADDED:
                case FILE_REMOVED:
                    SwingUtils.invokeLater(new Runnable() {
                        public void run() {
                            all.repaint();
                            some.repaint();
                            buddies.repaint();
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
        all.setName("FilesSharingSummaryPanel.all");
        all.setIcon(new NumberIcon(libraryManager.getGnutellaList(), allIcon));
        all.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                showGnutella();
            }
        });
        new ShareDropTarget(all, libraryManager);
        
		buddies.setName("FilesSharingSummaryPanel.buddies");
		buddies.setIcon(new NumberIcon(libraryManager.getAllBuddyList(), buddiesIcon));
		buddies.addActionListener(new ActionListener(){
		    public void actionPerformed(ActionEvent event) {
		        showBuddy();
		    }
		});
		some.setName("FilesSharingSummaryPanel.some");
		some.setIcon(new NumberIcon(libraryManager.getUniqueLists(), someBuddiesIcon));
		some.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                showIndividualBuddies();
            }
		});
		
		buttonGroup.add(all);
		buttonGroup.add(buddies);
		buttonGroup.add(some);
                
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
        add(all, gbc);
        
        gbc.gridheight = GridBagConstraints.RELATIVE;
        add(buddies, gbc);
        
        gbc.gridheight = GridBagConstraints.REMAINDER;
        add(some, gbc);
     
    }
    
    private void showGnutella() {
        navTarget.showNavigablePanel(navItems.get(GnutellaSharePanel.NAME));
    }
    
    private void showBuddy() {
        navTarget.showNavigablePanel(navItems.get(BuddySharePanel.NAME));
    }
    
    private void showIndividualBuddies() {
        navTarget.showNavigablePanel(navItems.get(IndividualSharePanel.NAME));
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
        
    @Inject
    public void addDefaultNavigableItems(GnutellaSharePanel gnutellaShare, BuddySharePanel buddyShare, 
            IndividualSharePanel individualShare) {
        navItems.put(GnutellaSharePanel.NAME, addNavigablePanel(NavCategory.SHARING, GnutellaSharePanel.NAME, gnutellaShare, false));
        navItems.put(BuddySharePanel.NAME, addNavigablePanel(NavCategory.SHARING, BuddySharePanel.NAME, buddyShare, false));
        navItems.put(IndividualSharePanel.NAME, addNavigablePanel(NavCategory.SHARING, IndividualSharePanel.NAME, individualShare, false));
    }

    public NavItem addNavigablePanel(final NavCategory category, final String name, final JComponent panel,
            boolean userRemovable) {
        NavItem item = new NavItem() {
            @Override
            public String getName() {
                return name;
            }
            
            @Override
            public void remove() {
                navTarget.removeNavigablePanel(name);
//                navigator.removeNavigablePanel(category, this);
            }
            
            @Override
            public void select() {
//                selectNavigablePanel(category, this);
            }
        };
        navTarget.addNavigablePanel(item, panel);
        return item;
    }
    
    
}
