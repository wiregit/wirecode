package org.limewire.ui.swing.search.resultpanel.list;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.StyleSheet;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.ui.swing.components.HTMLLabel;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.search.model.VisualStoreResult;
import org.limewire.ui.swing.search.resultpanel.ListViewTable;
import org.limewire.ui.swing.search.resultpanel.list.ListViewRowHeightRule.RowDisplayConfig;
import org.limewire.ui.swing.search.resultpanel.list.ListViewRowHeightRule.RowDisplayResult;
import org.limewire.ui.swing.search.resultpanel.list.ListViewTableEditorRenderer.NoDancingHtmlLabel;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

/**
 * Renderer component for store results.
 */
class ListViewStoreRenderer extends JXPanel {

    private final CategoryIconManager categoryIconManager;
    private final StoreStyle storeStyle;
    
    private JButton itemIconButton = new IconButton();
    private JXPanel searchResultTextPanel = new JXPanel();
    private JEditorPane headingPane = new JEditorPane();
    private JLabel subheadingLabel = new NoDancingHtmlLabel();
    private JLabel metadataLabel = new NoDancingHtmlLabel();
    
    private JButton trackIconButton = new IconButton();

    private JXPanel trackResultPanel = new JXPanel();
    
    private JTable table;
    private VisualStoreResult vsr;

    /**
     * Constructs a ListViewStoreRenderer.
     */
    @Inject
    public ListViewStoreRenderer(
            CategoryIconManager categoryIconManager,
            StoreStyle storeStyle) {
        this.categoryIconManager = categoryIconManager;
        this.storeStyle = storeStyle;
        
        initComponents();
        layoutComponents();
    }

