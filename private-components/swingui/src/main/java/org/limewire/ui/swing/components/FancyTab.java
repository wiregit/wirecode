package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JToggleButton;
import javax.swing.LayoutStyle;
import javax.swing.SwingConstants;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * A fancy 'tab' for use in a {@link FancyTabList}.
 */
public class FancyTab extends JXPanel {

    private final TabActionMap tabActions;
    private final TextButton mainButton;
    private final JButton removeButton;
    private final JLabel additionalText;
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

    public FancyTab(TabActionMap actionMap, ButtonGroup group, FancyTabProperties fancyTabProperties) {
        GuiUtils.assignResources(this);
        
        this.tabActions = actionMap;
        this.props = fancyTabProperties;
        this.mainButton = new TextButton(actionMap.getSelectAction());
        if(group != null) {
            group.add(mainButton);
        }
            
        setOpaque(false);
        setToolTipText(getTitle());
        HighlightListener highlightListener = new HighlightListener();
        
        additionalText = new JLabel();
        additionalText.setVisible(false);
        
        if(actionMap.getMoreTextAction() != null) {
            additionalText.setOpaque(false);
            additionalText.setHorizontalAlignment(SwingConstants.LEADING);
            additionalText.setFont(props.getTextFont());
            
            String name = (String)actionMap.getMoreTextAction().getValue(Action.NAME);
            if(name != null) {
                additionalText.setText(name);
                additionalText.setVisible(true);
            }
            actionMap.getMoreTextAction().addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if(evt.getPropertyName().equals(Action.NAME)) {
                        additionalText.setText("(" + (String)evt.getNewValue() + ")");
                        additionalText.setVisible(true);
                    }
                }
            });
            additionalText.addMouseListener(highlightListener);
        }
        
        
        removeButton = createRemoveButton();
        removeButton.setVisible(false);
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
        
        mainButton.setHorizontalAlignment(SwingConstants.LEADING);
        additionalText.setHorizontalAlignment(SwingConstants.LEADING);
        removeButton.setHorizontalAlignment(SwingConstants.TRAILING);
        
        mainButton.setVerticalAlignment(SwingConstants.CENTER);
        additionalText.setVerticalAlignment(SwingConstants.CENTER);
        removeButton.setVerticalAlignment(SwingConstants.CENTER);
        
        layout.setAutoCreateGaps(true);
        
        layout.setHorizontalGroup(layout.createSequentialGroup()
                .addComponent(mainButton, 0, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE)
                .addComponent(additionalText)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(removeButton)
                .addGap(5)
                );
        
        layout.setVerticalGroup(
                    layout.createParallelGroup(GroupLayout.Alignment.CENTER, true)
                    .addComponent(mainButton, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(additionalText, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(removeButton, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                );
    }
    
    @Override
    public String toString() {
        return "FancyTab for: " + getTitle() + ", " + super.toString();
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
        return button;
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
    }

    /** The actual button. */
    private class TextButton extends JToggleButton {
        public TextButton(Action action) {
            super(action);
            setActionCommand(TabActionMap.SELECT_COMMAND);
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
        return (String)tabActions.getSelectAction().getValue(Action.NAME);
    }
}
