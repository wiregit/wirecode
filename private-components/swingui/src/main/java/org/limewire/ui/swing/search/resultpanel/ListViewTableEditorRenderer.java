package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.ui.swing.search.resultpanel.HyperlinkTextUtil.hyperlinkText;
import static org.limewire.ui.swing.util.I18n.tr;
import static org.limewire.ui.swing.util.I18n.trn;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.AbstractCellEditor;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXHyperlink;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.Category;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.SearchResult.PropertyKey;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.downloads.MainDownloadPanel;
import org.limewire.ui.swing.library.LibraryNavigator;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.search.RemoteHostActions;
import org.limewire.ui.swing.search.model.BasicDownloadState;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.table.RowColorResolver;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.util.StringUtils;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * This class is responsible for rendering an individual SearchResult
 * in "List View".
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class ListViewTableEditorRenderer
extends AbstractCellEditor
implements TableCellEditor, TableCellRenderer {

    private final CategoryIconManager categoryIconManager;
    
    private static final String HTML = "<html>";
    private static final String CLOSING_HTML_TAG = "</html>";
    private final Log LOG = LogFactory.getLog(getClass());
    private final PropertyKeyComparator AUDIO_COMPARATOR = 
        new PropertyKeyComparator(PropertyKey.GENRE, PropertyKey.BITRATE, PropertyKey.TRACK_NUMBER, PropertyKey.SAMPLE_RATE);
    private final PropertyKeyComparator VIDEO_COMPARATOR = 
        new PropertyKeyComparator(PropertyKey.YEAR, PropertyKey.RATING, PropertyKey.COMMENTS, PropertyKey.HEIGHT, 
                                  PropertyKey.WIDTH, PropertyKey.BITRATE);
    private final PropertyKeyComparator DOCUMENTS_COMPARATOR = 
        new PropertyKeyComparator(PropertyKey.DATE_CREATED, PropertyKey.AUTHOR);
    private final PropertyKeyComparator PROGRAMS_COMPARATOR = 
        new PropertyKeyComparator(PropertyKey.PLATFORM, PropertyKey.COMPANY);

    public static final int HEIGHT = 56;
    public static final int WIDTH = 740;

    private String searchText;
    @Resource private Icon similarResultsIcon;
    @Resource private Color headingLabelColor;
    @Resource private Color subHeadingLabelColor;
    @Resource private Color metadataLabelColor;
    @Resource private Font headingFont;
    @Resource private Font subHeadingFont;
    @Resource private Font metadataFont;
    @Resource private Font similarResultsButtonFont;
    
    private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("M/d/yyyy");

    private final ActionColumnTableCellEditor actionEditor;
    private final RowColorResolver<VisualSearchResult> rowColorResolver;
    private ActionButtonPanel actionButtonPanel;
    private FromWidget fromWidget;
    private JLabel itemIconLabel;
    private JXHyperlink similarButton = new JXHyperlink();
    private JLabel headingLabel = new JLabel();
    private JLabel subheadingLabel = new JLabel();
    private JLabel metadataLabel = new JLabel();
    private JXPanel rightPanel = new JXPanel();
    private JXPanel editorComponent;
    private final JXHyperlink downloadingLink = new JXHyperlink();

    private VisualSearchResult vsr;
    private int column;
    private JComponent similarResultIndentation;
    private JPanel indentablePanel;
    private JPanel leftPanel;
    private JPanel centerPanel;
    private JXPanel searchResultTextPanel;

    @AssistedInject
    ListViewTableEditorRenderer(CategoryIconManager categoryIconManager,
        @Assisted ActionColumnTableCellEditor actionEditor, 
        @Assisted String searchText, 
        @Assisted RemoteHostActions remoteHostActions, 
        @Assisted Navigator navigator, 
        @Assisted RowColorResolver<VisualSearchResult> colorResolver) {

        this.categoryIconManager = categoryIconManager;
        
        this.actionEditor = actionEditor;
        this.searchText = searchText;
        this.rowColorResolver = colorResolver;

        GuiUtils.assignResources(this);

        similarButton.setFont(similarResultsButtonFont);
        
        fromWidget = new FromWidget(remoteHostActions);
        
        makePanel(navigator);
    }

    /**
     * Adds an HTML bold tag around every occurrence of highlightText.
     * Note that comparisons are case insensitive.
     * @param text the text to be modified
     * @return the text containing bold tags
     */
    private String highlightMatches(String sourceText) {
        boolean haveSearchText = searchText != null && searchText.length() > 0;

        // If there is no search or filter text then return sourceText as is.
        if (!haveSearchText)
            return sourceText;
        
        return SearchHighlightUtil.highlight(searchText, sourceText);
    }

    private PropertyMatch getPropertyMatch(VisualSearchResult vsr) {
        if(searchText == null)
            return null;
        
        ArrayList<PropertyKey> properties = new ArrayList<PropertyKey>(vsr.getProperties().keySet());
        Collections.sort(properties, getComparator());
        for (PropertyKey key : properties) {
            String value = vsr.getPropertyString(key);

            String propertyMatch = highlightMatches(value);
            if (value != null && isDifferentLength(value, propertyMatch)) {
                String betterKey = key.toString().toLowerCase();
                betterKey = betterKey.replace('_', ' ');
                return new PropertyMatch(betterKey, propertyMatch);
            }
        }

        // No match found.
        return null;
    }

    private Comparator<PropertyKey> getComparator() {
        switch (getCategory()) {
        case AUDIO:
            return AUDIO_COMPARATOR;
        case VIDEO:
            return VIDEO_COMPARATOR;
        case DOCUMENT:
            return DOCUMENTS_COMPARATOR;
        default:
            return PROGRAMS_COMPARATOR;
        }
    }
    
    private static class PropertyKeyComparator implements Comparator<PropertyKey> {
        private final PropertyKey[] keyOrder;

        public PropertyKeyComparator(PropertyKey... keys) {
            this.keyOrder = keys;
        }

        @Override
        public int compare(PropertyKey o1, PropertyKey o2) {
            if (o1 == o2) {
                return 0;
            }
            
            for(PropertyKey key : keyOrder) {
                if (o1 == key) {
                    return -1;
                } else if (o2 == key) {
                    return 1;
                }
            }
            return 0;
        }
    }

    public Object getCellEditorValue() {
        return vsr;
    }

    public Component getTableCellEditorComponent(
        final JTable table, Object value, boolean isSelected, int row, final int col) {

        vsr = (VisualSearchResult) value;
        column = col;
        LOG.debugf("getTableCellEditorComponent: row = {0} column = {1}", row, col);

        actionButtonPanel =
            (ActionButtonPanel) actionEditor.getTableCellEditorComponent(
                    table, value, isSelected, row, col);
        
        if (editorComponent == null) {
            editorComponent = new JXPanel(new MigLayout("insets 0 0 0 0", "0[]0", "0[]0")) {

                @Override
                public void setBackground(Color bg) {
                    super.setBackground(bg);
                    if (column == 2) {
                        actionButtonPanel.setBackground(bg);
                    }
                }
            };
            
            actionButtonPanel.configureForListView(table);
            
            itemIconLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    actionButtonPanel.startDownload();
                    table.editingStopped(new ChangeEvent(table));
                }
            });
        }

        LOG.debugf("row: {0} shouldIndent: {1}", row, vsr.getSimilarityParent() != null);
        
        populatePanel((VisualSearchResult) value, col);
        
        editorComponent.removeAll();

        JPanel panel = col == 0 ? leftPanel : col == 1 ? centerPanel : rightPanel;
        
        editorComponent.add(panel, "height 100%");

        return editorComponent;
    }

    private int getSimilarResultsCount() {
        return vsr == null ? 0 : vsr.getSimilarResults().size();
    }

    public Component getTableCellRendererComponent(
        JTable table, Object value,
        boolean isSelected, boolean hasFocus,
        int row, int column) {

        return getTableCellEditorComponent(
            table, value, isSelected, row, column);
    }
    
    private boolean isShowingSimilarResults() {
        return vsr != null && getSimilarResultsCount() > 0 && vsr.isChildrenVisible();
    }

    private JPanel makeCenterPanel() {
        similarButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                boolean toggleVisibility = !isShowingSimilarResults();
                vsr.setChildrenVisible(toggleVisibility);
                
                similarButton.setText(getHideShowSimilarFilesButtonText());
            }
        });

        JXPanel panel = new JXPanel(new MigLayout("insets 0 0 0 0", "0[]0", "0[]0[]0")) {
            @Override
            public void setBackground(Color color) {
                super.setBackground(color);
                for (Component component : getComponents()) {
                    component.setBackground(color);
                }
            }
        };

        panel.setOpaque(false);
        panel.add(fromWidget, "wrap");
        panel.add(similarButton);

        return panel;
    }

    private Component makeLeftPanel(final Navigator navigator) {
        itemIconLabel = new JLabel();
        itemIconLabel.setCursor(
            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        itemIconLabel.setOpaque(false);
        
        headingLabel.setForeground(headingLabelColor);
        headingLabel.setFont(headingFont);

        subheadingLabel.setForeground(subHeadingLabelColor);
        subheadingLabel.setFont(subHeadingFont);
        
        metadataLabel.setForeground(metadataLabelColor);
        metadataLabel.setFont(metadataFont);
        
        JXPanel downloadPanel = new JXPanel(new MigLayout("insets 7 0 0 5", "0[]", "0[top]0"));
        downloadPanel.setOpaque(false);
        downloadPanel.add(itemIconLabel);

        searchResultTextPanel = new JXPanel(new MigLayout("insets 0 0 0 0", "3[]", "5[]0[]0[]0"));
        searchResultTextPanel.setOpaque(false);
        searchResultTextPanel.add(headingLabel, "wrap, wmin 350");
        searchResultTextPanel.add(subheadingLabel, "wrap, wmin 350");
        searchResultTextPanel.add(metadataLabel, "wmin 350");

        JXPanel panel = new JXPanel(new MigLayout("insets 0 0 0 0", "5[][]0", "0[]0"));

        panel.setOpaque(false);

        panel.add(downloadPanel);
        panel.add(searchResultTextPanel);
        
        FontUtils.changeSize(downloadingLink, -2.0f);
        downloadingLink.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                BasicDownloadState downloadState = vsr.getDownloadState();
                if (downloadState == BasicDownloadState.DOWNLOADING) {
                    navigator.getNavItem(
                        NavCategory.DOWNLOAD,
                        MainDownloadPanel.NAME).select(vsr);
                } else if (downloadState == BasicDownloadState.DOWNLOADED || downloadState == BasicDownloadState.LIBRARY) {
                    navigator.getNavItem(
                        NavCategory.LIBRARY,
                        LibraryNavigator.NAME_PREFIX + vsr.getCategory()).select(vsr);
                    
                }
            }
        });

        return panel;
    }

    private void makePanel(Navigator navigator) {
        leftPanel = makeIndentablePanel(makeLeftPanel(navigator));

        centerPanel = makeCenterPanel();

        rightPanel.setOpaque(false);
    }

    private JPanel makeIndentablePanel(Component component) {
        indentablePanel = new JPanel(new MigLayout("insets 0 0 0 5", "[][]", "[]"));
        indentablePanel.setOpaque(false);
        similarResultIndentation = new JPanel(new BorderLayout());
        similarResultIndentation.add(new JLabel(similarResultsIcon), BorderLayout.CENTER);
        indentablePanel.add(component, "cell 1 0, top, left");
        return indentablePanel;
    }

    private void markAsJunk(boolean junk) {
        float opacity = junk ? 0.2f : 1.0f;
        editorComponent.setAlpha(opacity);
    }

    private void populateFrom(VisualSearchResult vsr) {
        Collection<RemoteHost> sources = vsr.getSources();
        fromWidget.setPeople(new ArrayList<RemoteHost>(sources));
    }

    /**
     * Returns a value to indicate whether the heading was decorated to highlight
     * parts of the content that match search terms
     */
    private boolean populateHeading(VisualSearchResult vsr) {
        String heading = vsr.getHeading();
        String highlightMatches = highlightMatches(heading);
        LOG.debugf("Heading: {0} highlightedMatches: {1}", heading, highlightMatches);
        headingLabel.setText(highlightMatches);
        return isDifferentLength(heading, highlightMatches);
    }

    private void populateOther(VisualSearchResult vsr, boolean shouldHidePropertyMatches) {
        metadataLabel.setText("");
        if (shouldHidePropertyMatches) { 
            return;
        }
        
        PropertyMatch pm = getPropertyMatch(vsr);
        
        if (pm != null) {
            String html = pm.highlightedValue;
            String tag = HTML;
            // Insert the following: the key, a colon and a space after the html start tag, then the closing tags.
            html = tag + pm.key + ": " + 
                    html.substring(tag.length(), html.length() - CLOSING_HTML_TAG.length()) + CLOSING_HTML_TAG;
            metadataLabel.setText(html);
        }
    }

    private void populatePanel(VisualSearchResult vsr, int column) {
        if (vsr == null) return;
        
        if (column == 0) {
            if (vsr.getSimilarityParent() != null) {
                indentablePanel.add(similarResultIndentation, "cell 0 0, height 100%");
                Color backgroundColor = rowColorResolver.getColorForItemRow(vsr.getSimilarityParent());
                similarResultIndentation.setBackground(backgroundColor);
            } else {
                indentablePanel.remove(similarResultIndentation);
            }
            
            itemIconLabel.setIcon(categoryIconManager.getIcon(vsr.getCategory()));

            boolean headingDecorated = populateHeading(vsr);

            populateSearchResultTextPanel();
            
            switch (vsr.getDownloadState()) {
            case NOT_STARTED:
                downloadingLink.setText("");
                boolean subheadingDecorated = populateSubheading(vsr);
                populateOther(vsr, headingDecorated || subheadingDecorated);
                break;
            case DOWNLOADING:
                downloadingLink.setText(hyperlinkText(tr("Downloading...")));
                break;
            case DOWNLOADED:
                downloadingLink.setText(hyperlinkText(tr("Download Complete")));
                break;
            case LIBRARY:
                downloadingLink.setText(hyperlinkText(tr("In Your Library")));
                break;
            }
            
        } else if (column == 1) {
            similarButton.setVisible(getSimilarResultsCount() > 0);
            populateFrom(vsr);
            
            if (getSimilarResultsCount() > 0) {
                similarButton.setText(getHideShowSimilarFilesButtonText());
            }
            
        } else  { //col 2
            actionButtonPanel.setDownloadingDisplay(vsr);
            rightPanel.removeAll();
            rightPanel.add(actionButtonPanel);
        }
        markAsJunk(actionButtonPanel.getSpamButton().isSelected());
    }

    private void populateSearchResultTextPanel() {
        if (vsr.getDownloadState() == BasicDownloadState.NOT_STARTED) {
            searchResultTextPanel.remove(downloadingLink);
            
            searchResultTextPanel.add(subheadingLabel, "cell 0 1, wmin 350, wrap");
            searchResultTextPanel.add(metadataLabel, "cell 0 2, wmin 350");
        } else {
            searchResultTextPanel.remove(subheadingLabel);
            searchResultTextPanel.remove(metadataLabel);
            
            searchResultTextPanel.add(downloadingLink, "cell 0 1");
        }
    }

    /**
     * Returns a value to indicate whether the subheading was decorated to highlight
     * parts of the content that match search terms
     */
    private boolean populateSubheading(VisualSearchResult vsr) {
        String subheading = "";

        switch(getCategory()) {
        case AUDIO: {
            String albumTitle = vsr.getPropertyString(PropertyKey.ALBUM_TITLE);
            String quality = vsr.getPropertyString(PropertyKey.QUALITY);
            String length = vsr.getPropertyString(PropertyKey.LENGTH);
            
            
            boolean changed = false;
            if(!StringUtils.isEmpty(albumTitle)) {
                subheading += albumTitle;
                changed = true;
            }
            
            if(!StringUtils.isEmpty(quality)) {
                if(changed) {
                    subheading += " - ";
                }
                subheading += quality; 
                changed = true;
            }
            
            if(!StringUtils.isEmpty(length)) {
                if(changed) {
                    subheading += " - ";
                }
                subheading += length;
            }
        }
            break;
        case VIDEO: {
            String quality = vsr.getPropertyString(PropertyKey.QUALITY);
            String length = vsr.getPropertyString(PropertyKey.LENGTH);
            boolean changed = false;
            if(!StringUtils.isEmpty(quality)) {
                subheading += quality; 
                changed = true;
            }
            
            if(!StringUtils.isEmpty(length)) {
                if(changed) {
                    subheading += " - ";
                }
                subheading += length;
            }
        }
            break;
        case IMAGE: {
            Object time = vsr.getProperty(PropertyKey.DATE_CREATED);
            if(time != null) {
                subheading = DATE_FORMAT.format(new java.util.Date((Long)time));
            }
        }
            break;
        case PROGRAM: {
            String fileSize = vsr.getPropertyString(PropertyKey.FILE_SIZE);
            if(!StringUtils.isEmpty(fileSize)) {
                subheading = fileSize + tr("MB");
            }
        }
            break;
        case DOCUMENT:
        case OTHER:
        default: {
            subheading = "{application name}";
            String fileSize = vsr.getPropertyString(PropertyKey.FILE_SIZE);
            if(!StringUtils.isEmpty(fileSize)) {
                subheading = " - " + fileSize + tr("MB");
            }
        }
        }

        String highlightMatches = highlightMatches(subheading);
        LOG.debugf("Subheading: {0} highlightedMatches: {1}", subheading, highlightMatches);
        subheadingLabel.setText(highlightMatches);
        return isDifferentLength(subheading, highlightMatches);
    }

    private boolean isDifferentLength(String str1, String str2) {
        return str1 != null && str2 != null && str1.length() != str2.length();
    }

    private String getHideShowSimilarFilesButtonText() {
        int similarResultsCount = getSimilarResultsCount();
        if (isShowingSimilarResults()) {
            return hyperlinkText(trn("Hide 1 similar file", "Hide {0} similar files", similarResultsCount));
        }
        return hyperlinkText(trn("Show 1 similar file", "Show {0} similar files", similarResultsCount));
    }

    private Category getCategory() {
        return vsr.getCategory();
    }

    private static class PropertyMatch {
        private final String key;
        private final String highlightedValue;
        PropertyMatch(String key, String highlightedProperty) {
            this.key = key;
            this.highlightedValue = highlightedProperty;
        }
    }
}