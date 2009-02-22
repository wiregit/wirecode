package org.limewire.ui.swing.search;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.util.I18n;

public class ProgramsNotAllowedPanel extends JXPanel {
    
    public ProgramsNotAllowedPanel() {
        setLayout(new MigLayout("fill"));
        add(new MultiLineLabel(I18n.tr("To protect against viruses, LimeWire has disabled showing search results for programs. " +
                "To change this option, go to Options, choose Security, and configure your 'Unsafe Categories'.")),
                "grow, wmax 300, align center, gaptop 20");
    }

}
