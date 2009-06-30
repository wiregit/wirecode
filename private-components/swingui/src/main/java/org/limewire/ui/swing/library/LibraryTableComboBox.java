package org.limewire.ui.swing.library;

import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.LimeComboBox;
import org.limewire.ui.swing.components.PromptTextField;
import org.limewire.ui.swing.components.TextFieldClipboardControl;
import org.limewire.ui.swing.components.decorators.TextFieldDecorator;
import org.limewire.ui.swing.library.table.AbstractLibraryFormat;
import org.limewire.ui.swing.library.table.AllTableFormat;
import org.limewire.ui.swing.library.table.AudioTableFormat;
import org.limewire.ui.swing.library.table.DocumentTableFormat;
import org.limewire.ui.swing.library.table.ImageTableFormat;
import org.limewire.ui.swing.library.table.OtherTableFormat;
import org.limewire.ui.swing.library.table.ProgramTableFormat;
import org.limewire.ui.swing.library.table.VideoTableFormat;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Allows the user to filter by category and filter 
 * with a filter textfield.
 */
class LibraryTableComboBox extends LimeComboBox {
    
    private final CategoryIconManager categoryIconManager;

    /** Is added to the button to display custom icon/text */
    private final JLabel comboBoxDisplayLabel;
    private final PromptTextField promptTextField;
    private final JPopupMenu popupMenu;
    private final ButtonGroup buttonGroup = new ButtonGroup();
    private final JMenuItem allMenuItem;
    
    /** Action that is currently selected in the menu */
    private ComboBoxAction selectedAction;
    
    @Inject
    public LibraryTableComboBox(Provider<AllTableFormat<LocalFileItem>> allFormat, 
            Provider<AudioTableFormat<LocalFileItem>> audioFormat,
            Provider<VideoTableFormat<LocalFileItem>> videoFormat,
            Provider<ImageTableFormat<LocalFileItem>> imageFormat,
            Provider<DocumentTableFormat<LocalFileItem>> documentFormat,
            Provider<ProgramTableFormat<LocalFileItem>> programFormat,
            Provider<OtherTableFormat<LocalFileItem>> otherFormat,
            CategoryIconManager categoryIconManager,
            TextFieldDecorator textFieldDecorator) {
        
        this.categoryIconManager = categoryIconManager;
        
        popupMenu = new JPopupMenu();
        overrideMenu(popupMenu);
        
        comboBoxDisplayLabel = new JLabel();
        add(comboBoxDisplayLabel);
        
        promptTextField = new PromptTextField(I18n.tr("Filter"));
        TextFieldClipboardControl.install(promptTextField);
        textFieldDecorator.decorateClearablePromptField(promptTextField, AccentType.NONE);
        promptTextField.setBorder(BorderFactory.createEmptyBorder(0,10,0,10));

        allMenuItem = createRadioMenuItem(new ComboBoxAction(I18n.tr("All"), null,  allFormat));
        allMenuItem.setSelected(true);
        popupMenu.add(allMenuItem);
        popupMenu.add(createRadioMenuItem(new ComboBoxAction(I18n.tr("Audio"), Category.AUDIO,audioFormat)));
        popupMenu.add(createRadioMenuItem(new ComboBoxAction(I18n.tr("Video"), Category.VIDEO, videoFormat)));
        popupMenu.add(createRadioMenuItem(new ComboBoxAction(I18n.tr("Image"), Category.IMAGE, imageFormat)));
        popupMenu.add(createRadioMenuItem(new ComboBoxAction(I18n.tr("Document"), Category.DOCUMENT, documentFormat)));
        popupMenu.add(createRadioMenuItem(new ComboBoxAction(I18n.tr("Programs"), Category.PROGRAM, programFormat)));
        popupMenu.add(createRadioMenuItem(new ComboBoxAction(I18n.tr("Other"), Category.OTHER, otherFormat)));
        popupMenu.add(promptTextField);

        setSelectedAction((ComboBoxAction)allMenuItem.getAction());
    }
    
    @Inject
    public void register() {       
        popupMenu.addPopupMenuListener(new PopupMenuListener(){

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                // user may have made changes to the text but not used any enter mechanism
                setDisplayText();
            }

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                SwingUtilities.invokeLater(new Runnable(){
                    public void run() {
                        promptTextField.grabFocus();                        
                    }
                });
            }
            
        });
        promptTextField.addKeyListener(new KeyAdapter(){
            @Override
            public void keyTyped(KeyEvent e) {
                if(e.getKeyChar() == KeyEvent.VK_ENTER) {
                    popupMenu.setVisible(false);
                }
            }      
        });
    }
    
    public JTextField getFilterField() {
        return promptTextField;
    }
    
    public AbstractLibraryFormat<LocalFileItem> getSelectedTableFormat() {
        return selectedAction.getTableFormat();
    }
    
    public Category getSelectedCategory() {
        return selectedAction.getCategory();
    }
    
    private void setSelectedAction(ComboBoxAction action) {
        selectedAction = action;
        setDisplayText();
        fireChangeEvent(action);
    }
    
    private void setDisplayText() {
        String text = getFilterField().getText();
        if(text != null && text.trim().length() > 0) 
            comboBoxDisplayLabel.setText(getFilterField().getText().trim());
        else
            comboBoxDisplayLabel.setText(selectedAction.getText());
        comboBoxDisplayLabel.setIcon(selectedAction.getIcon());
    }
    
    private JMenuItem createRadioMenuItem(Action action) {
        JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(action);
        buttonGroup.add(menuItem);
        return menuItem;
    }
    
    /**
     * An action for a menuItem to choose which category to show.
     */
    private class ComboBoxAction extends AbstractAction {

        private final Provider<? extends AbstractLibraryFormat<LocalFileItem>> tableFormat;
        private final Category category;
        
        public ComboBoxAction(String displayName, Category category,
                Provider<? extends AbstractLibraryFormat<LocalFileItem>> tableFormat) {
            super(displayName);
            if(category != null)
                putValue(SMALL_ICON, categoryIconManager.getIcon(category));
            
            this.category =  category;
            this.tableFormat = tableFormat;
        }
        
        public AbstractLibraryFormat<LocalFileItem> getTableFormat() {
            return tableFormat.get();
        }
        
        public Category getCategory() {
            return category;
        }
        
        /**
         * Returns the Icon to display when the item is selected.
         */
        public Icon getIcon() {
            if(getValue(SMALL_ICON) != null)
                return (Icon)getValue(SMALL_ICON);
            else
                return null;
        }
        
        /**
         * Returns the text to display when item is selected.
         */
        public String getText() {
            if(getValue(SMALL_ICON) != null)
                return "";
            else
                return (String)getValue(Action.NAME);
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            setSelectedAction(this);
        }                
    }

    /**
     * Clears filters on the combobox. Sets filter text to an empty string and selects
     * the all category menu item.
     */
    public void clearFilters() {
        promptTextField.setText("");
        allMenuItem.doClick();
    }
}
