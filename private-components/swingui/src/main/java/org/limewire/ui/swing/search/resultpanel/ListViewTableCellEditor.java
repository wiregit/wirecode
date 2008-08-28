package org.limewire.ui.swing.search.resultpanel;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractCellEditor;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
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
public class ListViewTableCellEditor
extends AbstractCellEditor
implements TableCellEditor, TableCellRenderer {

    public static final int HEIGHT = 50;
    public static final int WIDTH = 727;

    @Resource private Icon downloadIcon;

    private FromWidget fromWidget = new FromWidget();
    private JLabel headingLabel = new JLabel();
    private JLabel similarLabel = new JLabel();
    private JLabel subheadingLabel = new JLabel();
    private JPanel thePanel;
    private VisualSearchResult vsr;

    public ListViewTableCellEditor() {
        // Cause the @Resource fields to be injected
        // using properties in AppFrame.properties.
        // The icon PNG file is in swingui/src/main/resources/
        // org/limewire/ui/swing/mainframe/resources/icons.
        GuiUtils.assignResources(this);
    }

    public Object getCellEditorValue() {
        return vsr;
    }

    public Component getTableCellEditorComponent(
        JTable table, Object value, boolean isSelected, int row, int column) {
        vsr = (VisualSearchResult) value;

        if (thePanel == null) thePanel = makePanel();

        populatePanel((VisualSearchResult) value);
        setBackground(isSelected);

        return thePanel;
    }

    public Component getTableCellRendererComponent(
        JTable table, Object value,
        boolean isSelected, boolean hasFocus,
        int row, int column) {
        return getTableCellEditorComponent(
            table, value, isSelected, row, column);
    }

    private Component makeCenterPanel() {
        JPanel panel = new JPanel(new GridBagLayout()) {
            @Override
            public void setBackground(Color color) {
                super.setBackground(color);
                for (Component component : getComponents()) {
                    component.setBackground(color);
                }
            }
        };

        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridy = 0;
        panel.add(fromWidget, gbc);

        // TODO: RMV Uncomment this after debugging.
        //if (similarCount > 0) {
            gbc.gridy++;
            panel.add(similarLabel, gbc);
        //}

        return panel;
    }

    private Component makeLeftPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel label = new JLabel(downloadIcon);
        panel.add(label, gbc);

        gbc.gridx++;
        panel.add(headingLabel, gbc);

        gbc.gridy++;
        panel.add(subheadingLabel, gbc);

        return panel;
    }

    private JPanel makePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 0;
        panel.add(makeLeftPanel(), gbc);

        gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 1;
        panel.add(makeCenterPanel(), gbc);

        gbc.weightx = 0;
        panel.add(makeRightPanel(), gbc);

        return panel;
    }

    private Component makeRightPanel() {
        JPanel panel = new ActionButtonPanel();
        panel.setOpaque(false);
        return panel;
    }

    private void setBackground(boolean isSelected) {
        if (thePanel == null) return;
        Color color = isSelected ? new Color(220, 220, 255) : Color.WHITE;
        thePanel.setBackground(color);
        int childCount = thePanel.getComponentCount();
        for (int i = 0; i < childCount; i++) {
            Component child = thePanel.getComponent(i);
            child.setBackground(color);
        }
    }

    private void populatePanel(VisualSearchResult vsr) {
        String heading = "";
        heading += vsr.getProperty(SearchResult.PropertyKey.ARTIST_NAME);
        heading += " - ";
        heading += vsr.getProperty(SearchResult.PropertyKey.NAME);
        headingLabel.setText(heading);

        Object comment = vsr.getProperty(SearchResult.PropertyKey.COMMENTS);
        String subheading = "";
        if (comment != null) subheading += comment + " - ";
        subheading += vsr.getProperty(SearchResult.PropertyKey.QUALITY);
        subheading += " - ";
        subheading += vsr.getProperty(SearchResult.PropertyKey.LENGTH);
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
}