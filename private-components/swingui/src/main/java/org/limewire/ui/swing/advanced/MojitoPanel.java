package org.limewire.ui.swing.advanced;

import java.awt.BorderLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.limewire.ui.swing.plugin.SwingUiPlugin;

import com.google.inject.Inject;
import com.google.inject.name.Named;

class MojitoPanel extends TabPanel {
    
    private SwingUiPlugin plugin;

    public MojitoPanel() {
        setLayout(new BorderLayout());
    }
    
    @Inject(optional=true) void register(@Named("MojitoArcsPlugin") SwingUiPlugin mojitoPlugin) {
        this.plugin = mojitoPlugin;
    }

    @Override
    public void start() {
        if(plugin != null) {
            JComponent render = plugin.getPluginComponent();
            if(render != null) {
                removeAll();
                add(render, BorderLayout.CENTER);
            } else {
                fail();
            }
            plugin.startPlugin();
        } else {
            fail();
        }        
    }
    
    private void fail() {
        removeAll();
        JLabel naLabel = new JLabel();
        naLabel.setText("Mojito Plugin not available");
        naLabel.setHorizontalAlignment(JLabel.CENTER);
        naLabel.setVerticalAlignment(JLabel.CENTER);
        add(naLabel, BorderLayout.CENTER);
    }

    @Override
    public void stop() {
        if(plugin != null) {
            plugin.stopPlugin();
        }
    }
}
