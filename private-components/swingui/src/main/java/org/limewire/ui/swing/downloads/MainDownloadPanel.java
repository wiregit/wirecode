package org.limewire.ui.swing.downloads;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.downloads.table.DownloadStateMatcher;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.matchers.Matcher;

import com.google.inject.Inject;
import com.google.inject.Singleton;




@Singleton
public class MainDownloadPanel extends JPanel {
	
    public static final String NAME = "MainDownloadPanel";
	private final CardLayout cardLayout;
	private final JPanel cardPanel;
	private static final String NO_CATEGORY = "noCategory";
	private static final String CATEGORY = "category";
	
	private final DownloadMediator downloadMediator;

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

    private final Action clearAction = new AbstractAction(I18n.tr("Clear Finished")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            downloadMediator.clearFinished();
        }
    };
    
	/**
	 * Create the panel
	 */
	@Inject
	public MainDownloadPanel(DownloadListManager downloadListManager) {
		this.downloadMediator = new DownloadMediator(downloadListManager);
		setLayout(new BorderLayout());
		
        resumeAction.setEnabled(false);
        pauseAction.setEnabled(false);
        clearAction.setEnabled(false);

		cardPanel = new JPanel();
		cardLayout = new CardLayout();
		cardPanel.setLayout(cardLayout);
		add(cardPanel, BorderLayout.CENTER);

		final AllDownloadPanel noCategoryPanel = new AllDownloadPanel(downloadMediator.getFilteredList());
		noCategoryPanel.setName(NO_CATEGORY);
		cardPanel.add(noCategoryPanel, noCategoryPanel.getName());
		
		final CategoryDownloadPanel categoryPanel = CategoryDownloadPanel.createCategoryDownloadPanel(downloadMediator.getFilteredList());
		categoryPanel.setName(CATEGORY);
		cardPanel.add(categoryPanel, categoryPanel.getName());

		final DownloadSettingsPanel settingsPanel = new DownloadSettingsPanel();
		settingsPanel.setBorder(new LineBorder(Color.BLACK, 1, false));
		add(settingsPanel, BorderLayout.NORTH);
		
		final EventList<DownloadItem> pausableList = new FilterList<DownloadItem>(downloadMediator.getFilteredList(), 
		        new PausableMatcher());
		final EventList<DownloadItem> resumableList = new FilterList<DownloadItem>(downloadMediator.getFilteredList(), 
                new ResumableMatcher());
		final EventList<DownloadItem> doneList = new FilterList<DownloadItem>(downloadMediator.getFilteredList(), 
                        new DownloadStateMatcher(DownloadState.DONE));
		
		pausableList.addListEventListener(new ListEventListener<DownloadItem>() {
            @Override
            public void listChanged(ListEvent<DownloadItem> listChanges) {
                pauseAction.setEnabled(pausableList.size() > 0);
            }
        });

        resumableList.addListEventListener(new ListEventListener<DownloadItem>() {
            @Override
            public void listChanged(ListEvent<DownloadItem> listChanges) {
                resumeAction.setEnabled(resumableList.size() > 0);
            }
        });
        
        doneList.addListEventListener(new ListEventListener<DownloadItem>() {
            @Override
            public void listChanged(ListEvent<DownloadItem> listChanges) {
                clearAction.setEnabled(doneList.size() > 0);
            }
        });
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
		private final JLabel titleLabel;
		

		private final Action categorizeAction = new AbstractAction(I18n.tr("Categorize downloads")) {

			@Override
			public void actionPerformed(ActionEvent e) {
				setCategorized(categorizeCheckBox.isSelected());
			}

		};
			
		

		public DownloadSettingsPanel() {
			super(new GridBagLayout());

			pauseAllButton = new JButton(pauseAction);	
			resumeAllButton = new JButton(resumeAction);
			clearFinishedButton = new JButton(clearAction);
			categorizeCheckBox = new JCheckBox(categorizeAction);
			titleLabel = new JLabel(I18n.tr("Downloads"));
			FontUtils.changeStyle(titleLabel, Font.BOLD);
			FontUtils.changeSize(titleLabel, 5);
			searchBar = downloadMediator.getFilterTextField();
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
			add(titleLabel, gbc);
			
            
            gbc.gridx++;
            gbc.gridy = 0;
            gbc.insets = insets;
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
	
	private static class PausableMatcher implements Matcher<DownloadItem> {
        @Override
        public boolean matches(DownloadItem item) {
            return item.getState().isPausable();
        }
    }

    private static class ResumableMatcher implements Matcher<DownloadItem> {
        @Override
        public boolean matches(DownloadItem item) {
            return item.getState().isResumable();
        }
    }

}
