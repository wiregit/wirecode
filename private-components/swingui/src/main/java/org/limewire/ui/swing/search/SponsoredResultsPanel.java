package org.limewire.ui.swing.search;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.limewire.ui.swing.util.FontUtils;

public class SponsoredResultsPanel extends JPanel {
    
    private GridBagConstraints gbc = new GridBagConstraints();
    
    public SponsoredResultsPanel() {
        setLayout(new GridBagLayout());
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        
        JLabel title = new JLabel("Sponsored Results");
        FontUtils.changeSize(title, 2);
        FontUtils.bold(title);
        add(title, gbc);
        
        gbc.insets.top = 10; // leave space above each entry that follow
        gbc.insets.left = 5; // indent entries a little
    }
    
    public void addEntry(String entry) {
        JTextArea textArea = new JTextArea(entry);
        textArea.setEditable(false);
        FontUtils.changeSize(textArea, -3);
        add(textArea, gbc);
    }
}
