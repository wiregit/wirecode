package org.limewire.ui.swing.advanced;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

import com.google.inject.Inject;

/**
 * An Advanced Tools tab panel that displays log messages.
 */
public class LoggingPanel extends JPanel {
    
    private BorderLayout panelLayout = new BorderLayout();
    private JLabel todoLabel = new JLabel();

    /**
     * Constructs a LoggingPanel. 
     */
    @Inject
    public LoggingPanel() {
        initComponents();
    }
    
    /**
     * Initializes the components in the container.
     */
    private void initComponents() {
        setLayout(panelLayout);
        
        todoLabel.setHorizontalAlignment(JLabel.CENTER);
        todoLabel.setText("TODO logging");
        
        add(todoLabel, BorderLayout.CENTER);
    }

}
