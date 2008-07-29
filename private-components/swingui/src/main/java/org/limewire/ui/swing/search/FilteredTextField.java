package org.limewire.ui.swing.search;

import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.*;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * This class is a special text field used for filtering search results.
 * 
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
class FilteredTextField extends JTextField implements FocusListener {
    
    private static final String DEFAULT_TEXT = "Filter results...";
    
    // The icon displayed on the left side of the text field,
    // supplied by the call to GuiUtils.assignResources().
    @Resource private Icon icon;
    
    private boolean valueEntered;

    /**
     * Creates a FilteredTextField that displays a given number of columns.
     * @param columns
     */
    FilteredTextField(int columns) {
        super(columns);
        
        // Cause the @Resource fields to be injected
        // using properties in AppFrame.properties.
        // The icon PNG file is in swingui/src/main/resources/
        // org/limewire/ui/swing/mainframe/resources/icons.
        GuiUtils.assignResources(this);
        
        focusLost(null);
        addFocusListener(this);
    }
    
    @Override
    public void focusGained(FocusEvent e) {
        if (!valueEntered) {
            setText("");
            setForeground(Color.BLACK);
        }
    }
    
    @Override
    public void focusLost(FocusEvent e) {
        valueEntered = getText().length() > 0;
        if (!valueEntered) {
            setForeground(Color.LIGHT_GRAY);
            setText(DEFAULT_TEXT);
        }
    }
    
    @Override
    public Dimension getSize() {
        Dimension size = super.getSize();
        size.width += icon.getIconWidth();
        return size;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        
        if (icon == null) {
            // TODO: How should not finding the icon be handled?
            g2d.drawLine(0, 0, getWidth(), getHeight());
            g2d.drawLine(0, getHeight(), getWidth(), 0);
        } else {
            Image image = ((ImageIcon) icon).getImage();
            g2d.drawImage(image, 0, 0, Color.WHITE, null);
        }
        
        g2d.translate(icon.getIconWidth(), 0);
        super.paintComponent(g2d);
        
        g2d.dispose();
    }
}
