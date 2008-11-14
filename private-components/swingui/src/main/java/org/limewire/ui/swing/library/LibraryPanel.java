package org.limewire.ui.swing.library;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.text.JTextComponent;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.Category;
import org.limewire.core.api.friend.Friend;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * This is the framework for a Library Panel. It contains a Header
 * displaying the category being displayed/filter box. It contains
 * a left hand navigation bar where category buttons can be 
 * added to change the view of the library category on the right/main
 * panel
 */
public abstract class LibraryPanel extends JPanel implements Disposable {
    
    private Map<String, ButtonItem> categoryTables = new HashMap<String, ButtonItem>();
    private List<String> categoryOrder = new ArrayList<String>();
    private List<Disposable> disposableList = new ArrayList<Disposable>();
    
    protected LibraryHeaderPanel headerPanel;
    protected JPanel cardPanel = new JPanel();
    private CardLayout cardLayout = new CardLayout();
    protected JPanel selectionPanel = new JPanel();
    
    protected Friend friend;
    
    private ButtonItem currentItem = null;
    
    private Next next = new Next();
    private Prev prev = new Prev();
    
    public LibraryPanel(Friend friend, boolean isLibraryPanel) {        
        setLayout(new MigLayout("fill, gap 0, insets 0 0 0 0", "[120!][]", "[][]"));
        
        cardPanel.setLayout(cardLayout);
        
        this.friend = friend;
        
        createHeader(friend, isLibraryPanel);
        createSelectionPanel();
        
        add(headerPanel, "span, growx, wrap");
        add(selectionPanel, "growy");
        add(cardPanel, "grow");
    }
    
    public void createHeader(Friend friend, boolean isLibraryPanel) {
        Category category = Category.AUDIO;

        headerPanel = new LibraryHeaderPanel(category, friend, isLibraryPanel);
    }
    
    public abstract void loadHeader();
    
    public void createSelectionPanel() {
        selectionPanel.setLayout(new MigLayout("insets 0, gap 0, fillx, wrap", "[120!]", ""));
    }
    
    public abstract void loadSelectionPanel();
    
    public void select(String id) {
        
        if(currentItem != null)
            currentItem.fireSelected(false);
        
        currentItem = categoryTables.get(id);
        currentItem.fireSelected(true);
        
        cardLayout.show(cardPanel, id);
        headerPanel.setCategory(currentItem.getCategory());
    }
    
    public void selectFirst() {
        if(categoryOrder.size() > 0)
            select(categoryOrder.get(0));
    }
    
    public JTextComponent getFilterTextField() {
        return headerPanel.getFilterTextField();
    }
    
    protected void addDisposable(Disposable disposable) {
        disposableList.add(disposable);
    }
    
    protected JButton createButton(Icon icon, Category category, JComponent component) {
        cardPanel.add(component, category.toString());
        
        ButtonItem item = new ButtonItemImpl(category);
        categoryTables.put(category.toString(), item);
        categoryOrder.add(category.toString());
        
        SelectionButton button = new SelectionButton(new SelectionAction(icon, category, item));
        
        button.getActionMap().put(Next.KEY, next);
        button.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), Next.KEY);
        button.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), Next.KEY);
        
        button.getActionMap().put(Prev.KEY, prev);
        button.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), Prev.KEY);
        button.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), Prev.KEY);
        
        selectionPanel.add(button, "growx");
        
        return button;
    }
    

    @Override
    public void dispose() {
        for(Disposable disposable : disposableList)
            disposable.dispose();
    }
    
    private void selectNext() {
        for(int i = 0; i < categoryOrder.size(); i++) {
            if(categoryOrder.get(i).equals(currentItem.getId())) {
                if(i == categoryOrder.size() -1) {
                    select(categoryOrder.get(0));
                } else
                    select(categoryOrder.get(i+1));
                break;
            }
        }
    }
    
    private void selectPrev() {
        for(int i = 0; i < categoryOrder.size(); i++) {
            if(categoryOrder.get(i).equals(currentItem.getId())) {
                if(i == 0) {
                    select(categoryOrder.get(categoryOrder.size()-1));
                } else
                    select(categoryOrder.get(i-1));
                break;
            }
        }
    }
    
    private class Next extends AbstractAction {
        final static String KEY = "MOVE_DOWN";
        
        @Override
        public void actionPerformed(ActionEvent e) {
            selectNext();
        }
    }
    
    private class Prev extends AbstractAction {
        final static String KEY = "MOVE_UP";
        
        @Override
        public void actionPerformed(ActionEvent e) {
            selectPrev();
        }
    }
    
    protected class SelectionAction extends AbstractAction {
        
        public SelectionAction(Icon icon, Category category, ButtonItem buttonItem) {
            super(category.toString(), icon);
            
            putValue(Action.ACTION_COMMAND_KEY, category.toString());
            
            buttonItem.addButtonItemListener(new ButtonItemListener(){
                @Override
                public void itemSelect(boolean selected) {
                    putValue(SELECTED_KEY, selected);
                }
            });
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            select( (String) getValue(Action.ACTION_COMMAND_KEY));
        }
    }
    
    private interface ButtonItem {
        
        public void addButtonItemListener(ButtonItemListener listener);
        
        public void fireSelected(boolean selected);
        
        public void select();
        
        public Category getCategory();
        
        public String getId();
    }
    
    private interface ButtonItemListener {
        public void itemSelect(boolean selected);
    }
    
    protected class ButtonItemImpl implements ButtonItem {
        
        private final List<ButtonItemListener> listeners = new CopyOnWriteArrayList<ButtonItemListener>();
        
        private Category category;
        
        public ButtonItemImpl(Category category) {
            this.category = category;
        }

        @Override
        public void select() {
            LibraryPanel.this.select(getId());
        }

        @Override
        public String getId() {
            return category.toString();
        }

        @Override
        public Category getCategory() {
            return category;
        }
        
        @Override
        public void addButtonItemListener(ButtonItemListener listener) {
            listeners.add(listener);
        }

        @Override
        public void fireSelected(boolean selected) {
            for(ButtonItemListener listener : listeners)
                listener.itemSelect(selected);
        }
    }
    
    class SelectionButton extends JButton {
        @Resource Color selectedBackground;
        @Resource Font selectedTextFont;
        @Resource Color selectedTextColor;
        @Resource Font textFont;
        @Resource Color textColor;
        
        public SelectionButton(Action action) {
            super(action);

            GuiUtils.assignResources(this);
            
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setHorizontalAlignment(SwingConstants.LEFT);
            
            getAction().addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if(evt.getPropertyName().equals(Action.SELECTED_KEY)) {
                        repaint();
                    }
                }
            });

        }
        
        @Override
        public void paintComponent(Graphics g) {
            if(Boolean.TRUE.equals(getAction().getValue(Action.SELECTED_KEY))) {
                setBackground(selectedBackground);
                setForeground(selectedTextColor);
                setFont(selectedTextFont);
                setOpaque(true);
            } else {
                setOpaque(false);
                setForeground(textColor);
                setFont(textFont);
            }
            
            super.paintComponent(g);
        }
    }    
}
