package org.limewire.ui.swing.sharing;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.components.Circle;
import org.limewire.ui.swing.components.NumberedHyperLinkButton;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.SwingUtils;

import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

/**
 * Displays a set of hyperlinks to jump to the appropriate section that is
 * being shared. Each hyperlink displays the number of shared files of that
 * category next to it. Some hyperlinks are not always shown. For these
 * the hyperlink disappears when its category size == 0
 */
public class SharingShortcutPanel extends JPanel {

    @Resource
    private Color circleColor;
    @Resource
    private int circleSize;
    
    private String[] names;
    private List<FilterList<LocalFileItem>> models;
    
    private List<ButtonCircleComponent> buttons;
     
    public SharingShortcutPanel(String[] names, Action[] actions, List<FilterList<LocalFileItem>> models, boolean[] alwaysShown) {
        this.names = names;
        this.models = models;

        GuiUtils.assignResources(this);
        
        buttons = new ArrayList<ButtonCircleComponent>();
        
        setLayout(new MigLayout("hidemode 3, gapx 0, insets 18 0 10 0, ax 50%","",""));
        
        createComponents(actions, alwaysShown);
    }
    
    private void createComponents(Action[] actions, boolean[] alwaysShown) {
        for(int i = 0; i < names.length; i++) {
            ButtonCircleComponent buttonCircle;
            if(i == 0)
                buttonCircle = new ButtonCircleComponent(names[i], actions[i], models.get(i), CircleLocation.NONE, alwaysShown[i]);
            else
                buttonCircle = new ButtonCircleComponent(names[i], actions[i], models.get(i), CircleLocation.LEFT, alwaysShown[i]);
            
            buttons.add(buttonCircle);
            add(buttonCircle);
        }
    }
    
    /**
     * Sets the model for all the hyperlinks
     */
    public void setModel(List<FilterList<LocalFileItem>> model) {
        for(int i = 0; i < model.size(); i++) {
            buttons.get(i).setModel(model.get(i));
        }
        this.models = model;
    }
    
    /**
     * Location for the circle to drawn in relation to the hyperlink
     */
    public static enum CircleLocation {
        LEFT, RIGHT, NONE;
    }
    
    /**
     * Creates a hyperlink button. This hyperlink button jumps to the category 
     * of shared files it represents. Besides the hyperlink is the number of 
     * shared files for that category. The hyperlink can be chosen to only be
     * displayed if the size of shared files for that category > 0. 
     * 
     * A circle can also be chosen to be drawn to the left, right or not at all.
     */
    private class ButtonCircleComponent extends JPanel {
        
        private NumberedHyperLinkButton button;
        private FilterList<LocalFileItem> model;
        private SharingListEventListener listener;
        private boolean alwaysShow;
        
        public ButtonCircleComponent(String name, Action action, FilterList<LocalFileItem> model, CircleLocation loc, boolean alwaysShown) {
            
            this.model = model;
            this.alwaysShow = alwaysShown;
            createButton(name, action, alwaysShown);
            
            setLayout(new MigLayout("insets 0 0 0 0"));
            
            if(loc == CircleLocation.LEFT) {
                Circle circle = new Circle(circleSize);
                circle.setForeground(circleColor);
                add(circle, "gapafter 10");
                add(button, "gapafter 10");
            } else if(loc == CircleLocation.RIGHT) {
                add(button, "gapbefore 10");
                Circle circle = new Circle(circleSize);
                circle.setForeground(circleColor);
                add(circle, "gapbefore 10");
            } else
                add(button, "gapbefore 10, gapafter 10");
            setBorder(null);
            setVisible(alwaysShown);
        }
        
        private void createButton(String name, Action action, boolean alwaysShown) {
            button = new NumberedHyperLinkButton(name, action);
            button.setDisplayNumber(0);
            listener = new SharingListEventListener(alwaysShown);
            model.addListEventListener(listener);
        }
        
        public void setModel(FilterList<LocalFileItem> newModel) {
            if(model != null) {
                final FilterList<LocalFileItem> toRemove = model; 
                
                SwingUtils.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        toRemove.removeListEventListener(listener);
                    }
                });
            }
            this.model = newModel;
            model.addListEventListener(listener);
            button.setDisplayNumber(model.size());
            
            //if link not always visible, only display if size > 0
            if(!alwaysShow) {
                setVisible(model.size() > 0);
            }
        }
        
        public void setNumberLabel(int size) {
            button.setDisplayNumber(size);
        }

        /**
         * Listens for changes on this list. Updates the number of shared files next to the
         * hyperlink button. If the hyperlink is not always shown, will hide/display
         * the hyperlink as the list size changes.
         */
        private class SharingListEventListener implements ListEventListener<LocalFileItem> {
            private boolean alwaysShow;
            
            public SharingListEventListener() {
                this(true);
            }
            
            public SharingListEventListener(boolean alwaysShow) {
                this.alwaysShow = alwaysShow;
            }
            
            @Override
            public void listChanged(ListEvent<LocalFileItem> listChanges) {
                if(listChanges.getSourceList() == model) {                    
                    final int size = listChanges.getSourceList().size();
                    setNumberLabel(size);  
                    // if link not always shown, only show if size > 0
                    if(!alwaysShow) {
                        setVisible(size > 0);
                    }
                }
            }
        }
    }
}