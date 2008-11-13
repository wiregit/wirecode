package org.limewire.ui.swing.advanced;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.limewire.ui.swing.util.LogUtils;

import com.google.inject.Inject;

/**
 * An Advanced Tools tab panel that displays the console.
 */
public class ConsolePanel extends JPanel {
    
    private BorderLayout panelLayout = new BorderLayout();
    private JLabel naLabel = new JLabel();
    
    private Console console;

    /**
     * Constructs a ConsolePanel with the specified Console component. 
     */
    @Inject
    public ConsolePanel(Console console) {
        this.console = console;
        initComponents();
    }
    
    /**
     * Initializes the components in the container.
     */
    private void initComponents() {
        setLayout(panelLayout);
        
        if (LogUtils.isLog4JAvailable()) {
            add(console, BorderLayout.CENTER);
            
        } else {
            naLabel.setText("Console not available");
            naLabel.setHorizontalAlignment(JLabel.CENTER);
            naLabel.setVerticalAlignment(JLabel.CENTER);
            add(naLabel, BorderLayout.CENTER);
        }
    }
    
}
