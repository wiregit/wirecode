package org.limewire.ui.swing.search;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

/**
 * Display panel for an active filter.
 */
class ActiveFilterPanel extends JPanel {

    // TODO create resources
    private Color removeForeColor = Color.RED;
    
    private final JLabel label = new JLabel();
    private final JButton removeButton = new JButton();

    /**
     * Constructs an ActiveFilterPanel with the specified remove action.
     */
    public ActiveFilterPanel(Action removeAction) {
        setLayout(new MigLayout("insets 0 0 0 0, gap 0!", 
                "[left][right]", ""));
        setOpaque(false);
        
        label.setHorizontalAlignment(JLabel.LEADING);
        label.setText((String) removeAction.getValue(Action.NAME));
        
        removeButton.setAction(removeAction);
        removeButton.setBorder(BorderFactory.createEmptyBorder());
        removeButton.setContentAreaFilled(false);
        removeButton.setFont(removeButton.getFont().deriveFont(Font.BOLD));
        removeButton.setForeground(removeForeColor);
        removeButton.setText("x");
        
        // Add listener to show cursor on mouse over.
        removeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                e.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                e.getComponent().setCursor(Cursor.getDefaultCursor());
            }
        });
        
        add(label       , "wmax 126");
        add(removeButton, "gap 5 5");
    }
    
}
