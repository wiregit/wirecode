package org.limewire.ui.swing.options;

import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Filtering Option View
 */
@Singleton
public class FilteringOptionPanel extends OptionPanel {
    
    private BlockHostsPanel blockHostPanel;
    private AllowHostsPanel allowHostsPanel;
    
    @Inject
    public FilteringOptionPanel() {
        super();
        setLayout(new MigLayout("insets 10 10 10 10, fillx, wrap", "", ""));
        
        add(getBlockHostsPanel(), "pushx, growx");
        add(getAllowHostsPanel(), "pushx, growx");
    }
    
    private OptionPanel getBlockHostsPanel() {
        if(blockHostPanel == null) {
            blockHostPanel = new BlockHostsPanel();
        }
        return blockHostPanel;
    }
    
    private OptionPanel getAllowHostsPanel() {
        if(allowHostsPanel == null) {
            allowHostsPanel = new AllowHostsPanel();
        }
        return allowHostsPanel;
    }
    
    @Override
    void applyOptions() {
        getBlockHostsPanel().applyOptions();
        getAllowHostsPanel().applyOptions();
    }
    
    @Override
    boolean hasChanged() {
        return getBlockHostsPanel().hasChanged() || getAllowHostsPanel().hasChanged();
    }
    
    @Override
    public void initOptions() {
        getBlockHostsPanel().initOptions();
        getAllowHostsPanel().initOptions();
    }
    
    private class BlockHostsPanel extends OptionPanel {
    
        public BlockHostsPanel() {
            super(I18n.tr("Block Hosts"));
            
            add(new JLabel(I18n.tr("Block contact with specific people by adding their IP address")), "wrap");
        }
        
        @Override
        void applyOptions() {
    
        }
    
        @Override
        boolean hasChanged() {
            return false;
        }
    
        @Override
        public void initOptions() {
    
        }
    }
    
    private class AllowHostsPanel extends OptionPanel {
    
        public AllowHostsPanel() {
            super(I18n.tr("Allow Hosts"));
            
            add(new JLabel(I18n.tr("Override the block list and allow specific people by adding their IP address")), "wrap");
        }
        
        @Override
        void applyOptions() {
    
        }
    
        @Override
        boolean hasChanged() {
            return false;
        }
    
        @Override
        public void initOptions() {
    
        }
    }

}