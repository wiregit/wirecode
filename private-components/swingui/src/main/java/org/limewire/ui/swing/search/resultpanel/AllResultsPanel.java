package org.limewire.ui.swing.search.resultpanel;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.Scrollable;

import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.util.FontUtils;

import ca.odell.glazedlists.swing.EventListModel;
import ca.odell.glazedlists.swing.EventSelectionModel;

public class AllResultsPanel extends JXPanel implements Scrollable {
    
    private final JList resultsList;
    
    public AllResultsPanel(EventListModel<VisualSearchResult> listModel, EventSelectionModel<VisualSearchResult> selectionModel) {
        setLayout(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 0);
        JLabel title = new JLabel("All Results from Everyone");
        FontUtils.changeFontSize(title, 5);
        FontUtils.changeStyle(title, Font.BOLD);
        add(title, gbc);
                
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(0, 5, 5, 0);
        resultsList = new JList(listModel);
        resultsList.setSelectionModel(selectionModel);
        add(resultsList, gbc);
        
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.insets = new Insets(0, 5, 0, 5);
        add(new JLabel("sponsored results"), gbc);
    }
    
    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return resultsList.getPreferredScrollableViewportSize();
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect,
            int orientation, int direction) {
        return resultsList.getScrollableBlockIncrement(visibleRect, orientation, direction);
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return resultsList.getScrollableTracksViewportHeight();
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect,
            int orientation, int direction) {
        return resultsList.getScrollableUnitIncrement(visibleRect, orientation, direction);
    }



}
