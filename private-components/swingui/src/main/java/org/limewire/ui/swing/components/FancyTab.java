package org.limewire.ui.swing.components;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.LayoutStyle;
import javax.swing.SwingUtilities;
import javax.swing.JToggleButton.ToggleButtonModel;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXBusyLabel;
import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * A fancy 'tab' for use in a {@link FancyTabList}.
 */
public class FancyTab extends JXPanel {

    private static final int GAP = 2;

    private final TabActionMap tabActions;
    private final AbstractButton mainButton;
    private final AbstractButton removeButton;
    private final JXBusyLabel busyLabel;
    private final JLabel additionalText;
    private final FancyTabProperties props;
    
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

    public FancyTab(TabActionMap actionMap,
            ButtonGroup group,
            FancyTabProperties fancyTabProperties) {
        
        GuiUtils.assignResources(this);
        
        this.tabActions = actionMap;
        this.props = fancyTabProperties;
        this.mainButton = createMainButton();
        this.additionalText = createAdditionalText();
        this.removeButton = createRemoveButton();
        this.busyLabel = createBusyLabel();

        if(group != null) {
            group.add(mainButton);
        }

        mainButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                changeState(e.getStateChange() == ItemEvent.SELECTED ?
                    TabState.SELECTED : TabState.BACKGROUND);
            }
        });
            
        setOpaque(false);
        setToolTipText(getTitle());
        
        HighlightListener highlightListener = new HighlightListener();
        if(props.isRemovable()) {
            removeButton.addMouseListener(highlightListener);
            removeButton.setVisible(true);
        }
        
        addMouseListener(highlightListener);
        mainButton.addMouseListener(highlightListener);
        
        changeState(isSelected() ? TabState.SELECTED : TabState.BACKGROUND);
        
        layoutComponents();
    }
    
    private void layoutComponents() {
        GroupLayout layout = new GroupLayout(this);
        setLayout(layout);
        
        // RMV commented this out to reduce horizontal space in SearchTabItems.
        //layout.setAutoCreateGaps(true);
        
        layout.setHorizontalGroup(layout.createSequentialGroup()
            .addGap(GAP)
            .addComponent(busyLabel)
            .addComponent(mainButton, 0, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE)
            .addComponent(additionalText)
            .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(removeButton)
            .addGap(GAP)
            );
        
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.CENTER, true)
            .addComponent(busyLabel, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(mainButton, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(additionalText, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(removeButton, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            );
    }
    
    @Override
    public String toString() {
        return "FancyTab for: " + getTitle() + ", " + super.toString();
    }
    
    JXBusyLabel createBusyLabel() {
        final JXBusyLabel busy = new JXBusyLabel(new Dimension(16, 16));
        busy.setVisible(false);
        
        if(tabActions.getMainAction().getValue(TabActionMap.BUSY_KEY) ==
            Boolean.TRUE) {
            busy.setBusy(true);
            busy.setVisible(true);
        }
        
        tabActions.getMainAction().addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if(evt.getPropertyName().equals(TabActionMap.BUSY_KEY)) {
                    boolean on = evt.getNewValue() == Boolean.TRUE;
                    busy.setBusy(on);
                    busy.setVisible(on);
                }
            }
        });
        return busy;
    }
    
    JLabel createAdditionalText() {
        final JLabel label = new JLabel();
        label.setVisible(false);
        
        if(tabActions.getMoreTextAction() != null) {
            label.setOpaque(false);
            label.setFont(mainButton.getFont());
            
            String name = (String)tabActions.getMoreTextAction().getValue(Action.NAME);
            if(name != null) {
                label.setText("(" + name + ")");
                label.setVisible(true);
            }
            
            tabActions.getMoreTextAction().addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if(evt.getPropertyName().equals(Action.NAME)) {
                        label.setText("(" + (String)evt.getNewValue() + ")");
                        label.setVisible(true);
                    }
                }
            });
        }
        
        return label;
    }
    
    JButton createRemoveButton() {
        JButton button = new JButton();
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setRolloverEnabled(true);
        button.setIcon(removeBackgroundIcon);
        button.setSelectedIcon(removeSelectedIcon);
        button.setRolloverIcon(removeRolloverIcon);
        button.setPressedIcon(removeArmedIcon);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setAction(tabActions.getRemoveAction());
        button.setActionCommand(TabActionMap.REMOVE_COMMAND);
        button.setHideActionText(true);
        button.setVisible(false);
        if(removeButton != null) {
            for(ActionListener listener : removeButton.getActionListeners()) {
                if(listener == tabActions.getRemoveAction()) {
                    // Ignore the remove action -- it's added implicitly.
                    continue;
                }
                button.addActionListener(listener);
            }
        }
        return button;
    }
    
    AbstractButton createMainButton() {
        AbstractButton button = new JToggleButton();
        button.setModel(new NoToggleModel());
        button.setAction(tabActions.getMainAction());
        button.setActionCommand(TabActionMap.SELECT_COMMAND);
        button.setBorder(null);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setToolTipText(getTitle());

        if (props.getTextFont() != null) {
            button.setFont(props.getTextFont());
        } else {
            FontUtils.changeStyle(button, Font.BOLD);
            FontUtils.changeSize(button, 2);
        }
        
        return button;
    }
    
    void remove() {
        removeButton.doClick(0);
    }
    
    void select() {
        mainButton.doClick(0);
    }
    
    void addRemoveActionListener(ActionListener listener) {
        removeButton.addActionListener(listener);
    }
    
    void removeRemoveActionListener(ActionListener listener) {
        removeButton.removeActionListener(listener);
    }

    /** Gets the action underlying this tab. */
    TabActionMap getTabActionMap() {
        return tabActions;
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
        if(currentState != tabState) {
            this.currentState = tabState;
            switch(tabState) {
            case SELECTED:
                FontUtils.removeUnderline(mainButton);
                FontUtils.removeUnderline(additionalText);
                mainButton.setForeground(props.getSelectionColor());
                additionalText.setForeground(props.getSelectionColor());
                this.setBackgroundPainter(props.getSelectedPainter());
                removeButton.setIcon(removeSelectedIcon);
                break;
            case BACKGROUND:
                FontUtils.underline(mainButton);
                FontUtils.underline(additionalText);
                mainButton.setForeground(props.getNormalColor());
                additionalText.setForeground(props.getNormalColor());
                this.setBackgroundPainter(props.getNormalPainter());
                removeButton.setIcon(removeBackgroundIcon);
                break;
            case ROLLOVER:
                setBackgroundPainter(props.getHighlightPainter());
                removeButton.setIcon(removeSelectedIcon);
                break;
            }
        }
    }

    public String getTitle() {
        return (String)tabActions.getMainAction().getValue(Action.NAME);
    }
    
    private void showPopup(MouseEvent event) {
        if(props.isRemovable() || !tabActions.getRightClickActions().isEmpty()) {
            JPopupMenu menu = new JPopupMenu();
            for(Action action : tabActions.getRightClickActions()) {
                menu.add(action);
            }
            
            if(menu.getComponentCount() != 0 && props.isRemovable()) {
                menu.addSeparator();
            }
            
            if(props.isRemovable()) {
                menu.add(new AbstractAction(props.getCloseOneText()) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        remove();
                    }
                });
                menu.add(tabActions.getRemoveOthers());
                menu.addSeparator();
                menu.add(tabActions.getRemoveAll());
            }
            
            menu.show((Component)event.getSource(), event.getX()+3, event.getY()+3);
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
            if(props.isRemovable() && SwingUtilities.isMiddleMouseButton(e)) {
                remove();
            } else if(!(e.getSource() instanceof AbstractButton) && SwingUtilities.isLeftMouseButton(e)) {
                select();
            } else if(e.isPopupTrigger()) {
                showPopup(e);
            }
        }
        
        @Override
        public void mousePressed(MouseEvent e) {
            if(e.isPopupTrigger()) {
                showPopup(e);
            }
        }
        
        @Override
        public void mouseReleased(MouseEvent e) {
            if(e.isPopupTrigger()) {
                showPopup(e);
            }
        }
    }    
    
    private static class NoToggleModel extends ToggleButtonModel {
        @Override
        public void setPressed(boolean b) {
            if ((isPressed() == b) || !isEnabled()) {
                return;
            }

            // This is different than the super in that
            // we only go from false -> true, not true -> false.
            if (!b && isArmed() && !isSelected()) {
                setSelected(true);
            } 

            if (b) {
                stateMask |= PRESSED;
            } else {
                stateMask &= ~PRESSED;
            }

            fireStateChanged();

            if(!isPressed() && isArmed()) {
                int modifiers = 0;
                AWTEvent currentEvent = EventQueue.getCurrentEvent();
                if (currentEvent instanceof InputEvent) {
                    modifiers = ((InputEvent)currentEvent).getModifiers();
                } else if (currentEvent instanceof ActionEvent) {
                    modifiers = ((ActionEvent)currentEvent).getModifiers();
                }
                fireActionPerformed(
                    new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
                                    getActionCommand(),
                                    EventQueue.getMostRecentEventTime(),
                                    modifiers));
            }
        }
    }
}
