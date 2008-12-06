package org.limewire.ui.swing.library;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Font;
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
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.text.JTextComponent;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.limewire.core.api.Category;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FileItem;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.LimeHeaderBar;
import org.limewire.ui.swing.components.LimeHeaderBarFactory;
import org.limewire.ui.swing.components.Line;
import org.limewire.ui.swing.components.PromptTextField;
import org.limewire.ui.swing.util.ButtonDecorator;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

/**
 * This is the framework for a Library Panel. It contains a Header
 * displaying the category being displayed/filter box. It contains
 * a left hand navigation bar where category buttons can be 
 * added to change the view of the library category on the right/main
 * panel
 */
public class LibraryPanel extends JPanel implements Disposable {
    
    private final Map<Category, ButtonItem> categoryTables = new HashMap<Category, ButtonItem>();
    private final List<Category> categoryOrder = new ArrayList<Category>();
    private final List<Disposable> disposableList = new ArrayList<Disposable>();
    
    private final JPanel cardPanel = new JPanel();
    private final CardLayout cardLayout = new CardLayout();
    
    private ButtonItem currentItem = null;

    private final JPanel selectionPanel = new LibrarySelectionPanel();
    private final Friend friend;
    private final LimeHeaderBar headerPanel;    
    
    private final boolean isLibraryPanel;
    private final Next next = new Next();
    private final Prev prev = new Prev();

    private final PromptTextField filterField;        
    
    public LibraryPanel(Friend friend, boolean isLibraryPanel, LimeHeaderBarFactory headerBarFactory) {        
        setLayout(new MigLayout("fill, gap 0, insets 0", "[][][grow]", "[][grow]"));

        cardPanel.setLayout(cardLayout);
        
        this.friend = friend;
        this.isLibraryPanel = isLibraryPanel;        
        this.filterField = new PromptTextField(I18n.tr("Search Library..."));
        
		String renderName = friend == null ? "Unknown" : friend.getRenderName();
        if(isLibraryPanel) {
            headerPanel = headerBarFactory.createBasic(I18n.tr("Download from {0}", renderName));
        } else {
            headerPanel = headerBarFactory.createSpecial(I18n.tr("Sharing with {0}", renderName));
        }
        headerPanel.setLayout(new MigLayout("insets 0, gap 0, fill, alignx right"));
        headerPanel.add(filterField, "cell 1 0, gapafter 10");
        
        createSelectionPanel();
        
        add(headerPanel, "span, grow, wrap");
        addMainPanels();
    }
    
    protected Friend getFriend() {
        return friend;
    }
    
    protected void setHeaderTitle(String title) {
        headerPanel.setText(title);
    }
    
    protected void addMainPanels() {
        add(selectionPanel, "growy, width 125!");
        // TODO: move to properties -- funky because this class gets subclassed.
        add(Line.createVerticalLine(Color.decode("#696969")), "growy, width 1!");
        add(cardPanel, "grow");
    }
    
    protected void addShareButton(Action action, ButtonDecorator buttonDecorator) {
        JXButton shareButton = new JXButton(action);
        buttonDecorator.decorateDarkFullButton(shareButton);
        headerPanel.add(shareButton, "cell 0 0, push");
    }
    
    protected void addHeaderComponent(JComponent player, Object constraints) {
        headerPanel.add(player, constraints);
    }
    
    public void createSelectionPanel() {
        selectionPanel.setLayout(new MigLayout("insets 0, gap 0, fillx, wrap", "[125!]", ""));
    }
    
    protected void select(Category id) {        
        if(currentItem != null)
            currentItem.fireSelected(false);
        
        currentItem = categoryTables.get(id);
        currentItem.fireSelected(true);
        
        cardLayout.show(cardPanel, id.name());
    }
    
    protected void selectFirst() {
        select(categoryOrder.get(0));
    }
    
    protected JTextComponent getFilterTextField() {
        return filterField;
    }
    
    protected void addDisposable(Disposable disposable) {
        disposableList.add(disposable);
    }
    
    protected <T extends FileItem> JComponent createButton(Icon icon, Category category, 
            JComponent component, FilterList<T> filteredList, CategorySelectionCallback callback) {
        return createButton(icon, category, component, null, filteredList, callback);
    }
    
    protected <T extends FileItem> JComponent createButton(Icon icon, Category category, 
            JComponent component, FilterList<T> filteredAllFileList, FilterList<T> filteredList,
            CategorySelectionCallback callback) {
        cardPanel.add(component, category.name());
        
        ButtonItem item = new ButtonItemImpl(category);
        categoryTables.put(category, item);
        categoryOrder.add(category);
        
        Action action = new SelectionAction(icon, category, item, callback);
        JComponent button = createSelectionButton(action, category);
        
        // If you only want to show the #s for the sharing panel -- make this false.
        boolean showForAll_NotJustSharing = true;
        if(showForAll_NotJustSharing || !isLibraryPanel) {
            ButtonSizeListener<T> listener = new ButtonSizeListener<T>(category.toString(), action, filteredAllFileList, filteredList, isLibraryPanel);
            filteredList.addListEventListener(listener);
            addDisposable(listener);
        }
        
        selectionPanel.add(button, "growx");
        
        return button;
    }
    
