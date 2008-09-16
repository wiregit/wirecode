package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractCellEditor;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXHyperlink;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.SearchResult.PropertyKey;
import org.limewire.ui.swing.downloads.MainDownloadPanel;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.library.MyLibraryPanel;
import org.limewire.ui.swing.nav.NavigableTree;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.search.FilterEvent;
import org.limewire.ui.swing.search.model.BasicDownloadState;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.util.MediaType;

import com.google.inject.Inject;

/**
 * This class is responsible for rendering an individual SearchResult
 * in "List View".
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class ListViewTableCellEditor
extends AbstractCellEditor
implements TableCellEditor, TableCellRenderer {

    private final Color SELECTED_COLOR = Color.GREEN;

    private final String SEARCH_TEXT_COLOR = "red";

    // Change this null to avoid seeing filter matches.
    private final String FILTER_TEXT_COLOR = null; // "blue";

    public static final int HEIGHT = 50;
    public static final int LEFT_WIDTH = 440;
    public static final int WIDTH = 740;

    private String searchText;

    @Resource private Icon downloadIcon;

    private ActionColumnTableCellEditor actionEditor;
    private ActionButtonPanel actionButtonPanel;
    private FromWidget fromWidget = new FromWidget();
    private JLabel downloadButton;
    private JXHyperlink downloadingLink = new JXHyperlink();
    private JXHyperlink similarButton = new JXHyperlink();
    private JLabel headingLabel = new JLabel();
    private JLabel subheadingLabel = new JLabel();
    private JLabel otherLabel = new JLabel();
    private JXPanel actionPanel = new JXPanel();
    private JXPanel thePanel;
    private JTable table;
    private NavigableTree navTree;
    private String filterText = "";
    private String schema;

    private VisualSearchResult vsr;
    private boolean isShowingSimilar;
   // private int row;
    private int similarCount;

    @Inject
    public ListViewTableCellEditor(
        ActionColumnTableCellEditor actionEditor,
        NavigableTree navTree) {

        this.actionEditor = actionEditor;
        this.navTree = navTree;

        // Cause the @Resource fields to be injected
        // using properties in AppFrame.properties.
        // The icon PNG file is in
        // private-components/swingui/src/main/resources/
        // org/limewire/ui/swing/mainframe/resources/icons.
        GuiUtils.assignResources(this);

        EventAnnotationProcessor.subscribe(this);
    }

    /**
     * Adds an HTML bold tag around every occurrence of highlightText.
     * Note that comparisons are case insensitive.
     * @param text the text to be modified
     * @return the text containing bold tags
     */
    private String highlightMatches(String sourceText) {
        boolean haveSearchText = searchText != null && searchText.length() > 0;
        System.out.println("haveSearchText = " + haveSearchText);
        boolean haveFilterText = filterText != null && filterText.length() > 0;

        // If there is no search or filter text then return sourceText as is.
        if (!haveSearchText && !haveFilterText) return sourceText;

        String lowerText = sourceText.toLowerCase();
        if (haveSearchText) searchText = searchText.toLowerCase();
        if (haveFilterText) filterText = filterText.toLowerCase();

        int searchIndex = haveSearchText ? lowerText.indexOf(searchText) : -1;
        int filterIndex = haveFilterText ? lowerText.indexOf(filterText) : -1;

        boolean foundSearchText = searchIndex != -1;
        boolean foundFilterText = filterIndex != -1;

        // If sourceText doesn't contains searchText or filterText
        // then return sourceText as is.
        if (!foundSearchText && !foundFilterText) return sourceText;

        // We know that either the search text or the filter text was found
        // at this point.
        // Get the index of the first one found.
        int index =
            foundSearchText && !foundFilterText ? searchIndex :
            foundFilterText && !foundSearchText ? filterIndex :
            Math.min(searchIndex, filterIndex);

        String result = "<html>";

        while (index != -1) {
            // Which one was found first?
            boolean useSearchText = index == searchIndex;

            int matchLength =
                useSearchText ? searchText.length() : filterText.length();
            String match = sourceText.substring(index, index + matchLength);
            String color =
                useSearchText ? SEARCH_TEXT_COLOR : FILTER_TEXT_COLOR;

            result += sourceText.substring(0, index);
            if (color == null) {
                result += match;
            } else {
                result += "<span style='color:" + color + "; font-weight:bold'>";
                result += match;
                result += "</span>";
            }

            // Find the next occurrences.

            sourceText = sourceText.substring(index + matchLength);
            lowerText = lowerText.substring(index + matchLength);

            searchIndex = haveSearchText ? lowerText.indexOf(searchText) : -1;
            filterIndex = haveFilterText ? lowerText.indexOf(filterText) : -1;

            foundSearchText = searchIndex != -1;
            foundFilterText = filterIndex != -1;

            index =
                !foundSearchText && !foundFilterText ? -1 :
                foundSearchText && !foundFilterText ? searchIndex :
                foundFilterText && !foundSearchText ? filterIndex :
                Math.min(searchIndex, filterIndex);
        }

        result += sourceText; // tack on the remaining sourceText
        System.out.println("highlighted text is " + result);
        return result;
    }

    private PropertyMatch getPropertyMatch(VisualSearchResult vsr) {
        if (filterText == null) return null;

        // If any data displayed in the headingLabel or subheadingLabel
        // matches the search or filter criteria,
        // then the otherLabel isn't needed.
        // In this case, return an empty string.
        if (headingLabel.getText().contains(filterText)) return null;
        if (subheadingLabel.getText().contains(filterText)) return null;

        // Look for metadata that matches the search criteria.
        Map<PropertyKey, Object> props = vsr.getProperties();
        for (PropertyKey key : props.keySet()) {
            String value = vsr.getPropertyString(key);

            if (value.toLowerCase().contains(filterText)) {
                String betterKey = key.toString().toLowerCase();
                betterKey = betterKey.replace('_', ' ');
                return new PropertyMatch(betterKey, value);
            }
        }

        // No match found.
        return null;
    }

    public Object getCellEditorValue() {
        return vsr;
    }

    public Component getTableCellEditorComponent(
        JTable table, Object value, boolean isSelected, int row, int column) {
        //System.out.println(
        //    "ListViewTableCellEditor.getTableCellEditorComponent: row = " + row);

        this.table = table;

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
                JToggleButton junkButton = actionButtonPanel.getJunkButton();
                markAsJunk(junkButton.isSelected());
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

        return getTableCellEditorComponent(
            table, value, isSelected, row, column);
    }
    
    private String hyperlinkText(Object...pieces ) {
        StringBuilder bldr = new StringBuilder();
        bldr.append("<html><u>");
        for(Object s : pieces) {
            bldr.append(s);
        }
        bldr.append("</u></html>");
        return bldr.toString();
    }

    private Component makeCenterPanel() {
        similarButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                isShowingSimilar = !isShowingSimilar;
                String text = hyperlinkText(tr(isShowingSimilar ? "Hide" : "Show"), 
                        " ", similarCount, tr(" similar files"));
                similarButton.setText(text);

                // TODO: RMV Need code here to display similar results.
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
                int row = table.rowAtPoint(e.getPoint());
                startDownload(row);
            }
        });

        downloadingLink.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (vsr.getDownloadState() == BasicDownloadState.DOWNLOADING) {
                    navTree.getNavigableItemByName(
                        Navigator.NavCategory.DOWNLOAD,
                        MainDownloadPanel.NAME).select();
                } else if (vsr.getDownloadState() == BasicDownloadState.DOWNLOADED) {
                    navTree.getNavigableItemByName(
                        Navigator.NavCategory.LIBRARY,
                        MyLibraryPanel.NAME).select();
                }
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
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.weightx = 1;
        headingPanel.add(headingLabel, gbc);
        headingPanel.add(subheadingLabel, gbc);
        headingPanel.add(otherLabel, gbc);

        JXPanel panel = new JXPanel(new GridBagLayout()) {
            @Override
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }

            @Override
            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                size.width = LEFT_WIDTH;
                return size;
            }
        };

        panel.setOpaque(false);

        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        panel.add(downloadPanel, gbc);

        gbc.gridx++;
        gbc.weightx = 1;
        panel.add(headingPanel, gbc);

        return panel;
    }

    private JXPanel makePanel() {
        final JXPanel panel = new JXPanel(new GridBagLayout()) {
            @Override
            public Dimension getPreferredSize() {
                int count = 1;
                if (isShowingSimilar) count += similarCount;
                return new Dimension(WIDTH, count * HEIGHT);
            }

            @Override
            public void setBackground(Color bg) {
                super.setBackground(bg);
                if (actionButtonPanel != null) {
                    actionButtonPanel.setBackground(bg);
                }
            }
        };

        GridBagConstraints gbc = new GridBagConstraints();

        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 0;
        panel.add(makeLeftPanel(), gbc);

        gbc.weightx = 1;
        panel.add(makeCenterPanel(), gbc);

        gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 0;
        actionPanel.setOpaque(false);
        panel.add(actionPanel, gbc);

        return panel;
    }

    private void markAsJunk(boolean junk) {
        JToggleButton junkButton = actionButtonPanel.getJunkButton();
        junkButton.setSelected(junk);

        float opacity = junk ? 0.2f : 1.0f;
        thePanel.setAlpha(opacity);
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

        headingLabel.setText(highlightMatches(heading));
    }

    private String getProperty(VisualSearchResult vsr, PropertyKey key) {
        Object property = vsr.getProperty(key);
        return property == null ? "?" : property.toString();
    }

    /**
     * This is invoked when the user enters filter text
     * in the SortAndFilterPanel.
     * @param event the FilterEvent
     */
    @EventSubscriber
    public void handleFilter(FilterEvent event) {
        // Change the text that is highlighted in search results
        // to be the filter text.
        // At least for now this functionality isn't desired.
        filterText = event.getText().toLowerCase();
    
        // Cause the table associated with the renders and editors to repaint
        // so text matching highlightText will be displayed/highlighted.
        AbstractTableModel model = (AbstractTableModel) table.getModel();
        model.fireTableDataChanged();
    }

    private void populateOther(VisualSearchResult vsr) {
        PropertyMatch pm = getPropertyMatch(vsr);
        if (pm == null) {
            otherLabel.setText("");
        } else {
            String html = highlightMatches(pm.value);
            String tag = "<html>";
            // Insert the key, a colon and a space after the html start tag.
            html = tag + pm.key + ": " + html.substring(tag.length());
            otherLabel.setText(html);
        }
    }

    private void populatePanel(VisualSearchResult vsr) {
        if (vsr == null) return;

        switch (vsr.getDownloadState()) {
            case NOT_STARTED:
                downloadButton.setEnabled(true);
                downloadingLink.setText("");
                downloadingLink.setVisible(false);
                break;
            case DOWNLOADING:
                downloadButton.setEnabled(false);
                downloadingLink.setText(hyperlinkText(tr("Downloading...")));
                downloadingLink.setVisible(true);
                break;
            case DOWNLOADED:
                downloadButton.setEnabled(false);
                downloadingLink.setText(hyperlinkText(tr("Download Complete")));
                downloadingLink.setVisible(true);
                break;
        }

        populateHeading(vsr);
        populateSubheading(vsr);
        populateOther(vsr);
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

        subheadingLabel.setText(highlightMatches(subheading));
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

    private void startDownload(int row) {
        // Find the BaseResultPanel this is inside.
        Container parent = thePanel.getParent();
        while (!(parent instanceof BaseResultPanel)) {
            parent = parent.getParent();
        }
        BaseResultPanel brp = (BaseResultPanel) parent;

        downloadButton.setEnabled(false);
        downloadingLink.setVisible(true);
        brp.download(vsr, row);
    }

    static class PropertyMatch {
        String key;
        String value;
        PropertyMatch(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }
}