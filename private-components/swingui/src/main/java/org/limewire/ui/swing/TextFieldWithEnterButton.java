package org.limewire.ui.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;

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
    private JTextField textField;
    private List<ActionListener> listeners = new ArrayList<ActionListener>();
    private String promptText;
    private boolean empty = true;
    
    /**
     * Creates a FilteredTextField that displays a given number of columns.
     * @param columns the number of visible columns in the text field
     * @param promptText the prompt text to be displayed in the text field
     *                   (pass null to omit)
     * @param icon the Icon to be display on the button
     */
    public TextFieldWithEnterButton(
        int columns, String promptText, Icon icon) {
        
        this.promptText = promptText;
        
        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        
        // Configure the JTextField.
        textField = new JTextField(columns);
        textField.addFocusListener(this);
        textField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                notifyListeners();
            }
        });
        prompt();
        add(textField);
        
        // Configure the JButton.
        button = new JButton(icon);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        Dimension size =
            new Dimension(icon.getIconWidth(), icon.getIconHeight());
        button.setPreferredSize(size);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                notifyListeners();
            }
        });
        add(button);
        
        // Fix borders.
        Border border = textField.getBorder();
        textField.setBorder(null);
        setBorder(border);
    }
    
    public synchronized void addActionListener(ActionListener listener) {
        listeners.add(listener);
    }
    
    private synchronized void notifyListeners() {
        for (ActionListener listener : listeners) {
            ActionEvent event = new ActionEvent(this, 0, null);
            listener.actionPerformed(event);
        }
    }
    
    public synchronized void removeActionListener(ActionListener listener) {
        listeners.remove(listener);
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
        empty = textField.getText().length() == 0;
        prompt();
    }
    
    /**
     * Gets the text displayed in the JTextField.
     * @return the text
     */
    public String getText() {
        return empty ? "" : textField.getText();
    }
    
    private void prompt() {
        if (empty) {
            textField.setForeground(Color.GRAY);
            textField.setText(promptText);
        }
    }
    
    /**
     * Sets the text displayed in the JTextField.
     * @param the text
     */
    public void setText(String text) {
        textField.setText(text);
        empty = false;
    }
}
