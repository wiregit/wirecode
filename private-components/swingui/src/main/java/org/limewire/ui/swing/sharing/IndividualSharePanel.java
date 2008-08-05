package org.limewire.ui.swing.sharing;

import javax.swing.JLabel;
import javax.swing.JPanel;

public class IndividualSharePanel extends JPanel {
    public static final String NAME = "Individual Shared";
    
    public IndividualSharePanel() {
        add(new JLabel(NAME));
    }
}
