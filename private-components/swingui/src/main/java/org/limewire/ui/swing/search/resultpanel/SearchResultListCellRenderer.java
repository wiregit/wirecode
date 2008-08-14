package org.limewire.ui.swing.search.resultpanel;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import org.jdesktop.application.Resource;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.SearchResult;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * This class is responsible for rendering an individual SearchResult
 * in "List View".
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class SearchResultListCellRenderer extends DefaultListCellRenderer {

    @Resource private Icon downloadIcon;

    //private JLabel fromLabel = new JLabel();
    private FromWidget fromWidget = new FromWidget();
    private JLabel headingLabel = new JLabel();
    private JLabel similarLabel = new JLabel();
    private JLabel subheadingLabel = new JLabel();
    private JPanel panel;

    public SearchResultListCellRenderer() {
        // Cause the @Resource fields to be injected
        // using properties in AppFrame.properties.
        // The icon PNG file is in swingui/src/main/resources/
        // org/limewire/ui/swing/mainframe/resources/icons.
        GuiUtils.assignResources(this);
    }

    @Override
    public Component getListCellRendererComponent(
        JList list, Object value, int index,
        boolean isSelected, boolean cellHasFocus) {

        if (panel == null) makePanel();

        populatePanel((VisualSearchResult) value);
        setBackground(isSelected);

        return panel;
    }

    private void populatePanel(VisualSearchResult vsr) {
        Map<Object, Object> properties = vsr.getProperties();

        String heading = "";
        heading += properties.get(SearchResult.PropertyKey.ARTIST_NAME);
        heading += " - ";
        heading += properties.get(SearchResult.PropertyKey.TRACK_NAME);
        headingLabel.setText(heading);

        Object comment = properties.get(SearchResult.PropertyKey.COMMENT);
        String subheading = "";
        if (comment != null) subheading += comment + " - ";
        subheading += properties.get(SearchResult.PropertyKey.QUALITY);
        subheading += " - ";
        subheading += properties.get(SearchResult.PropertyKey.TRACK_TIME);
        subheadingLabel.setText(subheading);

        //int sourceCount = vsr.getSources().size();
        //fromLabel.setText("From " + sourceCount + " people");
        List<String> people = new ArrayList<String>();
        Collection<RemoteHost> sources = vsr.getSources();
        for (RemoteHost source : sources) {
            people.add(source.getHostDescription());
        }
        fromWidget = new FromWidget(people.toArray(new String[] {}));
        
        int similarCount = vsr.getSimiliarResults().size();
        similarLabel.setText("Show " + similarCount + " similar files");
    }

    private void makePanel() {
        panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 0;
        panel.add(makeLeftPanel(), gbc);

        gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 1;
        panel.add(makeCenterPanel(), gbc);

        gbc.weightx = 0;
        panel.add(makeRightPanel(), gbc);
    }

    private Component makeCenterPanel() {
        JPanel centerPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridy = 0;
        //centerPanel.add(fromLabel, gbc);
        centerPanel.add(fromWidget, gbc);

        // TODO: RMV Uncomment this after debugging.
        //if (similarCount > 0) {
            gbc.gridy++;
            centerPanel.add(similarLabel, gbc);
        //}

        return centerPanel;
    }

    private Component makeLeftPanel() {
        JPanel leftPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel label = new JLabel(downloadIcon);
        leftPanel.add(label, gbc);

        gbc.gridx++;
        leftPanel.add(headingLabel, gbc);

        gbc.gridy++;
        leftPanel.add(subheadingLabel, gbc);

        return leftPanel;
    }

    private Component makeRightPanel() {
        JPanel rightPanel = new ActionButtonPanel();
        return rightPanel;
    }

    private void setBackground(boolean isSelected) {
        Color color = isSelected ? new Color(220, 220, 255) : Color.WHITE;
        panel.setBackground(color);
        int childCount = panel.getComponentCount();
        for (int i = 0; i < childCount; i++) {
            Component child = panel.getComponent(i);
            child.setBackground(color);
        }
    }
}