    private void initComponents() {
        itemIconButton.setBorderPainted(true);
        itemIconButton.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        
        searchResultTextPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        
        Color artistForeground = storeStyle.getArtistForeground();
        String headingColor = "#2152a6"; // TODO convert from artistForeground
        
        headingPane.setContentType("text/html");
        headingPane.setEditable(false);
        headingPane.setCaretPosition(0);
        headingPane.setSelectionColor(HTMLLabel.TRANSPARENT_COLOR);       
        headingPane.setOpaque(false);
        headingPane.setFocusable(false);
        StyleSheet mainStyle = ((HTMLDocument)headingPane.getDocument()).getStyleSheet();
        String rules = "body { font-family: " + storeStyle.getArtistFont().getFamily() + "; }" +
            ".title { color: " + headingColor + "; font-size: " + storeStyle.getArtistFont().getSize() + "; }" +
            "a { color: " + headingColor + "; }";
        StyleSheet newStyle = new StyleSheet();
        newStyle.addRule(rules);
        mainStyle.addStyleSheet(newStyle); 
        headingPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));

        subheadingLabel.setForeground(storeStyle.getQualityForeground());
        subheadingLabel.setFont(storeStyle.getQualityFont());

        metadataLabel.setForeground(storeStyle.getQualityForeground());
        metadataLabel.setFont(storeStyle.getQualityFont());
        
        trackIconButton.setBorderPainted(true);
        trackIconButton.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        trackIconButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (vsr != null) {
                    vsr.setShowTracks(!vsr.isShowTracks());
                    trackIconButton.setText(vsr.isShowTracks() ? I18n.tr("Hide Tracks") : I18n.tr("Show Tracks"));
                    if (table instanceof ListViewTable) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                ((ListViewTable) table).updateRowSizes();
                            }
                        });
                    }
                }
            }
        });
        
        trackResultPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
    }
    
    private void layoutComponents() {
        setLayout(new MigLayout("insets 6 6 0 6, gap 0! 0!, novisualpadding"));
        
        searchResultTextPanel.setLayout(new MigLayout("nogrid, ins 0 0 0 0, gap 0! 0!, novisualpadding"));
        searchResultTextPanel.setOpaque(false);
        
        searchResultTextPanel.add(headingPane, "left, shrinkprio 200, growx, wmax pref, hidemode 3, wrap");
        searchResultTextPanel.add(subheadingLabel, "left, shrinkprio 200, growx, hidemode 3, wrap");
        searchResultTextPanel.add(metadataLabel, "left, shrinkprio 200, hidemode 3");
        
        trackResultPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0! 0!, fill, novisualpadding"));

        //add(similarResultIndentation, "growy, hidemode 3, shrinkprio 0");
        add(itemIconButton, "alignx left, aligny 50%, shrinkprio 0, growprio 0");
        add(searchResultTextPanel, "alignx left, aligny 50%, gapleft 4, growx, shrinkprio 200, growprio 200, pushx 200");
        //add(downloadSourceCount, "gapbottom 3, gapright 2, shrinkprio 0");
        //add(new JLabel(dividerIcon), "shrinkprio 0");
        //TODO: better number for wmin
        //add(fromWidget, "wmin 90, left, shrinkprio 0");
        //Tweaked the width of the icon because display gets clipped otherwise
        //add(similarButton, "gapright 4, hidemode 0, hmax 25, wmax 27, shrinkprio 0");
        //add(propertiesButton, "gapright 4, hmax 25, wmax 27, shrinkprio 0");
        add(trackIconButton, "aligny top, gapleft 4, shrinkprio 0, growprio 0, wrap");
        
        add(trackResultPanel, "span 3, left, aligny top, gap 16 16 6 0, grow");
    }

    public void update(JTable table, VisualStoreResult vsr, RowDisplayResult result) {
        System.out.println("storeRenderer update..."); // TODO
        
        this.table = table;
        this.vsr = vsr;
        
        if (vsr.getStoreResult().isAlbum()) {
            itemIconButton.setIcon(vsr.getStoreResult().getAlbumIcon());
        } else {
            itemIconButton.setIcon(categoryIconManager.getIcon(vsr));
        }
        
        trackIconButton.setText(vsr.isShowTracks() ? I18n.tr("Hide Tracks") : I18n.tr("Show Tracks"));
        
        updateLabelVisibility(result.getConfig());
        updateHeading(result);
        updateSubHeading(result);
        updateMetaData(result);
        updateTracks(vsr);
    }
    
    private void updateLabelVisibility(RowDisplayConfig config) {
        switch(config) {
        case HeadingOnly:
            subheadingLabel.setVisible(false);
            metadataLabel.setVisible(false);
            break;
        case HeadingAndSubheading:
            subheadingLabel.setVisible(true);
            metadataLabel.setVisible(false);
            break;
        case HeadingAndMetadata:
            subheadingLabel.setVisible(false);
            metadataLabel.setVisible(true);
            break;
        case HeadingSubHeadingAndMetadata:
        default:
            subheadingLabel.setVisible(true);
            metadataLabel.setVisible(true);
            break;
        }
    }
    
    private void updateHeading(RowDisplayResult result) {
        headingPane.setText(result.getHeading());
    }
    
    private void updateSubHeading(RowDisplayResult result) {
        subheadingLabel.setText(result.getSubheading());
    }
    
    private void updateMetaData(RowDisplayResult result) {
        
    }
    
    /**
     * Adds display panels when multiple tracks are available.
     */
    private void updateTracks(VisualStoreResult vsr) {
        trackResultPanel.removeAll();
        
        if (vsr.getStoreResult().getAlbumResults().size() > 1) {
            trackIconButton.setVisible(true);
            if (vsr.isShowTracks()) {
                trackResultPanel.setVisible(true);
                List<SearchResult> fileList = vsr.getStoreResult().getAlbumResults();
                for (SearchResult result : fileList) {
                    Component comp = createTrackComponent(result);
                    trackResultPanel.add(comp, "align left, growx, wrap");
                }
            } else {
                trackResultPanel.setVisible(false);
            }
            
        } else {
            trackIconButton.setVisible(false);
            trackResultPanel.setVisible(false);
        }
    }
    
    private Component createTrackComponent(SearchResult result) {
        JLabel label = new JLabel();
        
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)));
        label.setForeground(storeStyle.getTrackForeground());
        label.setFont(storeStyle.getTrackFont());
        label.setText((String) result.getProperty(FilePropertyKey.NAME));
        System.out.println("prefSize=" + label.getPreferredSize()); // TODO
        
        return label;
    }
}
