package org.limewire.ui.swing.search;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.limewire.ui.swing.components.MessageComponent;
import org.limewire.ui.swing.util.I18n;

public class ProgramsNotAllowedPanel extends JPanel {
    
    public ProgramsNotAllowedPanel() {
    
        super(new MigLayout("fill"));
        
        MessageComponent messageComponent = new MessageComponent(14, 22, 14, 6);
        
        JLabel message = new JLabel("<html>" + I18n.tr("To protect against viruses, LimeWire has disabled showing search results for programs. " +
            "To change this option, go to Options, choose Security, and configure your 'Unsafe Categories'.")+"</html>");
        
        messageComponent.decorateSubLabel(message);
        messageComponent.addComponent(message, "align center, wmax 420");
        
        this.add(messageComponent, "align 50% 40%");
    }

}
