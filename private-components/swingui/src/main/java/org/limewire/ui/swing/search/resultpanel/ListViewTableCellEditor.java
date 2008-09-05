package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.core.api.search.SearchResult.PropertyKey;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
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

    public static final int HEIGHT = 50;
    public static final int WIDTH = 727;

    @Resource private Icon downloadIcon;

    private FromWidget fromWidget = new FromWidget();
    private JLabel headingLabel = new JLabel();
    private JLabel similarLabel = new JLabel();
    private JLabel subheadingLabel = new JLabel();
    private JPanel thePanel;
    private String schema;
    private VisualSearchResult vsr;
    private int similarCount;

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

        MediaType mediaType =
            MediaType.getMediaTypeForExtension(vsr.getFileExtension());
        schema = mediaType == null ? "other" : mediaType.toString();

        similarCount = vsr.getSimiliarResults().size();

        if (thePanel == null) thePanel = makePanel();

        populatePanel((VisualSearchResult) value);
        setBackground(isSelected);

        thePanel.setBorder(BorderFactory.createLineBorder(Color.RED, 2));

        return thePanel;
    }

    public Component getTableCellRendererComponent(
        JTable table, Object value,
        boolean isSelected, boolean hasFocus,
        int row, int column) {

        JPanel panel = (JPanel) getTableCellEditorComponent(
            table, value, isSelected, row, column);
        panel.setBorder(BorderFactory.createEmptyBorder());
        return panel;
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

        panel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                System.out.println("got a mouse press; button = " + e.getButton());
                if (e.getButton() == 3) {
                    System.out.println("got a right click");
                }
            }
        });

        return panel;
    }

    private Component makeRightPanel() {
        JPanel panel = new ActionButtonPanel();
        panel.setOpaque(false);
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
        String heading;

        if (MediaType.SCHEMA_AUDIO.equals(schema)) {
            heading = vsr.getProperty(PropertyKey.ARTIST_NAME)
                + " - " + vsr.getProperty(PropertyKey.NAME);
        } else if (MediaType.SCHEMA_VIDEO.equals(schema)
            || MediaType.SCHEMA_IMAGES.equals(schema)) {
            heading = vsr.getProperty(PropertyKey.NAME).toString();
        } else {
            heading = vsr.getProperty(PropertyKey.NAME)
                + "." + vsr.getFileExtension();
        }

        headingLabel.setText(heading);
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
        Color color = isSelected ? new Color(220, 220, 255) : Color.WHITE;
        thePanel.setBackground(color);
        int childCount = thePanel.getComponentCount();
        for (int i = 0; i < childCount; i++) {
            Component child = thePanel.getComponent(i);
            child.setBackground(color);
        }
    }
}