package org.limewire.ui.swing.nav;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;

public class FilesSharingSummaryPanel extends JPanel {
    
    private final JLabel title = new JLabel();
    private final JLabel all = new JLabel();
    private final JLabel buddies = new JLabel();
    private final JLabel some = new JLabel();
    
    public FilesSharingSummaryPanel() {
        setOpaque(false);
        title.setName("FilesSharingSummaryPanel.title");
        title.setText("Files I'm Sharing");
        
        //TODO: NumberIcons
        all.setName("FilesSharingSummaryPanel.all");
		//all.setIcon(new NumberIcon(1));
		buddies.setName("FilesSharingSummaryPanel.buddies");
		//buddies.setIcon(new NumberIcon(1));
		some.setName("FilesSharingSummaryPanel.some");
		//some.setIcon(new NumberIcon(1));
                
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
    
//    private static class NumberIcon implements Icon {
//        private final int number;
//        public NumberIcon(int number) {
//            this.number = number;
//        }
//        
//        @Override
//        public int getIconHeight() {
//            return 16;
//        }
//        @Override
//        public int getIconWidth() {
//            return 16;
//        }
//        @Override
//        public void paintIcon(Component c, Graphics g, int x, int y) {
////            Graphics2D g2 = (Graphics2D)g;
////            g2.drawString(number + "", x, y);
//        }
//    }

}