    protected JComponent createSelectionButton(Action action, Category category) {
        return new SelectionPanel(action);
    }
    

    @Override
    public void dispose() {
        for(Disposable disposable : disposableList)
            disposable.dispose();
    }
    
    private void selectNext() {
        for(int i = 0; i < categoryOrder.size(); i++) {
            if(categoryOrder.get(i).equals(currentItem.getCategory())) {
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
            if(categoryOrder.get(i).equals(currentItem.getCategory())) {
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
    
    private class SelectionAction extends AbstractAction {
        
        public SelectionAction(Icon icon, Category category, ButtonItem buttonItem, final CategorySelectionCallback callback) {
            super(I18n.tr(category.toString()), icon);
            
            putValue("limewire.category", category);
            
            buttonItem.addButtonItemListener(new ButtonItemListener(){
                @Override
                public void itemSelect(boolean selected) {
                    putValue(SELECTED_KEY, selected);
                    
                    if (callback != null) {
                        callback.call((Category)getValue("limewire.category"), selected);
                    }
                }
            });
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            select((Category)getValue("limewire.category"));
        }
    }
    
    /**
     * Allows a category creator to define a method to notify back any selection
     *  events on this category 
     */
    protected interface CategorySelectionCallback {
        public void call(Category category, boolean state);
    }
    
    private interface ButtonItem {
        
        public void addButtonItemListener(ButtonItemListener listener);
        
        public void fireSelected(boolean selected);
        
        public void select();
        
        public Category getCategory();
    }
    
    private interface ButtonItemListener {
        public void itemSelect(boolean selected);
    }
    
    private class ButtonItemImpl implements ButtonItem {
        
        private final List<ButtonItemListener> listeners = new CopyOnWriteArrayList<ButtonItemListener>();
        
        private Category category;
        
        public ButtonItemImpl(Category category) {
            this.category = category;
        }

        @Override
        public void select() {
            LibraryPanel.this.select(getCategory());
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
    
    private class ButtonSizeListener<T> implements Disposable, ListEventListener<T> {
        private final String text;
        private final Action action;
        private final FilterList<T> allFileList;
        private final FilterList<T> list;
        private final boolean isLibraryPanel;
        
        private ButtonSizeListener(String text, Action action, FilterList<T> allFileList, FilterList<T> list, boolean isLibraryPanel) {
            this.text = text;
            this.action = action;
            this.allFileList = allFileList;
            this.list = list;            
            this.isLibraryPanel = isLibraryPanel;
            setText();
        }

        private void setText() {
            if(isLibraryPanel)
                action.putValue(Action.NAME, I18n.tr(text) + " (" + list.size() + ")");
            else {
                action.putValue(Action.NAME, I18n.tr(text) + " (" + list.size() + "/" + allFileList.size() + ")");
            }
        }
        
        @Override
        public void dispose() {
            list.removeListEventListener(this);
            list.dispose();
        }

        @Override
        public void listChanged(ListEvent<T> listChanges) {
            setText();
        }
    }
    
    protected void addNavigation(JComponent component) {
        component.getActionMap().put(Next.KEY, next);
        component.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), Next.KEY);
        component.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), Next.KEY);
        
        component.getActionMap().put(Prev.KEY, prev);
        component.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), Prev.KEY);
        component.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), Prev.KEY);
    }
    
    private class SelectionPanel extends JPanel {
        @Resource Color selectedBackground;
        @Resource Font selectedTextFont;
        @Resource Color selectedTextColor;
        @Resource Font textFont;
        @Resource Color textColor;
        
        private JButton button;
        
        public SelectionPanel(Action action) {
            super(new BorderLayout());

            GuiUtils.assignResources(this);
            
            button = new JButton(action);
            
            add(button, BorderLayout.CENTER);
            
            button.setContentAreaFilled(false);
            button.setBorderPainted(false);
            button.setFocusPainted(false);
            button.setBorder(BorderFactory.createEmptyBorder(2,8,2,8));
            button.setHorizontalAlignment(SwingConstants.LEFT);
            
            setOpaque(false);

            button.getAction().addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if(evt.getPropertyName().equals(Action.SELECTED_KEY)) {
                        if(Boolean.TRUE.equals(evt.getNewValue())) {
                            setOpaque(true);
                            setBackground(selectedBackground);
                            button.setForeground(selectedTextColor);
                        } else {
                            setOpaque(false);
                            button.setForeground(textColor);
                        }
                        repaint();
                    }
                }
            });
            
            addNavigation(button);
        }        
        
        public JButton getButton() {
            return button;
        }
    }    
}
