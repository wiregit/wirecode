package org.limewire.ui.swing.mainframe;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JPanel;

import org.jdesktop.application.Resource;
import org.limewire.core.api.download.DownloadManager;
import org.limewire.core.api.search.SearchFactory;
import org.limewire.ui.swing.downloads.DownloadMediator;
import org.limewire.ui.swing.downloads.MainDownloadPanel;
import org.limewire.ui.swing.nav.NavigatorImpl;
import org.limewire.ui.swing.nav.Navigator.NavCategory;
import org.limewire.ui.swing.search.SearchHandler;
import org.limewire.ui.swing.search.SearchHandlerImpl;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.Line;

import com.google.inject.Injector;

public class LimeWireSwingUI extends JPanel {
    
    private final TopPanel topPanel;
    private final LeftPanel leftPanel;
    private final MainPanel mainPanel;
    private final StatusPanel statusPanel;
    private final NavigatorImpl navigator;
    private final SearchHandler searchHandler;
    private DownloadMediator downloadMediator;
    
    /**
	 * The color of the lines separating the GUI panels
	 */
	@Resource
    private Color lineColor;
    
    public LimeWireSwingUI(Injector coreInjector) {
    	GuiUtils.injectFields(this);
    	
        this.downloadMediator = new DownloadMediator(coreInjector.getInstance(DownloadManager.class));        
        
        this.mainPanel = new MainPanel();
        this.leftPanel = new LeftPanel(downloadMediator);
        this.navigator = new NavigatorImpl(mainPanel, leftPanel);
        this.topPanel = new TopPanel();
        this.statusPanel = new StatusPanel();
        this.searchHandler = new SearchHandlerImpl(navigator, coreInjector.getInstance(SearchFactory.class));
        //TODO:move this and have clicks on DownloadSummaryPanel navigate to downloadPanel
        MainDownloadPanel downloadPanel = new MainDownloadPanel(downloadMediator);
        navigator.addNavigablePanel(NavCategory.NONE, MainDownloadPanel.NAME, downloadPanel, false);
        
        leftPanel.setSearchHandler(searchHandler);
        navigator.addDefaultNavigableItems(searchHandler);
        
        GridBagLayout layout = new GridBagLayout();
        setLayout(layout);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;        
        add(new Line(lineColor), gbc);
        
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        add(topPanel, gbc);
        
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        add(new Line(lineColor), gbc);
        
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.weightx = 0;
        gbc.weighty = 1;
        gbc.gridwidth = 1;
        add(leftPanel, gbc);
        
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.weightx = 0;
        gbc.weighty = 1;
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        add(new Line(lineColor), gbc);
        
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        add(mainPanel, gbc);
        
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        add(new Line(lineColor), gbc);
        
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        add(statusPanel, gbc);
    }
    
    public void goHome() {        
        leftPanel.goHome();
    }
    
    

}
