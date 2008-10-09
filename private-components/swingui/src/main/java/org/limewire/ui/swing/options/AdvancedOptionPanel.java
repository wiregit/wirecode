package org.limewire.ui.swing.options;

import java.awt.Color;

import javax.swing.JTabbedPane;

import net.miginfocom.swing.MigLayout;

/**
 * Advanced Option View.
 */
public class AdvancedOptionPanel extends OptionPanel {

    private FilesOptionPanel filesOptionPanel;
    private ConnectionsOptionPanel connectionsOptionPanel;
    private SystemOptionPanel systemOptionPanel;
    private ReallyAdvancedOptionPanel reallyAdvancedOptionPanel;
    
    public AdvancedOptionPanel() {
        setLayout(new MigLayout("insets 15 15 15 15, fillx", "", ""));
        
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(Color.WHITE);
        tabbedPane.addTab("Files", getFilesOptionPanel());
        tabbedPane.addTab("Connections", getConnectionOptionPanel());
        tabbedPane.addTab("System", getSystemOptionPanel());
        tabbedPane.addTab("Super Really Advanced", getReallyAdvancedOptionPanel());
        
        add(tabbedPane, "grow");
    }
    
    private OptionPanel getFilesOptionPanel() {
        if(filesOptionPanel == null) {
            filesOptionPanel = new FilesOptionPanel();
            filesOptionPanel.setOpaque(false);
        }
        return filesOptionPanel;
    }
    
    private OptionPanel getConnectionOptionPanel() {
        if(connectionsOptionPanel == null) {
            connectionsOptionPanel = new ConnectionsOptionPanel();
            connectionsOptionPanel.setOpaque(false);
        }
        return connectionsOptionPanel;
    }
    
    private OptionPanel getSystemOptionPanel() {
        if(systemOptionPanel == null) {
            systemOptionPanel = new SystemOptionPanel();
            systemOptionPanel.setOpaque(false);
        }
        return systemOptionPanel;
    }
    
    private OptionPanel getReallyAdvancedOptionPanel() {
        if(reallyAdvancedOptionPanel == null) {
            reallyAdvancedOptionPanel = new ReallyAdvancedOptionPanel();
            reallyAdvancedOptionPanel.setOpaque(false);
        }
        return reallyAdvancedOptionPanel;
    }

    @Override
    void applyOptions() {
        getFilesOptionPanel().applyOptions();
        getConnectionOptionPanel().applyOptions();
        getSystemOptionPanel().applyOptions();
        getReallyAdvancedOptionPanel().applyOptions();
    }

    @Override
    boolean hasChanged() {
        return getFilesOptionPanel().hasChanged() || 
                getConnectionOptionPanel().hasChanged() ||
                getSystemOptionPanel().hasChanged() ||
                getReallyAdvancedOptionPanel().hasChanged();
    }

    @Override
    void initOptions() {
        getFilesOptionPanel().initOptions();
        getConnectionOptionPanel().initOptions();
        getSystemOptionPanel().initOptions();
        getReallyAdvancedOptionPanel().initOptions();
    }
}
