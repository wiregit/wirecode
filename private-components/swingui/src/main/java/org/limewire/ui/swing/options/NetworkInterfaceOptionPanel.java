package org.limewire.ui.swing.options;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import net.miginfocom.swing.MigLayout;

import org.limewire.ui.swing.util.I18n;

/**
 * Network Interface Option View
 */
public class NetworkInterfaceOptionPanel extends OptionPanel {

    private ButtonGroup buttonGroup;
    
    private JRadioButton limewireChooseRadioButton;
    private JRadioButton meChooseRadioButton;
    
    public NetworkInterfaceOptionPanel() {
        setLayout(new MigLayout("insets 15 15 15 15, fillx, wrap", "", ""));
        
        add(getNetworkPanel(), "pushx, growx");
    }
    
    private JPanel getNetworkPanel() {
        JPanel p = new JPanel();
        p.setBorder(BorderFactory.createTitledBorder(""));
        p.setLayout(new MigLayout("gapy 10"));
        
        limewireChooseRadioButton = new JRadioButton();
        meChooseRadioButton = new JRadioButton();
        
        buttonGroup = new ButtonGroup();
        buttonGroup.add(limewireChooseRadioButton);
        buttonGroup.add(meChooseRadioButton);

        p.add(limewireChooseRadioButton);
        p.add(new JLabel(I18n.tr("Let LimeWire choose my network interface (Recommended)")), "wrap");
        
        p.add(meChooseRadioButton);
        p.add(new JLabel(I18n.tr("Let me choose a specific network interface")), "wrap");
        
        return p;
    }
    
    @Override
    void applyOptions() {
        // TODO Auto-generated method stub
        
    }

    @Override
    boolean hasChanged() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    void initOptions() {
        // TODO Auto-generated method stub
        
    }

}
