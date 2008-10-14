package org.limewire.ui.swing.options;

import java.awt.Color;

import javax.swing.JTabbedPane;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import net.miginfocom.swing.MigLayout;

/**
 * Advanced Option View.
 */
@Singleton
public class AdvancedOptionPanel extends OptionPanel {

    private FilesOptionPanel filesOptionPanel;
    private ConnectionsOptionPanel connectionsOptionPanel;
    private SystemOptionPanel systemOptionPanel;
    private ReallyAdvancedOptionPanel reallyAdvancedOptionPanel;
    
    @Inject
    public AdvancedOptionPanel(FilesOptionPanel filesOptionPanel, ConnectionsOptionPanel connectionsOptionPanel,
                    SystemOptionPanel systemOptionPanel, ReallyAdvancedOptionPanel reallyAdvancedOptionPanel) {
        
        this.filesOptionPanel = filesOptionPanel;
        this.connectionsOptionPanel = connectionsOptionPanel;
        this.systemOptionPanel = systemOptionPanel;
        this.reallyAdvancedOptionPanel = reallyAdvancedOptionPanel;
        
        setLayout(new MigLayout("insets 15 15 15 15, fillx", "", ""));
        
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(Color.WHITE);
        tabbedPane.addTab("Files", filesOptionPanel);
        tabbedPane.addTab("Connections", connectionsOptionPanel);
        tabbedPane.addTab("System", systemOptionPanel);
        tabbedPane.addTab("Super Really Advanced", reallyAdvancedOptionPanel);
        
        add(tabbedPane, "grow");
    }

    @Override
    void applyOptions() {
        filesOptionPanel.applyOptions();
        connectionsOptionPanel.applyOptions();
        systemOptionPanel.applyOptions();
        reallyAdvancedOptionPanel.applyOptions();
    }

    @Override
    boolean hasChanged() {
        return filesOptionPanel.hasChanged() || 
        connectionsOptionPanel.hasChanged() ||
                systemOptionPanel.hasChanged() ||
                reallyAdvancedOptionPanel.hasChanged();
    }

    @Override
    public void initOptions() {
        filesOptionPanel.initOptions();
        connectionsOptionPanel.initOptions();
        systemOptionPanel.initOptions();
        reallyAdvancedOptionPanel.initOptions();
    }
}
