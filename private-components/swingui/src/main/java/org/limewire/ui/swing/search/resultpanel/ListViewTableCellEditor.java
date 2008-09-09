package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.core.api.search.SearchResult.PropertyKey;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import javax.swing.AbstractCellEditor;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.application.Resource;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.util.MediaType;

/**
 * This class is responsible for rendering an individual SearchResult
 * in "List View".
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class ListViewTableCellEditor
extends AbstractCellEditor
implements TableCellEditor, TableCellRenderer {

    private static final Color SELECTED_COLOR = Color.GREEN;

    public static final int HEIGHT = 50;
    public static final int WIDTH = 740;

    @Resource private Icon downloadIcon;

    private ActionColumnTableCellEditor actionEditor;
    private Component actionComponent;
    private FromWidget fromWidget = new FromWidget();
    private JLabel headingLabel = new JLabel();
    private JLabel similarLabel = new JLabel();
    private JLabel subheadingLabel = new JLabel();
    private JPanel actionPanel = new JPanel();
    private JPanel thePanel;
    private JTable table;
    private String schema;
    private VisualSearchResult vsr;
    private int row;
    private int similarCount;

    public ListViewTableCellEditor(ActionColumnTableCellEditor actionEditor) {
        this.actionEditor = actionEditor;

        // Cause the @Resource fields to be injected
        // using properties in AppFrame.properties.
        // The icon PNG file is in
        // private-components/swingui/src/main/resources/
        // org/limewire/ui/swing/mainframe/resources/icons.
        GuiUtils.assignResources(this);
    }

    public Object getCellEditorValue() {
        return vsr;
    }

    public Component getTableCellEditorComponent(
        JTable table, Object value, boolean isSelected, int row, int column) {

        //System.out.println(
        //    "ListViewTableCellEditor.getTableCellEditorComponent: row = " + row);

        this.table = table;
        this.row = row;

        vsr = (VisualSearchResult) value;
        MediaType mediaType =
            MediaType.getMediaTypeForExtension(vsr.getFileExtension());
        schema = mediaType == null ? "other" : mediaType.toString();

        similarCount = vsr.getSimiliarResults().size();

        if (thePanel == null) thePanel = makePanel();

        actionComponent = actionEditor.getTableCellEditorComponent(
            table, value, isSelected, row, column);
        actionPanel.removeAll();
        actionPanel.add(actionComponent);

        populatePanel((VisualSearchResult) value);
        setBackground(isSelected);

        return thePanel;
    }

    public Component getTableCellRendererComponent(
        JTable table, Object value,
        boolean isSelected, boolean hasFocus,
        int row, int column) {

        return (JPanel) getTableCellEditorComponent(
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

        gbc.gridy++;
        panel.add(similarLabel, gbc);

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
        // TODO: RMV Why doesn't the download icon appear?
        panel.add(label, gbc);

        gbc.gridx++;
        panel.add(headingLabel, gbc);

        gbc.gridy++;
        panel.add(subheadingLabel, gbc);

        return panel;
    }

    private JPanel makePanel() {
        final JPanel panel = new JPanel(new GridBagLayout()) {
            @Override
            public void setBackground(Color bg) {
                super.setBackground(bg);
                if (actionComponent != null) {
                    actionComponent.setBackground(bg);
                }
            }
        };

        panel.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 0;
        panel.add(makeLeftPanel(), gbc);

        gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 1;
        panel.add(makeCenterPanel(), gbc);

        gbc.weightx = 0;
        actionPanel.setOpaque(false);
        panel.add(actionPanel, gbc);

        return panel;
    }

    private void populateFrom(VisualSearchResult vsr) {
        List<String> people = new ArrayList<String>();
        Collection<RemoteHost> sources = vsr.getSources();
        for (RemoteHost source : sources) {
            people.add(source.getHostDescription());
        }
        fromWidget.setPeople(people);
    }

    private void populateHeading(VisualSearchResult vsr) {
        String name = getProperty(vsr, PropertyKey.NAME);
        String heading;

        if (MediaType.SCHEMA_AUDIO.equals(schema)) {
            heading = getProperty(vsr, PropertyKey.ARTIST_NAME) + " - " + name;
        } else if (MediaType.SCHEMA_VIDEO.equals(schema)
            || MediaType.SCHEMA_IMAGES.equals(schema)) {
            heading = name;
        } else {
            heading = name + "." + vsr.getFileExtension();
        }

        headingLabel.setText(heading);
    }

    private String getProperty(VisualSearchResult vsr, PropertyKey key) {
        Object property = vsr.getProperty(key);
        return property == null ? "?" : property.toString();
    }

    private void populatePanel(VisualSearchResult vsr) {
        populateHeading(vsr);
        populateSubheading(vsr);
        populateFrom(vsr);

        String text = similarCount == 0 ?
            "" : "Show " + similarCount + " similar files";
        similarLabel.setText(text);
    }

    private void populateSubheading(VisualSearchResult vsr) {
        String subheading;

        if (MediaType.SCHEMA_AUDIO.equals(schema)) {
            Object albumTitle = vsr.getProperty(PropertyKey.ALBUM_TITLE);
            String prefix = albumTitle == null ? "" : albumTitle + " - ";
            subheading = prefix + vsr.getProperty(PropertyKey.QUALITY)
                + " - " + vsr.getProperty(PropertyKey.LENGTH);
        } else if (MediaType.SCHEMA_VIDEO.equals(schema)) {
            subheading = vsr.getProperty(PropertyKey.QUALITY)
                + " - " + vsr.getProperty(PropertyKey.LENGTH);
        } else if (MediaType.SCHEMA_IMAGES.equals(schema)) {
            Object calendar = vsr.getProperty(PropertyKey.DATE_CREATED);
            SimpleDateFormat sdf = new SimpleDateFormat("M/d/yyyy");
            subheading = calendar == null ?
                "" : sdf.format(((Calendar) calendar).getTime());
        } else {
            subheading = "{application name}"
                + " - " + vsr.getProperty(PropertyKey.FILE_SIZE) + "MB";
        }

        subheadingLabel.setText(subheading);
    }

    private void setBackground(boolean isSelected) {
        if (thePanel == null) return;

        Color color = isSelected ? SELECTED_COLOR : Color.WHITE;
        thePanel.setBackground(color);

        int childCount = thePanel.getComponentCount();
        for (int i = 0; i < childCount; i++) {
            Component child = thePanel.getComponent(i);
            child.setBackground(color);
        }
    }
}