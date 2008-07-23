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
import java.awt.font.TextLayout;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.FileList;
import org.limewire.core.api.library.LibraryListEventType;
import org.limewire.core.api.library.LibraryListListener;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.SwingUtils;

import com.google.inject.Inject;

public class FilesSharingSummaryPanel extends JPanel {
    
    private final JLabel title = new JLabel();
    private final JLabel all = new JLabel();
    private final JLabel buddies = new JLabel();
    private final JLabel some = new JLabel();
    
    @Resource private Icon allIcon;    
    @Resource private Icon buddiesIcon;    
    @Resource private Icon someBuddiesIcon;    
    @Resource private Font iconOverlayFont;
    @Resource private Color iconOverlayColor;
        
    @Inject
    FilesSharingSummaryPanel(LibraryManager libraryManager) {
        GuiUtils.assignResources(this);
        
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
        all.setIcon(new NumberIcon(libraryManager.getGnutellaFileList(), allIcon));
		buddies.setName("FilesSharingSummaryPanel.buddies");
		buddies.setIcon(new NumberIcon(libraryManager.getBuddiesFileList(), buddiesIcon));
		some.setName("FilesSharingSummaryPanel.some");
		some.setIcon(new NumberIcon(libraryManager.getUniqueLists(), someBuddiesIcon));
                
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

}
