package org.limewire.ui.swing.nav;

import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JTextField;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;

public class SearchBar extends JTextField {

	private String defaultText = "Search...";
	
	/**
	 * the color of the search box text when defaultText is displayed
	 */
	@Resource
	private Color defaultTextColor;
	
	public SearchBar() {
	    GuiUtils.assignResources(this);
	    setText(defaultText);
		setForeground(defaultTextColor);
		addFocusListener(new SearchFocusListener());
	}
	
	public void setDefaultText(String defaultText){
		this.defaultText = defaultText;
	    setText(defaultText);
	}

	private class SearchFocusListener implements FocusListener {

		@Override
		public void focusGained(FocusEvent e) {
		    String text = getText();
		    if (text == null || text.equals(defaultText)) {
		        setText("");
		        setForeground(null);
		    }
		}

		@Override
		public void focusLost(FocusEvent e) {
			String text = getText();
			if (text == null || "".equals(text)) {
				setText(defaultText);
				setForeground(defaultTextColor);
			}
		}
	}
}
