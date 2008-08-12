package org.limewire.ui.swing.search.resultpanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import org.limewire.core.api.search.SearchResult;
import org.limewire.ui.swing.search.model.VisualSearchResult;

/**
 * This class is responsible for rendering an individual SearchResult
 * in "List View".
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class SearchResultListCellRenderer extends DefaultListCellRenderer {

    //@Resource private Icon downloadIcon;

    public SearchResultListCellRenderer() {
        // Cause the @Resource fields to be injected
        // using properties in AppFrame.properties.
        // The icon PNG file is in swingui/src/main/resources/
        // org/limewire/ui/swing/mainframe/resources/icons.
        //GuiUtils.assignResources(this);
    }

    @Override
    public Component getListCellRendererComponent(
        JList list, Object value, int index,
        boolean isSelected, boolean cellHasFocus) {

        VisualSearchResult vsr = (VisualSearchResult) value;

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(makeLeftPanel(vsr), BorderLayout.WEST);
        panel.add(makeCenterPanel(vsr), BorderLayout.CENTER);
        panel.add(makeRightPanel(), BorderLayout.EAST);
        return panel;
    }

    private Component makeCenterPanel(VisualSearchResult vsr) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        int sourceCount = vsr.getSources().size();
        int similarCount = vsr.getSimiliarResults().size();

        gbc.anchor = GridBagConstraints.EAST;
        gbc.gridy = 0;
        panel.add(new JLabel("From " + sourceCount + " people"), gbc);

        // TODO: RMV Uncomment this after debugging.
        //if (similarCount > 0) {
            gbc.gridy++;
            panel.add(new JLabel("Show " + similarCount + " similar files"), gbc);
        //}

        return panel;
    }

    private Component makeLeftPanel(VisualSearchResult vsr) {
        Map<Object, Object> properties = vsr.getProperties();

        String heading = "";
        heading += properties.get(SearchResult.PropertyKey.ARTIST_NAME);
        heading += " - ";
        heading += properties.get(SearchResult.PropertyKey.TRACK_NAME);

        Object comment = properties.get(SearchResult.PropertyKey.COMMENT);
        String subheading = "";
        if (comment != null) subheading += comment + " - ";
        subheading += properties.get(SearchResult.PropertyKey.QUALITY);
        subheading += " - ";
        subheading += properties.get(SearchResult.PropertyKey.TRACK_TIME);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        //System.out.println("icon width = " + downloadIcon.getIconWidth());
        //System.out.println("icon height = " + downloadIcon.getIconHeight());
        //JLabel label = new JLabel(downloadIcon);
        JLabel label = new JLabel("icon");
        label.setBorder(BorderFactory.createLineBorder(Color.RED));
        panel.add(label, gbc);

        gbc.gridx++;
        panel.add(new JLabel(heading), gbc);

        gbc.gridy++;
        panel.add(new JLabel(subheading), gbc);

        return panel;
    }

    private Component makeRightPanel() {
        return new ActionButtonPanel();
    }
}