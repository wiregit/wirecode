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
    
    // The icon displayed on the left side of the text field,
    // supplied by the call to GuiUtils.assignResources().
    // TODO: RMV We need to change this so the icon to be displayed
    // TODO: RMV is specified by the class that creates this component
    // TODO: RMV so that different icons can be used.
    @Resource private Icon icon;
    
    private JButton magButton;
    private String defaultText = "Filter results...";
    
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
        
        magButton = new JButton(icon);
        
        focusLost(null);
        addFocusListener(this);
    }
    
    /**
     * Repaints this component when focus is gained
     * so default text can be removed.
     */
    @Override
    public void focusGained(FocusEvent e) {
        repaint();
    }
    
    /**
     * Repaints this component when focus is lost
     * so default text can be displayed if no text has been entered.
     */
    @Override
    public void focusLost(FocusEvent e) {
        repaint();
    }
    
    /**
     * Gets the default text that is displayed when this component
     * doesn't have focus and doesn't contain any text.
     * @return the default text
     */
    public String getDefaultText() {
        return defaultText;
    }

    /**
     * @return the size of this component
     */
    @Override
    public Dimension getSize() {
        Dimension size = super.getSize();
        if(icon != null) {
            size.width += icon.getIconWidth();
        }
        return size;
    }

    /**
     * Paints this component, including an icon and
     * the default text when this component has focus and has no text value.
     */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        
        int iconWidth = icon == null ? getHeight() : icon.getIconWidth();
        if (icon == null) {
            // TODO: RMV How should not finding the icon be handled?
            // Draw an X since we don't have an icon.
            g2d.drawLine(0, 0, iconWidth, iconWidth);
            g2d.drawLine(0, iconWidth, iconWidth, 0);
        } else {
            Image image = ((ImageIcon) icon).getImage();
            g2d.drawImage(image, 0, 0, Color.WHITE, null);
        }
        
        g2d.translate(iconWidth, 0);
        super.paintComponent(g2d);
        
        boolean valueEntered = getText().length() > 0;
        if (!hasFocus() && !valueEntered) {
            g2d.setColor(Color.LIGHT_GRAY);
            FontMetrics fm = g2d.getFontMetrics();
            int x = 2; // using previous translate
            int y = fm.getAscent() + 2;
            g2d.drawString(defaultText, x, y);
        }
        
        g2d.dispose();
    }

    /**
     * Sets the default text that is displayed when this component
     * doesn't have focus and doesn't contain any text.
     * @param defaultText the default text
     */
    public void setDefaultText(String defaultText) {
        this.defaultText = defaultText;
    }
}
