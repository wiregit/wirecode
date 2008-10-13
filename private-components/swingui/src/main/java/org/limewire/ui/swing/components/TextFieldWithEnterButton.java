package org.limewire.ui.swing.components;

import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;

import net.miginfocom.swing.MigLayout;

import org.limewire.collection.AutoCompleteDictionary;

/**
 * This is a custom component that includes a JTextField and a JButton.
 * The text field can display grayed prompting text
 * until the user enters a value.
 * Pressing the button is the same as pressing the enter key.
 * 
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class TextFieldWithEnterButton extends JPanel {
    
    private JButton button;
    private JTextField textField;
    private List<ActionListener> listeners = new ArrayList<ActionListener>();
    private DropDownListAutoCompleteControl dropDownListAutoCompleteControl;

    /**
     * Creates a FilteredTextField that displays a given number of columns.
     * @param promptText the prompt text to be displayed in the text field
     *                   (pass null to omit)
     */
    public TextFieldWithEnterButton(
            String promptText, AutoCompleteDictionary friendLibraries) {
        setLayout(new MigLayout("insets 0 2 0 2"));
        
        // Configure the JTextField.
        textField = new PromptTextField(promptText, 999) {
            @Override
            protected void paintTextArea(Graphics2D g2) {
                // Do nothing special.
            }
        };
        textField.setBorder(null);
        dropDownListAutoCompleteControl = DropDownListAutoCompleteControl.install(textField, friendLibraries);
        textField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                notifyListeners();
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
     * Gets the text displayed in the JTextField.
     * @return the text
     */
    public String getText() {
        return textField.getText();
    }
    
    /**
     * Notifies ActionListeners only if the text field isn't empty.
     */
    private synchronized void notifyListeners() {
        if (textField.getText().isEmpty()) {
            return;
        }
        
        for (ActionListener listener : listeners) {
            ActionEvent event = new ActionEvent(this, 0, null);
            listener.actionPerformed(event);
        }
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
    }

    public void setAutoCompleteDictionary(AutoCompleteDictionary dictionary) {
        dropDownListAutoCompleteControl.setDictionary(dictionary);
    }
}
