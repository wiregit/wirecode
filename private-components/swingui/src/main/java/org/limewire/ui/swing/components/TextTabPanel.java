package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JToggleButton;

import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.Painter;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.ui.swing.util.FontUtils;

public class TextTabPanel extends JXPanel {
    
    private final List<ButtonBed> buttonBeds = new ArrayList<ButtonBed>();
    private final ButtonGroup buttonGroup = new ButtonGroup();
    
    private Painter<?> highlightPainter;
    private Color selectionColor;
    private Font textFont;
    private Insets buttonInsets;
    
    public TextTabPanel(Iterable<? extends Action> actions) {
        setOpaque(false);
        setLayout(new GridBagLayout());
        highlightPainter = new RectanglePainter<JXPanel>(2, 2, 2, 2, 5, 5, true, Color.WHITE, 0f, Color.LIGHT_GRAY);
        selectionColor = new Color(0, 100, 0);
        buttonInsets = new Insets(0, 1, 5, 1);
        setActions(actions);
    }
    
    public TextTabPanel(Action... actions) {
        this(Arrays.asList(actions));
    }
    
    public void setButtonInsets(Insets insets) {
        buttonInsets = insets;
        removeAll();
        addButtonBedsToPanel();
    }
    
    public void setActions(Iterable<? extends Action> actions) {
        clearButtons();

        for(Action action : actions) {
            buttonBeds.add(new ButtonBed(action, buttonGroup));
        }
        addButtonBedsToPanel();        
    }
    
    public void setActions(Action... actions) {
        setActions(Arrays.asList(actions));
    }
    
    private void addButtonBedsToPanel() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = buttonInsets;
        for(ButtonBed bed : buttonBeds) {
            add(bed, gbc);
        }
    }
    
    public void clearButtons() {
        removeAll();        
        for(ButtonBed bed : buttonBeds) {
            buttonGroup.remove(bed.getButton());
        }
        buttonBeds.clear();
    }
    
    public void setHighlightPainter(Painter<?> painter) {
        for(ButtonBed bed : buttonBeds) {
            if(bed.isHighlighted()) {
                bed.setHighlight(highlightPainter);
            }
        }
        this.highlightPainter = painter;
    }
    
    public void setSelectionColor(Color color) {
        for(ButtonBed bed : buttonBeds) {
            if(bed.isSelected()) {
                bed.setButtonForeground(color);
            }
        }
        this.selectionColor = color;
    }
    
    public void setTextFont(Font font) {
        for(ButtonBed bed : buttonBeds) {
            bed.setFont(font);
        }
    }
    
    private class ButtonBed extends JXPanel {
        private final TextButton button;
        
        public ButtonBed(Action action, ButtonGroup group) {
            this.button = new TextButton(action, group, this);
            
            setOpaque(false);
            setBackgroundPainter(null);
            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            add(button, gbc);
        }
        
        void setSelected(boolean selected) {
            button.setSelected(selected);
        }
        
        boolean isSelected() {
            return button.isSelected();
        }
        
        void setButtonForeground(Color color) {
            button.setForeground(color);
        }
        
        boolean isHighlighted() {
            return getBackgroundPainter() != null;
        }
        
        void setHighlight(Painter<?> painter) {
            setBackgroundPainter(painter);
        }
        
        AbstractButton getButton() {
            return button;
        }
    }
    
    private class TextButton extends JToggleButton {
        private final JXPanel parent;
        
        public TextButton(Action action, ButtonGroup group, JXPanel parentPanel) {
            super(action);
            this.parent = parentPanel;
            group.add(this);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setMargin(new Insets(2, 5, 2, 5));
            if(textFont != null) {
                setFont(textFont);
            } else {
                FontUtils.changeFontStyle(this, Font.BOLD);
                FontUtils.changeFontSize(this, 2);
            }
            addItemListener(new ItemListener() {
                private Color oldForeground;
                
                @Override
                public void itemStateChanged(ItemEvent e) {
                    if(e.getStateChange() == ItemEvent.SELECTED) {
                        oldForeground = getForeground();
                        setForeground(selectionColor);
                        parent.setBackgroundPainter(null);
                    } else {
                        setForeground(oldForeground);
                        oldForeground = null;
                    }
                }
            });
            
            addMouseListener(new MouseAdapter() {
               @Override
                public void mouseEntered(MouseEvent e) {
                   if(!isSelected() && getAction().isEnabled()) {
                       parent.setBackgroundPainter(highlightPainter);
                   }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if(!isSelected()) {
                        parent.setBackgroundPainter(null);
                    }
                }
            });
            
            // Make sure we get the initial state down.
            if(isSelected()) {
                fireItemStateChanged(new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED, this, ItemEvent.SELECTED));
            }
        }
    }

}
