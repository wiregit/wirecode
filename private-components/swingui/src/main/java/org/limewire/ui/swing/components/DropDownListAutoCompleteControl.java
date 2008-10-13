package org.limewire.ui.swing.components;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.limewire.collection.AutoCompleteDictionary;
import org.limewire.collection.StringTrieSet;
import org.limewire.core.settings.SearchSettings;
import org.limewire.core.settings.UISettings;
import org.limewire.util.OSUtils;


/**
 *A DropDown list of autocompletable items for a JTextField.
 * 
 */
public class DropDownListAutoCompleteControl {
    
    private static final String PROPERTY = "limewire.text.autocompleteControl"; 

    /** The dictionary this uses. */
    protected AtomicReference<AutoCompleteDictionary> dict = new AtomicReference<AutoCompleteDictionary>();

    /** The text field this is working on. */
    private final JTextField textField;

    /** The list auto-completable items are shown in */
    protected AutoCompleteList entryList;

    /** The panel the popup is shown in. */
    protected JPanel entryPanel;

    /** The popup the scroll pane is in */
    protected Popup popup;

    /** Whether or not we tried to show a popup while this wasn't showing */
    protected boolean showPending;
    
    /** Installs a dropdown list for autocompletion on the given text field. */
    public static DropDownListAutoCompleteControl install(JTextField textField) {
        DropDownListAutoCompleteControl control = new DropDownListAutoCompleteControl(textField);
        textField.putClientProperty(PROPERTY, control);
        Listener listener = control.new Listener();
        textField.addKeyListener(listener);
        textField.addHierarchyListener(listener);
        textField.addFocusListener(listener);
        textField.addActionListener(listener);
        return control;
    }

    /**
     * Installs a dropdown list using the given autocomplete dictionary on the
     * text field.
     */
    public static DropDownListAutoCompleteControl install(JTextField textField,
            AutoCompleteDictionary autocompleteDictionary) {
        DropDownListAutoCompleteControl control = install(textField);
        control.setDictionary(autocompleteDictionary);
        return control;
    }
    
    
    /** Returns the control for the text field. */
    public static DropDownListAutoCompleteControl getDropDownListAutoCompleteControl(JTextField textField) {
        return (DropDownListAutoCompleteControl)textField.getClientProperty(PROPERTY);
    }
    
    private DropDownListAutoCompleteControl(JTextField textField) {
        this.textField = textField;
    }
    
    /**
    * Set the dictionary that autocomplete lookup should be performed by.
    *
    * @param dict The dictionary that will be used for the autocomplete lookups.
    */
    public void setDictionary(AutoCompleteDictionary dict) {
        this.dict.set(dict);
    }

    /**
    * Gets the dictionary currently used for lookups.
    *
    * @return dict The dictionary that will be used for the autocomplete lookups.
    */
    public AutoCompleteDictionary getDictionary() {
        return dict.get();
    }
    
    /**
    * Sets whether the component is currently performing autocomplete lookups as
    * keystrokes are performed.
    *
    * @param val True or false.
    */
    public void setAutoComplete(boolean val) {
        UISettings.AUTOCOMPLETE_ENABLED.setValue(val);
    }
    
    /**
    * Gets whether the component is currently performing autocomplete lookups as
    * keystrokes are performed. Looks up the value in UISettings.
    *
    * @return True or false.
    */
    public boolean getAutoComplete() {
        return UISettings.AUTOCOMPLETE_ENABLED.getValue();
    }

    /**
    * Adds the current value of the field underlying dictionary
    */
    public void addToDictionary() {
        if( !getAutoComplete() ) return;

        if ( dict.get() == null ) {
            this.dict.set(new StringTrieSet(true));
        }
        dict.get().addEntry(textField.getText().trim());
    }
    
    /**
     * Adds the specified string to the underlying dictionary
     */
    public void addToDictionary(String s) {
        if( !getAutoComplete() ) return;

        if ( dict.get() == null ) {
            this.dict.set(new StringTrieSet(true));
        }
        dict.get().addEntry(s.trim());
    }
    
