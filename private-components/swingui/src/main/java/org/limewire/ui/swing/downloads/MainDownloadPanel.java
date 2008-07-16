package org.limewire.ui.swing.downloads;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;

import org.limewire.ui.swing.util.I18n;




public class MainDownloadPanel extends JPanel {
	
    public static final String NAME = "MainDownloadPanel";
	private final CardLayout cardLayout;
	private final JPanel cardPanel;
	private static final String NO_CATEGORY = "noCategory";
	private static final String CATEGORY = "category";
	private DownloadMediator downloadMediator;

	/**
	 * Create the panel
	 */
	public MainDownloadPanel(DownloadMediator downloadMediator) {
		this.downloadMediator = downloadMediator;
		setLayout(new BorderLayout());

		cardPanel = new JPanel();
		cardLayout = new CardLayout();
		cardPanel.setLayout(cardLayout);
		add(cardPanel, BorderLayout.CENTER);

		final AllDownloadPanel noCategoryPanel = new AllDownloadPanel(downloadMediator.getFilteredList());
		noCategoryPanel.setName(NO_CATEGORY);
		cardPanel.add(noCategoryPanel, noCategoryPanel.getName());
		
		final CategoryDownloadPanel categoryPanel = new CategoryDownloadPanel(downloadMediator.getFilteredList());
		categoryPanel.setName(CATEGORY);
		cardPanel.add(categoryPanel, categoryPanel.getName());

		final DownloadSettingsPanel settingsPanel = new DownloadSettingsPanel();
		settingsPanel.setBorder(new LineBorder(Color.black, 1, false));
		add(settingsPanel, BorderLayout.SOUTH);
	}
	
	public void setCategorized(boolean categorized){
		cardLayout.show(cardPanel, categorized? CATEGORY : NO_CATEGORY);
	}
	
	
	private class DownloadSettingsPanel extends JPanel {

		private final JButton pauseAllButton;
		private final JButton resumeAllButton;
		private final JButton clearFinishedButton;
		private final JCheckBox categorizeCheckBox;
		private final JTextField searchBar;
		
		private final Action pauseAction = new AbstractAction(I18n.tr("Pause All")) {

			@Override
			public void actionPerformed(ActionEvent e) {
				downloadMediator.pauseAll();
			}

		};
		
		private final Action resumeAction = new AbstractAction(I18n.tr("Resume All")) {

			@Override
			public void actionPerformed(ActionEvent e) {
				downloadMediator.resumeAll();
			}

		};

		private final Action categorizeAction = new AbstractAction(I18n.tr("Categorize downloads")) {

			@Override
			public void actionPerformed(ActionEvent e) {
				setCategorized(categorizeCheckBox.isSelected());
			}

		};
		
		private final Action clearAction = new AbstractAction(I18n.tr("Clear Finished")) {

			@Override
			public void actionPerformed(ActionEvent e) {
				downloadMediator.clearFinished();
			}

		};
		
		

		public DownloadSettingsPanel() {
			super(new GridBagLayout());

			pauseAllButton = new JButton(pauseAction);	
			resumeAllButton = new JButton(resumeAction);
			clearFinishedButton = new JButton(clearAction);
			categorizeCheckBox = new JCheckBox(categorizeAction);
			
			searchBar = downloadMediator.getFilterBar();
			//TODO: make SearchBar work with filtering
			//searchBar.setDefaultText(I18n.tr("Filter downloads..."));
			Dimension dim = searchBar.getPreferredSize();
			searchBar.setPreferredSize(new Dimension(150, dim.height));
			
			GridBagConstraints gbc = new GridBagConstraints();

			JPanel buttonPanel = new JPanel(new FlowLayout());
			buttonPanel.add(pauseAllButton);
			buttonPanel.add(resumeAllButton);
			
			Insets insets = new Insets(5,5,5,5);
			
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.weightx = .5;
			gbc.insets = insets;
			gbc.fill = GridBagConstraints.NONE;
			gbc.anchor = GridBagConstraints.LINE_START;
			add(buttonPanel, gbc);
			
			
			gbc.gridx++;
			gbc.gridy = 0;
			gbc.insets = insets;
			add(categorizeCheckBox, gbc);

			gbc.gridx++;
			gbc.gridy = 0;
			gbc.insets = insets;
			add(clearFinishedButton, gbc);
			
			gbc.gridx++;
			gbc.gridy = 0;
			gbc.insets = insets;
			gbc.anchor = GridBagConstraints.LINE_END;
			add(searchBar, gbc);			
	
		}
	}

}
