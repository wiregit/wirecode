package com.limegroup.gnutella.gui;

/**
 * file: UpdateWindow.java
 * auth: afisk
 * desc: This class creates a window to display the progress of
 *       an update. This simply displays a progress bar with
 *       a status label. This implements runnable to allow it
 *       to be called easily with SwingUtilities.invokeLater().
 */

import java.awt.event.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.Border;
import com.limegroup.gnutella.SettingsManager;
import com.limegroup.gnutella.update.VersionUpdate;
import com.limegroup.gnutella.downloader.CantConnectException;

public class UpdateHandler {
	
	private SettingsManager _settings;
	private ProgressWindow _progressWindow;
	private MultiLineLabel _label;

	/** Instantiates the Update Handler with a panel
	 *  for displaying the update notification window. */
	public UpdateHandler() {
		_settings = SettingsManager.instance();
	}

	/** Shows the message window asking the user if they
	 *  would like to update to the newer version.  Returns
     *  true if the user would like to upgrade.  If the user
     *  clicks "Don't check again", changes the SettingsManager. */
	public boolean showUpdatePrompt() {
		JPanel updatePanel = new UpdatePanel();
		int i = JOptionPane.showConfirmDialog(MainFrame.instance(),
											  updatePanel,"Update",
											  JOptionPane.YES_NO_OPTION);
		return (i == JOptionPane.YES_OPTION);
	}

	/** instantiates the progress window and shows it. */
	public void showProgressWindow(int updateSize) {
		_progressWindow = new ProgressWindow(updateSize);
		_progressWindow.setVisible(true);
	}
	
	/** hides the progress window and disposes of it. */
	public void hideProgressWindow() {
		_progressWindow.setVisible(false);
		_progressWindow.dispose();
	}

	/** tells the  window to update it progress. 
     *      @requires progress window is displayed */
	public void update(int amountRead) {
		_progressWindow.setAmountRead(amountRead);
		SwingUtilities.invokeLater(_progressWindow);
	}

    /** Displays an error box telling the user that the update failed. */
    public void showCouldntUpdate() {
        String str = "Could not connect to the "+
        "LimeWire server.  Please try again later.";						
        Utilities.showError(str); 
    }

	/** Class that creates a new update panel for prompting
	 *  the user for whether or not they would like to
	 *  update. */
	private class UpdatePanel extends JPanel {
		public UpdatePanel() {
			String label = "LimeWire has detected a newer version.  "+
			"Would you like to update?";
			_label = new MultiLineLabel(label);
			String checkLabel = "Don't inform me of updates.";
			final JCheckBox checkBox = new JCheckBox(checkLabel);
			checkBox.setForeground((Color)UIManager.get("Label.foreground"));
			checkBox.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					_settings.setCheckAgain(!checkBox.isSelected());
				}
			});
			JPanel checkBoxPanel = new JPanel();
			checkBoxPanel.add(checkBox);
			checkBoxPanel.add(Box.createHorizontalGlue());
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			add(_label);
			add(Box.createVerticalStrut(6));
			add(checkBoxPanel);
			setPreferredSize(new Dimension(Integer.MAX_VALUE, 80));
		}
	}

	/** This class constructs a dialog window that
	 *  shows the progress of an update. It implements 
	 *  Runnable so that it is easy to call on the 
	 *  event dispatch thread. */
	private class ProgressWindow extends JDialog implements Runnable {
	
		private final JProgressBar _progressBar;
		private double _updateSize;
		private double _amountRead;
		private JLabel _label;
		
		/** Creates the JDialog and sets the initial
		 *  values for amount read, file size, etc. */
		public ProgressWindow(int updateSize) {
			super(MainFrame.instance(), "Update");
			setSize(350, 100);
			setLocationRelativeTo(MainFrame.instance());	   
			JPanel panel = new JPanel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
			Border border = BorderFactory.createEmptyBorder(12,12,12,12);
			panel.setBorder(border);
			_progressBar = new JProgressBar();
			_progressBar.setStringPainted(true);
			_progressBar.setString("0%");
			_label = new JLabel("Performing update...");
			_updateSize = (double)updateSize;
			_amountRead = 0;
			getContentPane().add(panel);
			panel.add(_label);
			panel.add(Box.createVerticalStrut(6));
			panel.add(_progressBar);
		}
		
		/** sets the status label to complete. */
		public void setComplete() {
			_label.setText("Update Completed Successfully");
		}
		
		/** sets the amount read in bytes. */
		public void setAmountRead(int amountRead) {
			_amountRead = (double)amountRead;
		}
		
		/** calculates the new percent read and sets the
		 *  progress bar to that value. */
		public void run() {
			int percent = (int)(100*(_amountRead/_updateSize));	
			_progressBar.setValue(percent);
			_progressBar.setString(Integer.toString(percent)+"%");
		}
	}
}
