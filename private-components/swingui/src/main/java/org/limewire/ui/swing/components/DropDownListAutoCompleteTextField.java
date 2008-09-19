/**
 * Code taken freely from
 * http://www.java-engineer.com/java/auto-complete.html
 */
 
//------------------------------------------------------------------------------
// Copyright (c) 1999-2001 Matt Welsh.  All Rights Reserved.
//------------------------------------------------------------------------------
package org.limewire.ui.swing.components;

import java.awt.event.KeyEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.awt.AWTEvent;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Point;
import java.awt.Component;
import java.awt.Dimension;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.text.Document;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.ListModel;
import javax.swing.PopupFactory;
import javax.swing.Popup;
import javax.swing.JList;
import javax.swing.ListSelectionModel;

import org.limewire.collection.AutoCompleteDictionary;
import org.limewire.collection.StringTrieSet;
import org.limewire.core.settings.UISettings;
import org.limewire.util.OSUtils;


/**
 *
 * @author Matt Welsh (matt@matt-welsh.com)
 *
 * @modified Sam Berlin
 *      .. to not implement AutoComplete, not take
 *      the dictionary in the constructor, allow the Dictionary
 *      and listeners to be lazily created, and update the dictionary at will.
 * 
 */
public class DropDownListAutoCompleteTextField extends KeyProcessingTextField {
    
    protected AutoCompleteDictionary dict;

    public DropDownListAutoCompleteTextField() { super(); init(); }
    public DropDownListAutoCompleteTextField(Document a, String b, int c) {super(a, b, c); init();}
    public DropDownListAutoCompleteTextField(int a) { super(a); init(); }
    public DropDownListAutoCompleteTextField(String a) { super(a); init(); }
    public DropDownListAutoCompleteTextField(String a, int b) { super(a, b); init(); }
    
    //----------------------------------------------------------------------------
    // Public methods
    //----------------------------------------------------------------------------
    
    /**
    * Set the dictionary that autocomplete lookup should be performed by.
    *
    * @param dict The dictionary that will be used for the autocomplete lookups.
    */
    public void setDictionary(AutoCompleteDictionary dict) {
        this.dict = dict;
    }

    /**
    * Gets the dictionary currently used for lookups.
    *
    * @return dict The dictionary that will be used for the autocomplete lookups.
    */
    public AutoCompleteDictionary getDictionary() {
        return dict;
    }

    /**
    * Creates the default dictionary object
    */
    public AutoCompleteDictionary createDefaultDictionary() {
        return new StringTrieSet(true);
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

        if ( dict == null ) {
            this.dict = createDefaultDictionary();
        }
        dict.addEntry(getText().trim());
    }
    
    /**
     * Adds the specified string to the underlying dictionary
     */
    public void addToDictionary(String s) {
        if( !getAutoComplete() ) return;

        if ( dict == null ) {
            this.dict = createDefaultDictionary();
        }
        dict.addEntry(s.trim());
    }
    
    /**
     * Displays the popup window with a list of auto-completable choices,
     * if any exist.
     */
    public void autoCompleteInput() {
        String input = getText();
        if (input != null && input.length() > 0) {
            Iterator<String> it = dict.iterator(input);
            if (it.hasNext())
                showPopup(it);
            else
                hidePopup();
        } else {
            hidePopup();
        }
    }
    
    protected String lookup(String s) {
        if(dict != null && getAutoComplete() && !s.equals(""))
            return dict.lookup(s);
        return null;
    }
    
    /**
     * Sets up stuff.
     */
    private void init() {
        enableEvents(AWTEvent.KEY_EVENT_MASK);
        enableEvents(AWTEvent.HIERARCHY_EVENT_MASK);
        enableEvents(AWTEvent.FOCUS_EVENT_MASK);
    }
    
    /**
     * Fires an action event.
     *
     * If the popup is visible, this resets the current
     * text to be the selection on the popup (if something was selected)
     * prior to firing the event.
     */
    @Override
    protected void fireActionPerformed() {
        if(popup != null) {
            String selection = (String)entryList.getSelectedValue();
            hidePopup();
            if(selection != null) {
                setText(selection);
                return;
            }
        }
        
        super.fireActionPerformed();
    }
    
