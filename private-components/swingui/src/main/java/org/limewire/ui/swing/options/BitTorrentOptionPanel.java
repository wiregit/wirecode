package org.limewire.ui.swing.options;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import net.miginfocom.swing.MigLayout;

import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;


/**
 * BitTorrent Option View
 */
public class BitTorrentOptionPanel extends OptionPanel {
    
    private ButtonGroup buttonGroup;
    
    private JRadioButton limewireControl;
    
    @Inject
    public BitTorrentOptionPanel() {
        setLayout(new MigLayout("insets 15 15 15 15, fillx, wrap", "", ""));
        
        add(getBitTorrentPanel(), "pushx, growx");
    }

    private JPanel getBitTorrentPanel() {
        JPanel p = new JPanel();
        p.setBorder(BorderFactory.createTitledBorder(""));
        p.setLayout(new MigLayout("gapy 10"));
        
        limewireControl = new JRadioButton(I18n.tr("Let LimeWire manage my BitTorrent settings (Recommended)"));
        
        buttonGroup = new ButtonGroup();
        buttonGroup.add(limewireControl);
        
        p.add(limewireControl, "wrap");        

        return p;
    }
    
    @Override
    boolean applyOptions() {
        return false;
    }
    
    @Override
    boolean hasChanged() {
        return false;
    }
    
    @Override
    public void initOptions() {
        limewireControl.setSelected(true);
    }
    
}
