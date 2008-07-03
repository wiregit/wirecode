package org.limewire.ui.swing.nav;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;

public class DownloadSummaryPanel extends JPanel {
    
    private final JLabel title = new JLabel();
    private final TitleAndPercentage one = new TitleAndPercentage();
    private final TitleAndPercentage two = new TitleAndPercentage();
    private final TitleAndPercentage three = new TitleAndPercentage();
    
    public DownloadSummaryPanel() {
        setOpaque(false);
        title.setText("Downloads (5)");
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        
        one.update("Monkey On Skateboard", 62);
        two.update("The Juliens at the Knitting Factory", 27);
        three.update("Album - Colorado Trip", 25);
        
        setLayout(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 5, 0);        
        add(title, gbc);
        
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(0, 5, 0, 0);
        add(one, gbc);
        add(two, gbc);
        add(three, gbc);
    }
    
    private static class TitleAndPercentage extends JPanel {
        private JLabel title = new JLabel();
        private JLabel percentage = new JLabel();
        
        public TitleAndPercentage() {
            setOpaque(false);
            setLayout(new GridBagLayout());
            title.setFont(title.getFont().deriveFont(title.getFont().getSize() - 1f));
            percentage.setFont(title.getFont());
            
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridwidth = GridBagConstraints.RELATIVE;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.weightx = 1;
            gbc.weighty = 1;
            gbc.insets = new Insets(0, 0, 0, 5);
            add(title, gbc);
            
            gbc.weightx = 0;
            gbc.weighty = 0;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.EAST;
            add(percentage, gbc);
        }
        
        void update(String title, int percentage) {
            this.title.setText(title);
            this.percentage.setText(percentage + "%");
        }
        
        
        
    }

}
