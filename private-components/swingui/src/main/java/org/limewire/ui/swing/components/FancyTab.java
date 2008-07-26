package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JToggleButton;

import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.util.FontUtils;

/**
 * A fancy 'tab' for use in a {@link FancyTabList}.
 */
public class FancyTab extends JXPanel {

    private final Action action;
    private final TextButton button;
    private boolean highlighted;
    private FancyTabProperties props;

    public FancyTab(Action action, ButtonGroup group, FancyTabProperties fancyTabProperties) {
        this.action = action;
        this.props = fancyTabProperties;
        this.button = new TextButton(action, group, this);
            
        setOpaque(false);
        setLayout(new GridBagLayout());
        setToolTipText(getTitle());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 1;
        add(button, gbc);
        
        addMouseListener(new HighlightListener());
    }

    /** Gets the action underlying this tab. */
    Action getAction() {
        return action;
    }
    
    /** Selects this tab. */
    void setSelected(boolean selected) {
        button.setSelected(selected);
    }

    /** Returns true if this tab is selected. */
    boolean isSelected() {
        return button.isSelected();
    }

    /** Sets the foreground color of the tab. */
    void setButtonForeground(Color color) {
        button.setForeground(color);
    }

    /** Returns true if the tab is currently highlighted (in a rollover). */
    boolean isHighlighted() {
        return highlighted;
    }

    /** Removes this tab from the button group. */
    void removeFromGroup(ButtonGroup group) {
        group.remove(button);
    }

    /** The actual button. */
    private class TextButton extends JToggleButton {
        private final JXPanel parent;

        public TextButton(Action action, ButtonGroup group, JXPanel parentPanel) {
            super(action);
            this.parent = parentPanel;
            group.add(this);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setMargin(new Insets(2, 5, 2, 5));
            setToolTipText(getTitle());

            if (props.getTextFont() != null) {
                setFont(props.getTextFont());
            } else {
                FontUtils.changeStyle(this, Font.BOLD);
                FontUtils.changeSize(this, 2);
            }

            addPropertyChangeListener(Action.NAME, new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    fireCurrentState();
                }
            });

            addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        FontUtils.removeUnderline(TextButton.this);
                        setForeground(props.getSelectionColor());
                        parent.setBackgroundPainter(props.getSelectedPainter());
                    } else {
                        FontUtils.underline(TextButton.this);
                        setForeground(props.getNormalColor());
                        parent.setBackgroundPainter(props.getNormalPainter());
                    }
                }
            });
            
            addMouseListener(new HighlightListener());

            // Make sure we get the initial state down.
            fireCurrentState();
        }

        private void fireCurrentState() {
            fireItemStateChanged(new ItemEvent(TextButton.this, ItemEvent.ITEM_STATE_CHANGED,
                    TextButton.this, isSelected() ? ItemEvent.SELECTED : ItemEvent.DESELECTED));
        }
    }
    
    private class HighlightListener extends MouseAdapter {
        @Override
        public void mouseEntered(MouseEvent e) {
            if (!isSelected() && button.isEnabled()) {
                getTopLevelAncestor().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                highlighted = true;
                setBackgroundPainter(props.getHighlightPainter());
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            getTopLevelAncestor().setCursor(Cursor.getDefaultCursor());
            highlighted = false;

            if (!isSelected()) {
                setBackgroundPainter(props.getNormalPainter());
            }
        }
        
        @Override
        public void mouseClicked(MouseEvent e) {
            if(e.getSource() != button) {
                button.doClick();
            }
        }
    }

    public String getTitle() {
        return (String)action.getValue(Action.NAME);
    }

}