    /**
     * Displays the popup window with a list of auto-completable choices,
     * if any exist.
     */
    public void autoCompleteInput() {
        // Shove this into an invokeLater to force us seeing the proper text.
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                String input = textField.getText();
                if (input != null && input.length() > 0) {
                    Iterator<String> it = dict.get().iterator(input);
                    if (it.hasNext())
                        showPopup(it);
                    else
                        hidePopup();
                } else {
                    hidePopup();
                }
            }
        });
    }
    
    protected String lookup(String s) {
        if(dict.get() != null && getAutoComplete() && !s.equals(""))
            return dict.get().lookup(s);
        return null;
    }
    
    /**
     * Gets the component that is the popup listing other choices.
     */
    protected JComponent getPopupComponent() {
        if(entryPanel != null)
            return entryPanel;
            
        entryPanel = new JPanel(new GridBagLayout());
        entryPanel.setBorder(UIManager.getBorder("List.border"));
        entryPanel.setBackground(UIManager.getColor("List.background"));
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        
        entryList = new AutoCompleteList();
        JScrollPane entryScrollPane = new JScrollPane(entryList);
        entryScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        entryScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        entryPanel.add(entryScrollPane, c);
        
        //entryPanel.add(new ClearHistory(), c);
                                          
        return entryPanel;
    }

    /**
     * Fills the popup with text & shows it.
     */
    protected void showPopup(Iterator<String> iter) {
        getPopupComponent(); // construct the component.
        
        boolean different = false;
        Vector<String> v = new Vector<String>();
        ListModel model = entryList.getModel();
        for(int i = 0;
            iter.hasNext() && i < SearchSettings.POPULATE_SEARCH_BAR_NUMBER_FRIEND_FILES.getValue();
            i++) {
            String next = iter.next();
            v.add(next);
            
            if(!different && i < model.getSize())
                different |= !next.equals(model.getElementAt(i));
        }
        
        different |= model.getSize() != v.size();
    
        // if things were different, reset the data.
        if(different) {
            entryList.setListData(v);
            entryList.clearSelection();
        }
        
        entryList.setCurrentText(textField.getText());
        showPopup();
    }

    /**
     * Shows the popup.
     */
    public void showPopup() {
        // only show the popup if we're currently visible.
        // due to delay in focus-forwarding & key-pressing events,
        // we may not be visible by the time this is called.
        if(popup == null && entryList.getModel().getSize() > 0) {
            if(textField.isShowing()) {
                Point origin = textField.getLocationOnScreen();
                PopupFactory pf = PopupFactory.getSharedInstance();
				Component parent = textField;
				// OSX doesn't handle MOUSE_CLICKED events correctly
				// using medium-weight popups, so we need to force
				// PopupFactory to return a heavy-weight popup.
				// This is done by adding a panel into a Popup, which implicitly
				// adds it into a Popup.HeavyWeightWindow, which PopupFactory happens
				// to check as a condition for returning a heavy-weight popup.
				// In an ideal world, the OSX bug would be fixed.
				// In a less ideal world, Popup & PopupFactory would be written so that
				// outside developers can correctly subclass methods.
				if(OSUtils.isMacOSX()) {
					parent = new JPanel();
					new MyPopup(textField, parent, 0, 0);
				}
                popup = pf.getPopup(parent, getPopupComponent(), origin.x, origin.y + textField.getHeight() + 1);
                showPending = false;
                popup.show();
            } else {
                showPending = true;
            }
        }
    }

    /**
     * Hides the popup window.
     */
    public void hidePopup() {
        showPending = false;
        if(popup != null) {
            popup.hide();
            popup = null;
        }
    }
    
    private class Listener implements ActionListener, KeyListener, HierarchyListener, FocusListener {
        /**
         * Fires an action event.
         *
         * If the popup is visible, this resets the current
         * text to be the selection on the popup (if something was selected)
         * prior to firing the event.
         */
        // TODO: This used to return w/o firing the event sometimes.
        @Override
        public void actionPerformed(ActionEvent e) {
            if(popup != null) {
                String selection = (String)entryList.getSelectedValue();
                hidePopup();
                if(selection != null) {
                    textField.setText(selection);
                    // TODO: prevent event from firing?
                }
            }
        }        
        
        /** Forwards necessary events to the AutoCompleteList. */
        @Override
        public void keyPressed(KeyEvent evt) {
            if(evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN) {
                evt.consume();    
            }
            
            if(dict.get() != null) {
                switch(evt.getKeyCode()) {
                case KeyEvent.VK_UP:
                    if(popup != null)
                        entryList.decrementSelection();
                    else
                        showPopup(dict.get().iterator());
                    break;
                case KeyEvent.VK_DOWN:
                    if(popup != null)
                        entryList.incrementSelection();
                    else
                        showPopup(dict.get().iterator());                        
                    break;
                }
            }
        }
        
        /** Forwards necessary events to the AutoCompleteList. */
        @Override
        public void keyReleased(KeyEvent evt) {
            if(evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN)
                evt.consume();       
        }
        
        /** Forwards necessary events to the AutoCompleteList. */
        @Override
        public void keyTyped(KeyEvent evt) {
            if(evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN)
                evt.consume();
            
            if(dict.get() != null) {
                switch(evt.getKeyChar()) {
                case KeyEvent.VK_ESCAPE:
                    if (popup != null) {
                        hidePopup();
                        textField.selectAll();
                    }
                    break;
                case KeyEvent.VK_ENTER:
                    break;
                default:
                    autoCompleteInput();
                }
            }
        }
        
        @Override
        public void hierarchyChanged(HierarchyEvent evt) {            
            if((evt.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == HierarchyEvent.SHOWING_CHANGED) {
                boolean showing = textField.isShowing();
                if(!showing && popup != null)
                    hidePopup();
                else if(showing && popup == null && showPending)
                    autoCompleteInput();
            }
        }
        
        @Override
        public void focusGained(FocusEvent e) {
        }
        
        @Override
        public void focusLost(FocusEvent evt) {
            if(evt.getID() == FocusEvent.FOCUS_LOST) {
                if(popup != null)
                    hidePopup();
            }
        }
    }
    
 
    
    
    /**
     * A list that's used to show auto-complete items.
     */
    private class AutoCompleteList extends JList {
        private String currentText;
        
        AutoCompleteList() {
            super();
            enableEvents(AWTEvent.MOUSE_EVENT_MASK);
            setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            setFocusable(false);
        }
        
        /**
         * Sets the text field's selection with the clicked item.
         */
        @Override
        protected void processMouseEvent(MouseEvent me) {
			super.processMouseEvent(me);
            
            if(me.getID() == MouseEvent.MOUSE_CLICKED) {
                int idx = locationToIndex(me.getPoint());
                if(idx != -1 && isSelectedIndex(idx)) {
                    String selection = (String)getSelectedValue();
                    textField.setText(selection);
                    hidePopup();
                }
            }
        }
        
        /**
         * Sets the text to place in the text field when items are unselected.
         */
        void setCurrentText(String text) {
            currentText = text;
        }
        
        /**
         * Increments the selection by one.
         */
        void incrementSelection() {
            if(getSelectedIndex() == getModel().getSize() - 1) {
                textField.setText(currentText);
                clearSelection();
            } else {
                int selectedIndex = getSelectedIndex() + 1;
                setSelectedIndex(selectedIndex);
                ensureIndexIsVisible(selectedIndex);
                textField.setText((String)getSelectedValue());
            }
        }
        
        /**
         * Decrements the selection by one.
         */
        void decrementSelection() {
            if(getSelectedIndex() == 0) {
                textField.setText(currentText);
                clearSelection();
            } else {
                int selectedIndex = getSelectedIndex();
                if(selectedIndex == -1)
                    selectedIndex = getModel().getSize() - 1;
                else
                    selectedIndex--;
                setSelectedIndex(selectedIndex);
                ensureIndexIsVisible(selectedIndex);
                textField.setText((String)getSelectedValue());
            }
        }
        
        /**
         * Sets the size according to the number of entries.
         */
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            int width = textField.getSize().width - 2;
            int rows = Math.min(getModel().getSize(), 8);
            int height = rows * getCellBounds(0, 0).height;
            return new Dimension(width, height);
        }
    }
	
	/**
	 * Subclass that provides access to the constructor.
	 */
	private static class MyPopup extends Popup {
		public MyPopup(Component owner, Component contents, int x, int y) {
			super(owner, contents, x, y);
		}
	}
}

