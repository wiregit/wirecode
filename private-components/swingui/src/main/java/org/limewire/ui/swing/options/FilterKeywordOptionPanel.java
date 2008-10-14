package org.limewire.ui.swing.options;

import javax.swing.Action;
import javax.swing.JButton;

import net.miginfocom.swing.MigLayout;

import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.util.I18n;

public class FilterKeywordOptionPanel extends OptionPanel {

    private JButton okButton;
    
    public FilterKeywordOptionPanel(Action okAction) {
        setLayout(new MigLayout("gapy 10"));
        
        okButton = new JButton(okAction);
        
        add(new MultiLineLabel(I18n.tr("LimeWire will not show files with the following keywords in your search results"), 300), "span, wrap");
    
        add(okButton, "skip 1, alignx right");
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
    public void initOptions() {
        // TODO Auto-generated method stub
        
    }

}
