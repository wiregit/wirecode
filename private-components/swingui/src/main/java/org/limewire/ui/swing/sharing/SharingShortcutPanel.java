package org.limewire.ui.swing.sharing;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.components.Circle;
import org.limewire.ui.swing.components.NumberedHyperLinkButton;
import org.limewire.ui.swing.util.GuiUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

public class SharingShortcutPanel extends JPanel {

    @Resource
    private Color circleColor;
    @Resource
    private int circleSize;
    
    private String[] names;
    private List<EventList<LocalFileItem>> models;
    
    private List<ButtonCircleComponent> buttons;
     
    public SharingShortcutPanel(String[] names, Action[] actions, List<EventList<LocalFileItem>> models, boolean[] alwaysShown) {
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
    
    public void setModel(List<EventList<LocalFileItem>> model) {
        for(int i = 0; i < model.size(); i++) {
            buttons.get(i).setModel(model.get(i));
        }
        this.models = model;
    }
    
    public static enum CircleLocation {
        LEFT, RIGHT, NONE;
    }
    
    private class ButtonCircleComponent extends JPanel {
        
        private NumberedHyperLinkButton button;
        private EventList<LocalFileItem> model;
        private SharingListEventListener listener;
        
        public ButtonCircleComponent(String name, Action action, EventList<LocalFileItem> model, CircleLocation loc, boolean alwaysShown) {
            
            this.model = model;
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
        
        public void setModel(EventList<LocalFileItem> newModel) {
            if(model != null) {
                final EventList<LocalFileItem> toRemove = model;
                SwingUtilities.invokeLater(new Runnable() {
                     @Override
                    public void run() {
                         toRemove.removeListEventListener(listener);
                    }
                });
            }
            this.model = newModel;
            model.addListEventListener(listener);
            button.setDisplayNumber(model.size());
        }
        
        public void setNumberLabel(int size) {
            button.setDisplayNumber(size);
        }
        

        
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
                    if(!alwaysShow) {
                        if(size == 0)// && button.isVisible())
                            button.setVisible(false);
                        else// if(!button.isVisible())
                            button.setVisible(true);
                    }
                }
            }
        }
    }
}