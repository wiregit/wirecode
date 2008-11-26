package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.ui.swing.search.resultpanel.ListViewRowHeightRule.RowDisplayConfig.HeadingSubHeadingAndMetadata;
import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import java.util.regex.Pattern;

import javax.swing.AbstractCellEditor;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.downloads.MainDownloadPanel;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.search.RemoteHostActions;
import org.limewire.ui.swing.search.model.BasicDownloadState;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.ListViewRowHeightRule.PropertyMatch;
import org.limewire.ui.swing.search.resultpanel.ListViewRowHeightRule.RowDisplayConfig;
import org.limewire.ui.swing.search.resultpanel.ListViewRowHeightRule.RowDisplayResult;
import org.limewire.ui.swing.search.resultpanel.SearchResultTruncator.FontWidthResolver;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * This class is responsible for rendering an individual SearchResult
 * in "List View".
 */
public class ListViewTableEditorRenderer
extends AbstractCellEditor
implements TableCellEditor, TableCellRenderer {

    private static final int LEFT_COLUMN_WIDTH = 450;

    private final CategoryIconManager categoryIconManager;
    
    private static final String HTML = "<html>";
    private static final String CLOSING_HTML_TAG = "</html>";
    private final Log LOG = LogFactory.getLog(getClass());

    private final String searchText;
    @Resource private Icon similarResultsIcon;
    @Resource private Color subHeadingLabelColor;
    @Resource private Color metadataLabelColor;
    @Resource private Color downloadSourceCountColor;
    @Resource private Color similarResultsBackgroundColor;
    @Resource private Color surplusRowLimitColor;
    @Resource private Font headingFont;
    @Resource private Font subHeadingFont;
    @Resource private Font metadataFont;
    @Resource private Font downloadSourceCountFont;
    @Resource private Font surplusRowLimitFont;
    @Resource private Icon spamIcon;
    @Resource private Icon downloadingIcon;
    @Resource private Icon libraryIcon;
    @Resource private Icon optionsDownIcon;
    @Resource private Icon optionsHoverIcon;
    @Resource private Icon optionsUpIcon;
    @Resource private Icon similarUpIcon;
    @Resource private Icon similarDownIcon;
    @Resource private Icon similarActiveIcon;
    @Resource private Icon similarHoverIcon;
    @Resource private Icon dividerIcon;
    
    private final ActionColumnTableCellEditor actionEditor;
    private final SearchHeadingDocumentBuilder headingBuilder;
    private final ListViewRowHeightRule rowHeightRule;
    private final ListViewDisplayedRowsLimit displayLimit;
    private final SearchResultTruncator truncator;
    private final HeadingFontWidthResolver headingFontWidthResolver = new HeadingFontWidthResolver();
    private ActionButtonPanel actionButtonPanel;
    private SearchResultFromWidget fromWidget;
    private JLabel itemIconLabel;
    private JToggleButton similarButton = new JToggleButton();
    private JButton optionsButton = new JButton();
    private JEditorPane heading = new JEditorPane();
    private JLabel subheadingLabel = new JLabel();
    private JLabel metadataLabel = new JLabel();
    private JLabel downloadSourceCount = new JLabel();
    private JXPanel editorComponent;

    private VisualSearchResult vsr;
    private JTable table;
    private JComponent similarResultIndentation;
    private JPanel indentablePanel;
    private JPanel leftPanel;
    private JPanel fromPanel;
    private JPanel lastRowPanel;
    private final JPanel emptyPanel = new JPanel();
    private JXPanel searchResultTextPanel;

    private int currentColumn;
    private int currentRow;
    private int mousePressedRow = -1;
    private int mousePressedColumn = -1;

    private JLabel lastRowMessage;

    @AssistedInject
    ListViewTableEditorRenderer(
            CategoryIconManager categoryIconManager,
            SearchResultFromWidgetFactory fromWidgetFactory,
        @Assisted ActionColumnTableCellEditor actionEditor, 
        @Assisted String searchText, 
        final @Assisted RemoteHostActions remoteHostActions, 
        @Assisted Navigator navigator, 
        final @Assisted Color rowSelectionColor,
        final @Assisted DownloadHandler downloadHandler,
        SearchHeadingDocumentBuilder headingBuilder,
        ListViewRowHeightRule rowHeightRule,
        final PropertiesFactory<VisualSearchResult> properties,
        final @Assisted ListViewDisplayedRowsLimit displayLimit,
        LibraryNavigator libraryNavigator,
        SearchResultTruncator truncator) {

        this.categoryIconManager = categoryIconManager;
        
        this.actionEditor = actionEditor;
        this.searchText = searchText;
        this.headingBuilder = headingBuilder;
        this.rowHeightRule = rowHeightRule;
        this.displayLimit = displayLimit;
        this.truncator = truncator;
        GuiUtils.assignResources(this);

        similarButton.setPressedIcon(similarDownIcon);
        similarButton.setIcon(similarUpIcon);
        similarButton.setBorderPainted(false);
        similarButton.setContentAreaFilled(false);
        //This mouse listener is unfortunately necessary. Setting the icons for rollover
        //causes the wrong icon to be displayed after the button is active and the 
        //mouse has entered "edit" mode in the cell.
        similarButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                similarButton.setIcon(isShowingSimilarResults() ? similarActiveIcon : similarUpIcon);
            }

            @Override
            public void mouseEntered(MouseEvent arg0) {
                if (!isShowingSimilarResults()) {
                    similarButton.setIcon(similarHoverIcon);
                }
            }

            @Override
            public void mouseExited(MouseEvent arg0) { 
                if (!isShowingSimilarResults()) {
                    similarButton.setIcon(similarUpIcon);
                }
            }
        });

        optionsButton.setPressedIcon(optionsDownIcon);
        optionsButton.setRolloverIcon(optionsHoverIcon);
        optionsButton.setIcon(optionsUpIcon);
        optionsButton.setBorderPainted(false);
        optionsButton.setContentAreaFilled(false);
        
        fromWidget = fromWidgetFactory.create(remoteHostActions);
       
        makePanel(navigator, libraryNavigator, properties);
        
        editorComponent = new JXPanel(new BorderLayout()) {

            @Override
            public void setBackground(final Color bg) {
                //Don't highlight the limit row
                if (currentRow == displayLimit.getLastDisplayedRow()) {
                    super.setBackground(Color.WHITE);
                    return;
                }
                boolean editingColumn = mousePressedColumn == currentColumn;
                boolean editingRow = mousePressedRow == currentRow;
                boolean paintForCellSelection = editingColumn && editingRow;
                super.setBackground(paintForCellSelection ? rowSelectionColor : bg);
                if (paintForCellSelection) {
                    mousePressedColumn = -1;
                    mousePressedRow = -1;
                }
            }
        };
        
        itemIconLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if(e.getButton() == MouseEvent.BUTTON1) {
                    actionButtonPanel.startDownload();
                    table.editingStopped(new ChangeEvent(table));
                } else if(e.getButton() == MouseEvent.BUTTON3) {
                    SearchResultMenu searchResultMenu = new SearchResultMenu(downloadHandler, vsr, currentRow, remoteHostActions, properties);
                    searchResultMenu.show(itemIconLabel, e.getX(), e.getY());
                } 
            }
        });
        
        heading.addMouseListener( new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if(e.getButton() == MouseEvent.BUTTON3) {
                    SearchResultMenu searchResultMenu = new SearchResultMenu(downloadHandler, vsr, currentRow, remoteHostActions, properties);
                    searchResultMenu.show(heading, e.getX(), e.getY());
                }
            }
        });

    }

    public Object getCellEditorValue() {
        return vsr;
    }

    private int getSimilarResultsCount() {
        return vsr == null ? 0 : vsr.getSimilarResults().size();
    }
    
    @Override
    public boolean isCellEditable(EventObject e) {
        if (e instanceof MouseEvent) {
            MouseEvent event = (MouseEvent) e;
            if (event.getID() == MouseEvent.MOUSE_PRESSED) {
                //Cache the cell that's just been clicked on so that the editor component
                //can draw the correct selection background color 
                mousePressedRow = table.rowAtPoint(event.getPoint());
                mousePressedColumn = table.columnAtPoint(event.getPoint());
            }
        }
        return super.isCellEditable(e);
    }

    public Component getTableCellEditorComponent(
        final JTable table, Object value, boolean isSelected, int row, final int col) {
        
        vsr = (VisualSearchResult) value;
        this.table = table;
        LOG.debugf("getTableCellEditorComponent: row = {0} column = {1}", row, col);

        currentColumn = col;
        currentRow = row;
        
        if (value == null) {
            editorComponent.removeAll();
            editorComponent.add(emptyPanel, "height 100%");
            return editorComponent;
        }
        
        actionButtonPanel =
            (ActionButtonPanel) actionEditor.getTableCellEditorComponent(
                    table, value, isSelected, row, col);

        LOG.debugf("row: {0} shouldIndent: {1}", row, vsr.getSimilarityParent() != null);
        
        editorComponent.removeAll();
        JPanel panel = null;
        if (row == displayLimit.getLastDisplayedRow()) {
            lastRowMessage.setText(tr("Not showing {0} results", 
                    (displayLimit.getTotalResultsReturned() - displayLimit.getLastDisplayedRow())));
            panel = col == 0 ? lastRowPanel : emptyPanel;
        } else {
            populatePanel((VisualSearchResult) value, col);
            panel = col == 0 ? leftPanel : fromPanel;
            
            if (col == 1) {
                similarButton.setIcon(isShowingSimilarResults() ? similarActiveIcon : similarUpIcon);
            }
        }

        editorComponent.add(panel, BorderLayout.CENTER);

        return editorComponent;
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

    private JPanel makeFromPanel(final PropertiesFactory<VisualSearchResult> properties) {
        similarButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                boolean toggleVisibility = !isShowingSimilarResults();
                vsr.setChildrenVisible(toggleVisibility);
            }
        });
        
        optionsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                properties.newProperties().showProperties(vsr);
            }
        });

        JXPanel panel = new JXPanel(new MigLayout("", "0[][][]8[]", "5[]")) {
            @Override
            public void setBackground(Color color) {
                super.setBackground(color);
                for (Component component : getComponents()) {
                    component.setBackground(color);
                }
            }
        };

        panel.setOpaque(false);
        panel.add(new JLabel(dividerIcon));
        panel.add(fromWidget, "push");
        //Tweaked the width of the icon because display gets clipped otherwise
        panel.add(similarButton, "hmax 25, wmax 27");
        panel.add(optionsButton, "hmax 25, wmax 27");

        return panel;
    }

    private Component makeLeftPanel(final Navigator navigator, final LibraryNavigator libraryNavigator) {
        itemIconLabel = new JLabel();
        itemIconLabel.setCursor(
            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        itemIconLabel.setOpaque(false);
        
        heading.setContentType("text/html");
        heading.setEditable(false);
        heading.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        heading.setOpaque(false);
        
        heading.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseEntered(MouseEvent e) {
                populateHeading(rowHeightRule.getDisplayResult(vsr, searchText), vsr.getDownloadState(), true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                populateHeading(rowHeightRule.getDisplayResult(vsr, searchText), vsr.getDownloadState(), false);
            }
        });

        subheadingLabel.setForeground(subHeadingLabelColor);
        subheadingLabel.setFont(subHeadingFont);
        
        metadataLabel.setForeground(metadataLabelColor);
        metadataLabel.setFont(metadataFont);
        
        downloadSourceCount.setForeground(downloadSourceCountColor);
        downloadSourceCount.setFont(downloadSourceCountFont);
        
        JXPanel itemIconPanel = new JXPanel(new MigLayout("insets 7 0 0 5", "0[]", "0[]0"));
        itemIconPanel.setOpaque(false);
        itemIconPanel.add(itemIconLabel);

        searchResultTextPanel = new JXPanel(new MigLayout("fill, insets 0 0 0 0", "3[fill]", "[]0[]0[]"));
        searchResultTextPanel.setOpaque(false);
        searchResultTextPanel.add(heading, "wrap, growx");
        searchResultTextPanel.add(subheadingLabel, "wrap");
        searchResultTextPanel.add(metadataLabel);

        JXPanel panel = new JXPanel(new MigLayout("fill, insets 0 0 0 0", "5[][fill][]5", "0[]0"));
        panel.setOpaque(false);

        panel.add(itemIconPanel);
        panel.add(searchResultTextPanel, "push");
        panel.add(downloadSourceCount, "gapbottom 3");
        
        heading.addHyperlinkListener(new HyperlinkListener() {

            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (EventType.ACTIVATED == e.getEventType()) {
                    if (e.getDescription().equals("#download")) {
                        actionButtonPanel.startDownload();
                        table.editingStopped(new ChangeEvent(table));
                    } else if (e.getDescription().equals("#downloading")) {
                        navigator.getNavItem(
                            NavCategory.DOWNLOAD,
                            MainDownloadPanel.NAME).select(vsr);
                    } else if (e.getDescription().equals("#library")) {
                        libraryNavigator.selectInLibrary(vsr.getUrn(), vsr.getCategory());
                    }
                }
            }
        });

        return panel;
    }

    private void makePanel(Navigator navigator, LibraryNavigator libraryNavigator, PropertiesFactory<VisualSearchResult> properties) {
        leftPanel = makeIndentablePanel(makeLeftPanel(navigator, libraryNavigator));

        fromPanel = makeFromPanel(properties);

        lastRowPanel = new JPanel(new MigLayout("insets 10 30 0 0", "[]", "[]"));
        lastRowPanel.setOpaque(false);
        lastRowMessage = new JLabel();
        lastRowMessage.setFont(surplusRowLimitFont);
        lastRowMessage.setForeground(surplusRowLimitColor);
        lastRowPanel.add(lastRowMessage);
        emptyPanel.setOpaque(false);
    }

    private JPanel makeIndentablePanel(Component component) {
        indentablePanel = new JPanel(new BorderLayout());
        indentablePanel.setOpaque(false);
        similarResultIndentation = new JPanel(new BorderLayout());
        similarResultIndentation.add(new JLabel(similarResultsIcon), BorderLayout.CENTER);
        indentablePanel.add(component, BorderLayout.CENTER);
        return indentablePanel;
    }

    private void populateFrom(VisualSearchResult vsr) {
        fromWidget.setPeople(vsr.getSources());
    }

    private void populateHeading(final RowDisplayResult result, BasicDownloadState downloadState, boolean isMouseOver) {
        int width = heading.getVisibleRect().width;
        //Width is zero the first time editorpane is rendered - use a wide default (roughly width of left column)
        width = width == 0 ? LEFT_COLUMN_WIDTH : width;
        //Make the visible rect seem a little smaller than usual to trigger a more hungry truncation
        //otherwise the JEditorPane word-wrapping logic kicks in and the edge word just disappears
        final int fudgeFactorPixelWidth = width - 10;
        SearchHeading searchHeading = new SearchHeading() {
            @Override
            public String getText() {
                String headingText = result.getHeading();
                String truncatedHeading = truncator.truncateHeading(headingText, fudgeFactorPixelWidth, headingFontWidthResolver);
                handleHeadingTooltip(headingText, truncatedHeading);
                return truncatedHeading;
            }

            /**
             * Sets a tooltip for the heading field only if the text has been truncated. 
             */
            private void handleHeadingTooltip(String headingText, String truncatedHeading) {
                heading.setToolTipText(headingText.equals(truncatedHeading) ? null : HTML + headingText + CLOSING_HTML_TAG);
            }

            @Override
            public String getText(String adjoiningFragment) {
                int adjoiningTextPixelWidth = headingFontWidthResolver.getPixelWidth(adjoiningFragment);
                String headingText = result.getHeading();
                String truncatedHeading = truncator.truncateHeading(headingText, 
                        fudgeFactorPixelWidth - adjoiningTextPixelWidth, headingFontWidthResolver);
                handleHeadingTooltip(headingText, truncatedHeading);
                return truncatedHeading;
            }
        };
        this.heading.setText(headingBuilder.getHeadingDocument(searchHeading, downloadState, isMouseOver, result.isSpam()));
        this.downloadSourceCount.setText(Integer.toString(vsr.getSources().size()));
    }
    
    private class HeadingFontWidthResolver implements FontWidthResolver {
        private static final String EMPTY_STRING = "";
        //finds <b>foo</b> or <a href="#foo"> or {1} patterns
        //**NOTE** This does not account for all HTML sanitizing, just for HTML
        //**NOTE** that would have been added by search result display code
        private final Pattern findHTMLTagsOrReplacementTokens = Pattern.compile("([<][/]?[\\w =\"#]*[>])|([{][\\d]*[}])");
        
        @Override
        public int getPixelWidth(String text) {
            HTMLEditorKit editorKit = (HTMLEditorKit) heading.getEditorKit();
            StyleSheet css = editorKit.getStyleSheet();
            FontMetrics fontMetrics = css.getFontMetrics(headingFont);
            text = findHTMLTagsOrReplacementTokens.matcher(text).replaceAll(EMPTY_STRING);
            return fontMetrics.stringWidth(text);
        }
    }
    
    private void populateOther(RowDisplayResult result) {
        metadataLabel.setText("");
        RowDisplayConfig config = result.getConfig();
        if (config != HeadingSubHeadingAndMetadata && config != RowDisplayConfig.HeadingAndMetadata) { 
            return;
        }
        
        PropertyMatch pm = result.getMetadata();
        
        if (pm != null) {
            String html = pm.getHighlightedValue();
            // Insert the following: the key, a colon and a space after the html start tag, then the closing tags.
            html = html.replace(HTML, "").replace(CLOSING_HTML_TAG, "");
            html = HTML + pm.getKey() + ":" + html + CLOSING_HTML_TAG;
            metadataLabel.setText(html);
        }
    }

    private void populatePanel(VisualSearchResult vsr, int column) {
        if (vsr == null) return;
        
        if (column == 0) {
            if (vsr.getSimilarityParent() != null) {
                indentablePanel.add(similarResultIndentation, BorderLayout.WEST);
                similarResultIndentation.setBackground(similarResultsBackgroundColor);
            } else {
                indentablePanel.remove(similarResultIndentation);
            }
            
            itemIconLabel.setIcon(getIcon(vsr));

            RowDisplayResult result = rowHeightRule.getDisplayResult(vsr, searchText);
            
            populateHeading(result, vsr.getDownloadState(), false);
            
            populateSearchResultTextPanel(result.getConfig());

            populateSubheading(result);
            populateOther(result);
            
            
        } else if (column == 1) {
            similarButton.setVisible(getSimilarResultsCount() > 0);
            populateFrom(vsr);
        }
    }

    private Icon getIcon(VisualSearchResult vsr) {
        if (vsr.isSpam()) {
            return spamIcon;
        } else if (vsr.getDownloadState() == BasicDownloadState.DOWNLOADING) {
            return downloadingIcon;
        } else if (vsr.getDownloadState() == BasicDownloadState.LIBRARY) {
            return libraryIcon;
        }
        return categoryIconManager.getIcon(vsr.getCategory());
    }

    private void populateSearchResultTextPanel(RowDisplayConfig config) {
        switch(config) {
        case HeadingOnly:
            searchResultTextPanel.remove(subheadingLabel);
            searchResultTextPanel.remove(metadataLabel);
            break;
        case HeadingAndSubheading:
            searchResultTextPanel.add(subheadingLabel, "cell 0 1");
            searchResultTextPanel.remove(metadataLabel);
            break;
        case HeadingAndMetadata:
            searchResultTextPanel.remove(subheadingLabel);
            searchResultTextPanel.add(metadataLabel, "cell 0 1");
            break;
        case HeadingSubHeadingAndMetadata:
            searchResultTextPanel.add(subheadingLabel, "cell 0 1, wrap");
            searchResultTextPanel.add(metadataLabel, "cell 0 2");
        }
    }

    /**
     * Returns a value to indicate whether the subheading was decorated to highlight
     * parts of the content that match search terms
     */
    private void populateSubheading(RowDisplayResult result) {
        subheadingLabel.setText(result.getSubheading());
    }
}