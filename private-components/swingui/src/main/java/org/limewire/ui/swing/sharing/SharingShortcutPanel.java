package org.limewire.ui.swing.sharing;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.components.Circle;
import org.limewire.ui.swing.components.NumberedHyperLinkButton;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

public class SharingShortcutPanel extends JPanel {

    private String[] names;
    private List<EventList<LocalFileItem>> models;
    private List<SharingListEventListener> listeners;
    private List<NumberedHyperLinkButton> buttons;
     
    public SharingShortcutPanel(String[] names, Action[] actions, List<EventList<LocalFileItem>> models) {
        this.names = names;
        this.models = models;
        
        listeners = new ArrayList<SharingListEventListener>();
        buttons = new ArrayList<NumberedHyperLinkButton>();
        
        createComponents(actions);
    }
    
    private void createComponents(Action[] actions) {
        for(int i = 0; i < names.length; i++) {
            final NumberedHyperLinkButton button = new NumberedHyperLinkButton(names[i], actions[i]);
            button.setForegroundColor(Color.BLUE);
            button.setMouseOverColor(Color.GREEN);
            button.setDisabledColor(Color.DARK_GRAY);
            button.setDisplayNumber(0);
            SharingListEventListener listener = new SharingListEventListener(button);
            models.get(i).addListEventListener(listener);
            listeners.add(listener);
            buttons.add(button);
            add(button);
            if(i + 1 < names.length)
                add(new Circle(8));
        }
    }
    
    public void setModel(List<EventList<LocalFileItem>> model) {
        for(int i = 0; i < listeners.size(); i++) {
            models.get(i).removeListEventListener(listeners.get(i));
            model.get(i).addListEventListener(listeners.get(i));
            buttons.get(i).setDisplayNumber(model.get(i).size());
        }
        this.models = model;
    }
    
    private class SharingListEventListener implements ListEventListener<LocalFileItem> {

        private NumberedHyperLinkButton button;
        
        public SharingListEventListener(NumberedHyperLinkButton button) {
            this.button = button;
        }
        
        @Override
        public void listChanged(ListEvent<LocalFileItem> listChanges) {
            final int size = listChanges.getSourceList().size();
            SwingUtilities.invokeLater(new Runnable(){
                public void run() {
                    button.setDisplayNumber(size);                                
                }
            });
        }
    }
}