package org.limewire.ui.swing.search.resultpanel;

import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import static org.limewire.core.api.search.SearchResult.PropertyKey;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import javax.swing.AbstractCellEditor;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.ui.swing.components.HyperLinkButton;
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
    private ActionButtonPanel actionButtonPanel;
    private FromWidget fromWidget = new FromWidget();
    private HyperLinkButton downloadingLink =
        new HyperLinkButton("Downloading...");
    private JLabel downloadButton;
    private HyperLinkButton similarButton = new HyperLinkButton(null);
    private JLabel headingLabel = new JLabel();
    private JLabel subheadingLabel = new JLabel();
    private JXPanel actionPanel = new JXPanel();
    private JXPanel thePanel;
    private JTable table;
    private String schema;
    private VisualSearchResult vsr;
    private boolean isShowingSimilar;
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

        // Find the SearchResultPanel that the table is nested inside.
        // TODO: RMV Want this code?
        /*
        Container child = table.getParent();
        while (true) {
            Container parent = child.getParent();
            if (parent == null) break;
            if (parent instanceof SearchResultsPanel) {
                searchResultsPanel = (SearchResultsPanel) parent;
                break;
            }

            //System.out.println("container is a "
            //    + parent.getClass().getName());
            child = parent;
        }
        */

        this.table = table;
        this.row = row;

        vsr = (VisualSearchResult) value;
        MediaType mediaType =
            MediaType.getMediaTypeForExtension(vsr.getFileExtension());
        schema = mediaType == null ? "other" : mediaType.toString();

        similarCount = vsr.getSimilarResults().size();
        similarButton.setVisible(similarCount > 0);

        if (thePanel == null) thePanel = makePanel();

        actionButtonPanel =
            (ActionButtonPanel) actionEditor.getTableCellEditorComponent(
            table, value, isSelected, row, column);
        actionPanel.removeAll();
        actionPanel.add(actionButtonPanel);

        final JToggleButton junkButton = actionButtonPanel.getJunkButton();
        junkButton.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                float opacity = junkButton.isSelected() ? 0.2f : 1.0f;
                thePanel.setAlpha(opacity);
            }
        });

        populatePanel((VisualSearchResult) value);
        setBackground(isSelected);

        return thePanel;
    }

    public Component getTableCellRendererComponent(
        JTable table, Object value,
        boolean isSelected, boolean hasFocus,
        int row, int column) {

        return (JXPanel) getTableCellEditorComponent(
            table, value, isSelected, row, column);
    }

    @SuppressWarnings("unchecked")
    private Component makeCenterPanel() {
        similarButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                isShowingSimilar = !isShowingSimilar;
                String text = (isShowingSimilar ? "Hide" : "Show")
                    + ' ' + similarCount + " similar files";
                similarButton.setText(text);

                // TODO: RMV Waiting for feedback on replacing use of
                // TODO: RMV VisualSearchResult with SearchResult.
                /*
                BasicSearchResultsModel model = SearchHandlerImpl.model;
                List<VisualSearchResult> list = vsr.getSimilarResults();
                for (VisualSearchResult similarVSR : list) {
                    // TODO: RMV PROBLEM!  The model holds objects that
                    // TODO: RMV implement SearchResult, not VisualSearchResult!
                    // TODO: RMV Why are both interfaces needed?
                    if (isShowingSimilar) {
                        model.addSearchResult(similarVSR); //, list.size() - 1);
                    } else {
                        model.removeSearchResult(similarVSR);
                    }
                }
                */
            }
        });

        JXPanel panel = new JXPanel(new GridBagLayout()) {
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
        panel.add(similarButton, gbc);

        return panel;
    }

    private Component makeLeftPanel() {
        downloadButton = new JLabel(downloadIcon);
        downloadButton.setCursor(
            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        downloadButton.setOpaque(false);
        downloadButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                startDownload();
            }
        });

        GridBagConstraints gbc = new GridBagConstraints();

        JXPanel downloadPanel = new JXPanel(new GridBagLayout());
        downloadPanel.setOpaque(false);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        downloadPanel.add(downloadButton, gbc);
        downloadPanel.add(downloadingLink, gbc);

        JXPanel headingPanel = new JXPanel(new GridBagLayout());
        headingPanel.setOpaque(false);
        gbc.gridx = 0;
        headingPanel.add(headingLabel, gbc);
        headingPanel.add(subheadingLabel, gbc);

        JXPanel panel = new JXPanel(new GridBagLayout());
        panel.setOpaque(false);

        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(downloadPanel, gbc);

        gbc.gridx++;
        panel.add(headingPanel, gbc);

        return panel;
    }

    private JXPanel makePanel() {
        final JXPanel panel = new JXPanel(new GridBagLayout()) {
            @Override
            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                int count = 1;
                if (isShowingSimilar) count += similarCount;
                size.height = count * HEIGHT;
                return size;
            }

            @Override
            public void setBackground(Color bg) {
                super.setBackground(bg);
                if (actionButtonPanel != null) {
                    actionButtonPanel.setBackground(bg);
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
        boolean downloading = vsr.isDownloading();
        downloadButton.setEnabled(!downloading);
        downloadingLink.setVisible(downloading);

        populateHeading(vsr);
        populateSubheading(vsr);
        populateFrom(vsr);

        if (similarCount > 0) {
            String text = "Show " + similarCount + " similar files";
            similarButton.setText(text);
        }
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

    private void startDownload() {
        // Find the BaseResultPanel this is inside.
        Container parent = thePanel.getParent();
        while (!(parent instanceof BaseResultPanel)) {
            parent = parent.getParent();
        }
        BaseResultPanel brp = (BaseResultPanel) parent;

        brp.download(vsr);
        downloadButton.setEnabled(false);
        downloadingLink.setVisible(true);
    }
}