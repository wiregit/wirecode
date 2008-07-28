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
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JToggleButton;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * A fancy 'tab' for use in a {@link FancyTabList}.
 */
public class FancyTab extends JXPanel {

    private final Action selectedAction;
    private final TextButton mainButton;
    private final JButton removeButton;
    private FancyTabProperties props;
    
    private static enum TabState {
        BACKGROUND, ROLLOVER, SELECTED;
    }
    
    private TabState currentState;
    
    @Resource
    private Icon removeSelectedIcon;

    @Resource
    private Icon removeArmedIcon;

    @Resource
    private Icon removeRolloverIcon;
    
    //@Resource // Currently not picked up -- background is not shown.
    private Icon removeBackgroundIcon = null;
    

    public FancyTab(Action action, ButtonGroup group, FancyTabProperties fancyTabProperties) {
        GuiUtils.assignResources(this);
        
        this.selectedAction = action;
        this.props = fancyTabProperties;
        this.mainButton = new TextButton(action, group);
            
        setOpaque(false);
        setLayout(new GridBagLayout());
        setToolTipText(getTitle());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1;
        add(mainButton, gbc);
        
        HighlightListener highlightListener = new HighlightListener();
        
        removeButton = new JButton();
        if(props.isRemovable()) {
            removeButton.setBorderPainted(false);
            removeButton.setContentAreaFilled(false);
            removeButton.setFocusPainted(false);
            removeButton.setRolloverEnabled(true);
            removeButton.setIcon(removeBackgroundIcon);
            removeButton.setRolloverIcon(removeRolloverIcon);
            removeButton.setPressedIcon(removeArmedIcon);
            removeButton.setMargin(new Insets(0, 0, 0, 0));
            removeButton.addMouseListener(highlightListener);
            gbc.anchor = GridBagConstraints.EAST;
            gbc.weightx = 0;
            gbc.insets = new Insets(0, 0, 0, 3);
            add(removeButton, gbc);
        }
        
        addMouseListener(highlightListener);
        mainButton.addMouseListener(highlightListener);
        changeState(isSelected() ? TabState.SELECTED : TabState.BACKGROUND);
    }

    /** Gets the action underlying this tab. */
    Action getAction() {
        return selectedAction;
    }
    
    /** Selects this tab. */
    void setSelected(boolean selected) {
        mainButton.setSelected(selected);
    }

    /** Returns true if this tab is selected. */
    boolean isSelected() {
        return mainButton.isSelected();
    }

    /** Sets the foreground color of the tab. */
    void setButtonForeground(Color color) {
        mainButton.setForeground(color);
    }

    /** Returns true if the tab is currently highlighted (in a rollover). */
    boolean isHighlighted() {
        return currentState == TabState.ROLLOVER;
    }

    /** Removes this tab from the button group. */
    void removeFromGroup(ButtonGroup group) {
        group.remove(mainButton);
    }
    
    private void changeState(TabState tabState) {
        this.currentState = tabState;
        switch(tabState) {
        case SELECTED:
            FontUtils.removeUnderline(mainButton);
            mainButton.setForeground(props.getSelectionColor());
            this.setBackgroundPainter(props.getSelectedPainter());
            removeButton.setIcon(removeSelectedIcon);
            break;
        case BACKGROUND:
            FontUtils.underline(mainButton);
            mainButton.setForeground(props.getNormalColor());
            this.setBackgroundPainter(props.getNormalPainter());
            removeButton.setIcon(removeBackgroundIcon);
            break;
        case ROLLOVER:
            setBackgroundPainter(props.getHighlightPainter());
            removeButton.setIcon(removeSelectedIcon);
            break;
        }
    }

    /** The actual button. */
    private class TextButton extends JToggleButton {
        public TextButton(Action action, ButtonGroup group) {
            super(action);
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
                    changeState(isSelected() ? TabState.SELECTED : TabState.BACKGROUND);
                }
            });

            addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    changeState(e.getStateChange() == ItemEvent.SELECTED ? TabState.SELECTED : TabState.BACKGROUND);
                }
            });
        }
    }
    
    private class HighlightListener extends MouseAdapter {
        @Override
        public void mouseEntered(MouseEvent e) {
            if (!isSelected() && mainButton.isEnabled()) {
                getTopLevelAncestor().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                changeState(TabState.ROLLOVER);
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            getTopLevelAncestor().setCursor(Cursor.getDefaultCursor());            
            if (!isSelected()) {
                changeState(TabState.BACKGROUND);
            }
        }
        
        @Override
        public void mouseClicked(MouseEvent e) {
            if(e.getSource() != mainButton) {
                mainButton.doClick();
            }
        }
    }

    public String getTitle() {
        return (String)selectedAction.getValue(Action.NAME);
    }

}
