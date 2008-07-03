package org.limewire.ui.swing.nav;

import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JTextField;



public class SearchBar extends JTextField {

	private final String defaultText = "Search...";
	
	public SearchBar() {
	    super();
	    setText(defaultText);
		setForeground(Color.GRAY);
		addFocusListener(new SearchFocusListener());
	}

	private class SearchFocusListener implements FocusListener {

		@Override
		public void focusGained(FocusEvent e) {
		    String text = getText();
		    if(text == null || text.equals(defaultText)) {
		        setText("");
		        setForeground(null);
		    }
		}

		@Override
		public void focusLost(FocusEvent e) {
			String text = getText();
			if (text == null || "".equals(text)) {
				setText(defaultText);
				setForeground(Color.GRAY);
			}
		}
	}
}
