package org.limewire.ui.swing.sharing;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.components.Circle;
import org.limewire.ui.swing.components.NumberedHyperLinkButton;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

public class SharingShortcutPanel extends JPanel {

    private String[] names;
    private List<EventList<LocalFileItem>> models;
    
    private List<ButtonCircleComponent> buttons;
     
    public SharingShortcutPanel(String[] names, Action[] actions, List<EventList<LocalFileItem>> models, boolean[] alwaysShown) {
        this.names = names;
        this.models = models;

        buttons = new ArrayList<ButtonCircleComponent>();
        
        setLayout(new MigLayout("hidemode 3, gapx 0, ax 50%","",""));
        
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
            add(buttonCircle, "gapx 0");
            
        }
    }
    
    public void setModel(List<EventList<LocalFileItem>> model) {
        for(int i = 0; i < model.size(); i++) {
            buttons.get(i).setModel(model.get(i));
        }
        this.models = model;
    }
    
    private class SharingListEventListener implements ListEventListener<LocalFileItem> {

        private ButtonCircleComponent button;
        private boolean alwaysShow;
        
        public SharingListEventListener(ButtonCircleComponent button) {
            this(button, true);
        }
        
        public SharingListEventListener(ButtonCircleComponent button, boolean alwaysShow) {
            this.button = button;
            this.alwaysShow = alwaysShow;
        }
        
        @Override
        public void listChanged(ListEvent<LocalFileItem> listChanges) {
            final int size = listChanges.getSourceList().size();
            SwingUtilities.invokeLater(new Runnable(){
                public void run() {
                    button.setNumberLabel(size);  
                    if(!alwaysShow) {
                        if(size == 0)// && button.isVisible())
                            button.setVisible(false);
                        else// if(!button.isVisible())
                            button.setVisible(true);
                    }
                }
            });
        }
    }
//    
//    private class LinkSettingListener implements SettingListener {
//
//        private JComponent component;
//        private BooleanSetting setting;
//        
//        public LinkSettingListener(JComponent component, BooleanSetting setting) {
//            this.component = component;
//            this.setting = setting;
//        }
//        
//        @Override
//        public void settingChanged(SettingEvent evt) {
//            this.component.setVisible(setting.getValue());
//        }
//    }
    
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
            
            setLayout(new MigLayout());
            
            if(loc == CircleLocation.LEFT) {
                add(new Circle(8), "gapafter 8");
                add(button);
            } else if(loc == CircleLocation.RIGHT) {
                add(button);
                add(new Circle(8), "gapbefore 8");
            } else
                add(button);
            setBorder(null);
            setVisible(alwaysShown);
        }
        
        private void createButton(String name, Action action, boolean alwaysShown) {
            button = new NumberedHyperLinkButton(name, action);
            button.setDisplayNumber(0);
            listener = new SharingListEventListener(this, alwaysShown);
            model.addListEventListener(listener);
        }
        
//        @Override
//        public void setVisible(boolean value) { 
//            super.setVisible(value);
//        }
        
        public void setModel(EventList<LocalFileItem> newModel) {
            if(model != null)
                model.removeListEventListener(listener);
            this.model = newModel;
            model.addListEventListener(listener);
            button.setDisplayNumber(model.size());
        }
        
        public void setNumberLabel(int size) {
            button.setDisplayNumber(size);
        }
    }
}