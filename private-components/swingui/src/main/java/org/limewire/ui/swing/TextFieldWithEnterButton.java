package org.limewire.ui.swing;

import java.awt.Color;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;

import net.miginfocom.swing.MigLayout;

import org.limewire.collection.AutoCompleteDictionary;
import org.limewire.ui.swing.components.DropDownListAutoCompleteTextField;

/**
 * This is a custom component that includes a JTextField and a JButton.
 * The text field can display grayed prompting text
 * until the user enters a value.
 * Pressing the button is the same as pressing the enter key.
 * 
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class TextFieldWithEnterButton extends JPanel implements FocusListener {
    
    private JButton button;
    private DropDownListAutoCompleteTextField textField;
    private List<ActionListener> listeners = new ArrayList<ActionListener>();
    private String promptText;
    private boolean empty;
    
    /**
     * Creates a FilteredTextField that displays a given number of columns.
     * @param promptText the prompt text to be displayed in the text field
     *                   (pass null to omit)
     */
    public TextFieldWithEnterButton(
            String promptText, AutoCompleteDictionary friendLibraries) {
        
        this.promptText = promptText;
        
        setLayout(new MigLayout("insets 0 2 0 2"));
        
        // Configure the JTextField.
        textField = new DropDownListAutoCompleteTextField(999);
        textField.setDictionary(friendLibraries);
        prompt();
        textField.addFocusListener(this);
        textField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                notifyListeners();
            }
        });
        textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent event) {
                updateEmpty();
            }
            @Override
            public void insertUpdate(DocumentEvent event) {
                updateEmpty();
            }
            @Override
            public void removeUpdate(DocumentEvent event) {
                updateEmpty();
            }
        });
        add(textField, "grow, aligny top");
        
        // Configure the JButton.
        button = new JButton();
        button.setBorderPainted(false);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                notifyListeners();
            }
        });
        add(button, "grow");
        
        // Fix borders.
        textField.setBorder(null);
    }
    
    public void setIcon(Icon icon) {
        button.setIcon(icon);
    }
    
    public void setIconRollover(Icon icon) {
        button.setRolloverIcon(icon);
    }
    
    /**
     * Adds an ActionListener.
     * @param listener the ActionListener
     */
    public synchronized void addActionListener(ActionListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Repaints this component when focus is gained
     * so default text can be removed.
     */
    @Override
    public void focusGained(FocusEvent e) {
        if (empty) {
            textField.setText("");
            textField.setForeground(Color.BLACK);
        }
    }
    
    /**
     * Repaints this component when focus is lost
     * so default text can be displayed if no text has been entered.
     */
    @Override
    public void focusLost(FocusEvent e) {
        if (empty) prompt();
    }

    /**
     * Gets the text displayed in the JTextField.
     * @return the text
     */
    public String getText() {
        return empty ? "" : textField.getText();
    }
    
    /**
     * Notifies ActionListeners only if the text field isn't empty.
     */
    private synchronized void notifyListeners() {
        if (empty) return;
        
        for (ActionListener listener : listeners) {
            ActionEvent event = new ActionEvent(this, 0, null);
            listener.actionPerformed(event);
        }
    }
    
    /**
     * Prompts the user by added grayed text to the text field.
     */
    private void prompt() {
        textField.setForeground(Color.GRAY);
        textField.setText(promptText);
        empty = true;
    }
    
    /**
     * Removes an ActionListener.
     * @param listener the ActionListener
     */
    public synchronized void removeActionListener(ActionListener listener) {
        listeners.remove(listener);
    }

    /**
     * Sets the maximum number of characters that can be entered.
     * @param size the size
     */
    public void setMaximumSize(int size) {
        AbstractDocument doc = (AbstractDocument) textField.getDocument();
        doc.setDocumentFilter(new DocumentLengthFilter(size));
    }
    
    /**
     * Sets the text displayed in the JTextField.
     * @param the text
     */
    public void setText(String text) {
        textField.setText(text);
        updateEmpty();
    }
    
    /**
     * Updates the value of the empty flag.
     */
    private void updateEmpty() {
        empty = textField.getText().length() == 0;
    }
}
