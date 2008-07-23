package org.limewire.ui.swing.mainframe;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JPanel;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.downloads.MainDownloadPanel;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.nav.Navigator.NavCategory;
import org.limewire.ui.swing.search.SearchHandler;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.Line;

import com.google.inject.Inject;

public class LimeWireSwingUI extends JPanel {
    
    private final LeftPanel leftPanel;
    
    /**
	 * The color of the lines separating the GUI panels
	 */
	@Resource
    private Color lineColor;
    
	@Inject
    public LimeWireSwingUI(TopPanel topPanel, LeftPanel leftPanel, MainPanel mainPanel,
            StatusPanel statusPanel, Navigator navigator, SearchHandler searchHandler,
            MainDownloadPanel mainDownloadPanel) {
    	GuiUtils.assignResources(this);
    	        
    	this.leftPanel = leftPanel;
        
        //TODO:move this and have clicks on DownloadSummaryPanel navigate to downloadPanel
        navigator.addNavigablePanel(NavCategory.NONE, MainDownloadPanel.NAME, mainDownloadPanel, false);
        
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
