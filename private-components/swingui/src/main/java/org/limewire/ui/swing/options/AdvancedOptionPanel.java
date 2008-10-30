package org.limewire.ui.swing.options;

import java.awt.Color;

import javax.swing.JTabbedPane;

import org.limewire.ui.swing.util.I18n;

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
        tabbedPane.addTab(I18n.tr("Files"), filesOptionPanel);
        tabbedPane.addTab(I18n.tr("Connections"), connectionsOptionPanel);
        tabbedPane.addTab(I18n.tr("System"), systemOptionPanel);
        tabbedPane.addTab(I18n.tr("Super Really Advanced"), reallyAdvancedOptionPanel);
        
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
