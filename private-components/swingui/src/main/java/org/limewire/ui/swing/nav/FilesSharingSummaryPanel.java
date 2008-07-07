package org.limewire.ui.swing.nav;

import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class FilesSharingSummaryPanel extends JPanel {
    
    private final JLabel title = new JLabel();
    private final JLabel all = new JLabel();
    private final JLabel buddies = new JLabel();
    private final JLabel some = new JLabel();
    
    public FilesSharingSummaryPanel() {
        setOpaque(false);
        title.setText("Files I'm Sharing");
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        
        all.setVerticalTextPosition(JLabel.BOTTOM);
        all.setIcon(new NumberIcon(1));
        all.setFont(all.getFont().deriveFont(all.getFont().getSize() - 5f));
        all.setText("with everyone");
        buddies.setVerticalTextPosition(JLabel.BOTTOM);
        buddies.setIcon(new NumberIcon(1));
        buddies.setText("with all friends");
        buddies.setFont(all.getFont());
        some.setIcon(new NumberIcon(1));
        some.setVerticalTextPosition(JLabel.BOTTOM);
        some.setText("with some friends");
        some.setFont(all.getFont());
                
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
    
    private static class NumberIcon implements Icon {
        private final int number;
        public NumberIcon(int number) {
            this.number = number;
        }
        
        @Override
        public int getIconHeight() {
            return 16;
        }
        @Override
        public int getIconWidth() {
            return 16;
        }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
//            Graphics2D g2 = (Graphics2D)g;
//            g2.drawString(number + "", x, y);
        }
    }

}