    /**
     * Forwards necessary events to the AutoCompleteList.
     */
    @Override
    public void processKeyEvent(KeyEvent evt) {
        if(evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN)
            evt.consume();
        
        super.processKeyEvent(evt);
        
        if(dict != null) {
            switch(evt.getID()) {
            case KeyEvent.KEY_PRESSED:
                switch(evt.getKeyCode()) {
                case KeyEvent.VK_UP:
                    if(popup != null)
                        entryList.decrementSelection();
                    else
                        showPopup(dict.iterator());
                    break;
                case KeyEvent.VK_DOWN:
                    if(popup != null)
                        entryList.incrementSelection();
                    else
                        showPopup(dict.iterator());                        
                    break;
                }
                break;
            case KeyEvent.KEY_TYPED:
                switch(evt.getKeyChar()) {
                case KeyEvent.VK_ESCAPE:
                    if (popup != null) {
                        hidePopup();
                        selectAll();
                    }
                    break;
                case KeyEvent.VK_ENTER:
                    break;
                default:
                    autoCompleteInput();
                }
            }
        }
    }
    
    /**
     * Ensures the popup gets hidden if this text-box is hidden and that
     * the popup is shown if a previous show is pending (from trying to
     * autocomplete while it wasn't visible).
     */
    @Override
    protected void processHierarchyEvent(HierarchyEvent evt) {
        super.processHierarchyEvent(evt);
        
        if((evt.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == HierarchyEvent.SHOWING_CHANGED) {
            boolean showing = isShowing();
            if(!showing && popup != null)
                hidePopup();
            else if(showing && popup == null && showPending)
                autoCompleteInput();
        }
    }
    
    /**
     * Ensures that if we lose focus, the popup goes away.
     */
    @Override
    protected void processFocusEvent(FocusEvent evt) {
        super.processFocusEvent(evt);
        
        if(evt.getID() == FocusEvent.FOCUS_LOST) {
            if(popup != null)
                hidePopup();
        }
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
        for(int i = 0; iter.hasNext(); i++) {
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
        
        entryList.setCurrentText(getText());
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
            if(isShowing()) {
                Point origin = getLocationOnScreen();
                PopupFactory pf = PopupFactory.getSharedInstance();
				Component parent = this;
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
					new MyPopup(this, parent, 0, 0);
				}
                popup = pf.getPopup(parent, getPopupComponent(), origin.x, origin.y + getHeight() + 1);
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

    //----------------------------------------------------------------------------
    // Fields
    //----------------------------------------------------------------------------
    /** The list auto-completable items are shown in */
    protected AutoCompleteList entryList;
    /** The panel the popup is shown in. */
    protected JPanel entryPanel;
    /** The popup the scroll pane is in */
    protected Popup popup;
    /** Whether or not we tried to show a popup while this wasn't showing */
    protected boolean showPending;
    
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
                    DropDownListAutoCompleteTextField.this.setText(selection);
                    DropDownListAutoCompleteTextField.this.hidePopup();
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
                DropDownListAutoCompleteTextField.this.setText(currentText);
                clearSelection();
            } else {
                int selectedIndex = getSelectedIndex() + 1;
                setSelectedIndex(selectedIndex);
                ensureIndexIsVisible(selectedIndex);
                DropDownListAutoCompleteTextField.this.setText((String)getSelectedValue());
            }
        }
        
        /**
         * Decrements the selection by one.
         */
        void decrementSelection() {
            if(getSelectedIndex() == 0) {
                DropDownListAutoCompleteTextField.this.setText(currentText);
                clearSelection();
            } else {
                int selectedIndex = getSelectedIndex();
                if(selectedIndex == -1)
                    selectedIndex = getModel().getSize() - 1;
                else
                    selectedIndex--;
                setSelectedIndex(selectedIndex);
                ensureIndexIsVisible(selectedIndex);
                DropDownListAutoCompleteTextField.this.setText((String)getSelectedValue());
            }
        }
        
        /**
         * Sets the size according to the number of entries.
         */
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            int width = DropDownListAutoCompleteTextField.this.getSize().width - 2;
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

