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

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.Painter;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.DownloadState;
import org.limewire.player.api.AudioPlayer;
import org.limewire.ui.swing.components.CustomCheckBox;
import org.limewire.ui.swing.components.HeadingLabel;
import org.limewire.ui.swing.downloads.table.DownloadStateMatcher;
import org.limewire.ui.swing.painter.ButtonPainter;
import org.limewire.ui.swing.painter.SubpanelPainter;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;
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
	
	private DownloadSettingsPanel settingsPanel;

    private final Action pauseAction = new AbstractAction(I18n.tr("Pause All")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            downloadMediator.pauseAll();
        }
    };

    private final Action resumeAction = new AbstractAction(I18n.tr("Resume All")) {
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
    
    private final Action categorizeAction = new AbstractAction(I18n.tr("Categorize downloads")) {

        @Override
        public void actionPerformed(ActionEvent e) {
            setCategorized(settingsPanel.isCategorized());
        }

    };
    
    
	/**
	 * Create the panel
	 */
	@Inject
	public MainDownloadPanel(AllDownloadPanelFactory allDownloadPanelFactory, 
	        CategoryDownloadPanelFactory categoryDownloadPanelFactory,
	        DownloadListManager downloadListManager, AudioPlayer player) {
		this.downloadMediator = new DownloadMediator(downloadListManager);
		setLayout(new BorderLayout());
		
        resumeAction.setEnabled(false);
        pauseAction.setEnabled(false);
        clearAction.setEnabled(false);

		cardPanel = new JPanel();
		cardLayout = new CardLayout();
		cardPanel.setLayout(cardLayout);
		add(cardPanel, BorderLayout.CENTER);
		
        
        final AllDownloadPanel noCategoryPanel = allDownloadPanelFactory.create(downloadMediator.getFilteredList());
		noCategoryPanel.setName(NO_CATEGORY);
		cardPanel.add(noCategoryPanel, noCategoryPanel.getName());
		
		final CategoryDownloadPanel categoryPanel = categoryDownloadPanelFactory.create(downloadMediator.getFilteredList());
		categoryPanel.setName(CATEGORY);
		cardPanel.add(categoryPanel, categoryPanel.getName());
		
		settingsPanel = new DownloadSettingsPanel();
		add(settingsPanel, BorderLayout.NORTH);
		
		cardLayout.show(cardPanel, NO_CATEGORY);
		
		EventList<DownloadItem> pausableList = GlazedListsFactory.filterList(downloadMediator.getFilteredList(), 
		        new PausableMatcher());
		EventList<DownloadItem> resumableList = GlazedListsFactory.filterList(downloadMediator.getFilteredList(), 
                new ResumableMatcher());
		EventList<DownloadItem> doneList = GlazedListsFactory.filterList(downloadMediator.getFilteredList(), 
                        new DownloadStateMatcher(DownloadState.DONE));
		
		pausableList.addListEventListener(new ListEventListener<DownloadItem>() {
            @Override
            public void listChanged(ListEvent<DownloadItem> listChanges) {
                pauseAction.setEnabled(listChanges.getSourceList().size() > 0);
            }
        });

        resumableList.addListEventListener(new ListEventListener<DownloadItem>() {
            @Override
            public void listChanged(ListEvent<DownloadItem> listChanges) {
                resumeAction.setEnabled(listChanges.getSourceList().size() > 0);
            }
        });
        
        doneList.addListEventListener(new ListEventListener<DownloadItem>() {
            @Override
            public void listChanged(ListEvent<DownloadItem> listChanges) {
                clearAction.setEnabled(listChanges.getSourceList().size() > 0);
            }
        });
    }
	
	public void setCategorized(boolean categorized){
		cardLayout.show(cardPanel, categorized? CATEGORY : NO_CATEGORY);
	}
	
    
    
	
	private class DownloadSettingsPanel extends JXPanel {	    	    
		private final JButton pauseAllButton;
		private final JButton resumeAllButton;
	    private final JXButton clearFinishedButton;
		private final JCheckBox categoriseCheckBox;
		private final JTextField searchBar;
		private final JLabel titleLabel;
		
		public DownloadSettingsPanel() {
			super(new BorderLayout());
			
			this.setPreferredSize(new Dimension(getPreferredSize().width, 34));
			
			Painter painter = new SubpanelPainter();
			
			setBackgroundPainter(painter);

			pauseAllButton = new JButton(pauseAction);	
			resumeAllButton = new JButton(resumeAction);
			clearFinishedButton = new JXButton(clearAction);
			categoriseCheckBox = new CustomCheckBox(categorizeAction);

			clearFinishedButton.setBackgroundPainter(new ButtonPainter());
			clearFinishedButton.setOpaque(false);
			clearFinishedButton.setForeground(Color.WHITE);
			clearFinishedButton.setFont(new Font("Arial", Font.PLAIN, 10));
			clearFinishedButton.setBorderPainted(false);
			clearFinishedButton.setPreferredSize(
			        new Dimension((int)clearFinishedButton.getPreferredSize().getWidth(), 21));
						
			titleLabel = new HeadingLabel(I18n.tr("Downloads"));
			FontUtils.changeSize(titleLabel, 5);
			FontUtils.changeStyle(titleLabel, Font.PLAIN);
			
			categoriseCheckBox.setOpaque(false);
			categoriseCheckBox.setForeground(Color.WHITE);
			FontUtils.changeStyle(categoriseCheckBox, Font.PLAIN);
			
			searchBar = downloadMediator.getFilterTextField();
			
            searchBar.setPreferredSize(new Dimension(150,19));
			
	        categorizeAction.setEnabled(true);
			
			GridBagConstraints gbc = new GridBagConstraints();

			JPanel buttonPanel = new JPanel(new FlowLayout());
			buttonPanel.setOpaque(false);
			buttonPanel.add(pauseAllButton);
			buttonPanel.add(resumeAllButton);
			
			
			Insets insets = new Insets(5,5,5,5);
			
			JXPanel titlePanel = new JXPanel(new GridBagLayout());
			titlePanel.setOpaque(false);
			
			gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = .5;
            gbc.insets = insets;
            gbc.fill = GridBagConstraints.VERTICAL;
            gbc.anchor = GridBagConstraints.LINE_START;
            titlePanel.add(titleLabel,gbc);
			
			JPanel restPanel = new JPanel();
			restPanel.setOpaque(false);
			
			gbc.anchor = GridBagConstraints.NORTH;
            gbc.gridx++;
            gbc.gridy = 0;
            gbc.insets = insets;
            restPanel.add(categoriseCheckBox, gbc);

            gbc.anchor = GridBagConstraints.SOUTH;
			gbc.gridx++;
			gbc.gridy = 0;
            gbc.insets = insets;
			restPanel.add(clearFinishedButton, gbc);
			
			gbc.gridx++;
			gbc.gridy = 0;
			gbc.insets = insets;
			gbc.anchor = GridBagConstraints.LINE_END;
			restPanel.add(searchBar, gbc);
			
			add(titlePanel, BorderLayout.WEST);
            add(restPanel, BorderLayout.EAST);

		}
		
		public boolean isCategorized(){
	        return categoriseCheckBox.isSelected();
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
