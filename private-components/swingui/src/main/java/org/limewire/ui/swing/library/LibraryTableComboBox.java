package org.limewire.ui.swing.library;

import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextField;
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

public class LibraryTableComboBox extends LimeComboBox {
    
    private final CategoryIconManager categoryIconManager;

    Provider<AllTableFormat<LocalFileItem>> allFormat;
    
    private ComboBoxAction selectedAction;
    private JLabel textLabel;
    private PromptTextField promptTextField;
    private ButtonGroup buttonGroup = new ButtonGroup();
    
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
        this.allFormat = allFormat;
        
        final JPopupMenu popupMenu = new JPopupMenu();
        overrideMenu(popupMenu);
        ComboBoxAction action = new ComboBoxAction(I18n.tr("All"), null,  allFormat);
        JMenuItem item = createRadioMenuItem(action);
        item.setSelected(true);
        popupMenu.add(item);
        popupMenu.add(createRadioMenuItem(new ComboBoxAction(I18n.tr("Audio"), Category.AUDIO,audioFormat)));
        popupMenu.add(createRadioMenuItem(new ComboBoxAction(I18n.tr("Video"), Category.VIDEO, videoFormat)));
        popupMenu.add(createRadioMenuItem(new ComboBoxAction(I18n.tr("Image"), Category.IMAGE, imageFormat)));
        popupMenu.add(createRadioMenuItem(new ComboBoxAction(I18n.tr("Document"), Category.DOCUMENT, documentFormat)));
        popupMenu.add(createRadioMenuItem(new ComboBoxAction(I18n.tr("Programs"), Category.PROGRAM, programFormat)));
        popupMenu.add(createRadioMenuItem(new ComboBoxAction(I18n.tr("Other"), Category.OTHER, otherFormat)));
        
        textLabel = new JLabel();
        add(textLabel);
        
        promptTextField = new PromptTextField(I18n.tr("Filter"));
        TextFieldClipboardControl.install(promptTextField);
        textFieldDecorator.decorateClearablePromptField(promptTextField, AccentType.NONE);
        promptTextField.setBorder(BorderFactory.createEmptyBorder(0,10,0,10));
        promptTextField.addKeyListener(new KeyAdapter(){
            @Override
            public void keyTyped(KeyEvent e) {
                if(e.getKeyChar() == KeyEvent.VK_ENTER) {
                    popupMenu.setVisible(false);
                }
            }      
        });

        popupMenu.add(promptTextField);
        popupMenu.addPropertyChangeListener(new PropertyChangeListener(){

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if(evt.getPropertyName().equals("visible") && ((Boolean)evt.getNewValue()) == true) {
                    promptTextField.requestFocusInWindow();
                    promptTextField.selectAll();
                    //TODO: this isn't getting the focus
                }
            }
            
        });
        
        popupMenu.addPopupMenuListener(new PopupMenuListener(){

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                setDisplayText();
            }

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            }
            
        });

        setMySelectedItem(action);
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
    
    private void setMySelectedItem(ComboBoxAction action) {
        selectedAction = action;
        setDisplayText();
        fireChangeEvent(action);
    }
    
    private void setDisplayText() {
        String text = getFilterField().getText();
        if(text != null && text.trim().length() > 0) 
            textLabel.setText(getFilterField().getText().trim());
        else
            textLabel.setText(selectedAction.getText());
        textLabel.setIcon(selectedAction.getIcon());
    }
    
    private JMenuItem createRadioMenuItem(Action action) {
        JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(action);
        buttonGroup.add(menuItem);
        return menuItem;
    }
    
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
            setMySelectedItem(this);
        }                
    }
}
