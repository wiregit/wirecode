package org.limewire.ui.swing.search.resultpanel.list;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.ui.swing.components.HTMLLabel;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.search.model.VisualStoreResult;
import org.limewire.ui.swing.search.resultpanel.list.ListViewRowHeightRule.RowDisplayResult;
import org.limewire.ui.swing.search.resultpanel.list.ListViewTableEditorRenderer.NoDancingHtmlLabel;
import org.limewire.ui.swing.util.CategoryIconManager;

/**
 * Implementation of ListViewStoreRenderer for style A.
 */
class ListViewStoreRendererCD extends ListViewStoreRenderer {

    private JButton albumIconButton;
    private JXPanel albumTextPanel;
    private HTMLLabel albumHeadingLabel;
    private JLabel albumSubHeadingLabel;
    private JButton showTracksButton;
    
    private JButton mediaIconButton;
    private JXPanel mediaTextPanel;
    private HTMLLabel mediaHeadingLabel;
    private JLabel mediaSubHeadingLabel;
    
    /**
     * Constructs a store renderer with the specified icon manager and store
     * style.
     */
    public ListViewStoreRendererCD(CategoryIconManager categoryIconManager, StoreStyle storeStyle) {
        super(categoryIconManager, storeStyle);
    }

    @Override
    protected void initAlbumComponent() {
        albumPanel.setOpaque(false);
        
        albumIconButton = new IconButton();
        albumIconButton.setBorderPainted(true);
        albumIconButton.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        
        albumTextPanel = new JXPanel();
        albumTextPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        albumTextPanel.setOpaque(false);
        
        albumHeadingLabel = new HTMLLabel();
        albumHeadingLabel.setOpenUrlsNatively(false);
        albumHeadingLabel.setOpaque(false);
        albumHeadingLabel.setFocusable(false);
        albumHeadingLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
//        StyleSheet mainStyle = ((HTMLDocument) albumHeadingLabel.getDocument()).getStyleSheet();
//        String rules = "body { font-family: " + storeStyle.getArtistFont().getFamily() + "; }" +
//            ".title { color: " + headingColor + "; font-size: " + storeStyle.getArtistFont().getSize() + "; }" +
//            "a { color: " + headingColor + "; }";
//        StyleSheet newStyle = new StyleSheet();
//        newStyle.addRule(rules);
//        mainStyle.addStyleSheet(newStyle);
        albumHeadingLabel.setHtmlFont(storeStyle.getArtistFont());
        albumHeadingLabel.setHtmlForeground(storeStyle.getArtistForeground());
        albumHeadingLabel.setHtmlLinkForeground(storeStyle.getArtistForeground());
        
        albumSubHeadingLabel = new NoDancingHtmlLabel();
        albumSubHeadingLabel.setFont(storeStyle.getAlbumFont());
        albumSubHeadingLabel.setForeground(storeStyle.getAlbumForeground());
        
        showTracksButton = new IconButton();
        showTracksButton.setBorderPainted(true);
        showTracksButton.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        showTracksButton.setHideActionText(false);
        showTracksButton.setAction(showTracksAction);
        
        albumTextPanel.setLayout(new MigLayout("nogrid, ins 0 0 0 0, gap 0! 0!, novisualpadding"));
        albumTextPanel.add(albumHeadingLabel, "left, shrinkprio 200, growx, wmax pref, hidemode 3, wrap");
        albumTextPanel.add(albumSubHeadingLabel, "left, shrinkprio 200, growx, hidemode 3");
        
        albumPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0! 0!, novisualpadding"));
        albumPanel.add(albumIconButton, "alignx left, aligny 50%, shrinkprio 0, growprio 0");
        albumPanel.add(albumTextPanel, "alignx left, aligny 50%, gapleft 4, growx, shrinkprio 200, growprio 200, pushx 200");
        albumPanel.add(showTracksButton, "aligny top, gapleft 4, shrinkprio 0, growprio 0");
    }

    @Override
    protected void initMediaComponent() {
        mediaPanel.setOpaque(false);
        
        mediaIconButton = new IconButton();
        mediaIconButton.setBorderPainted(true);
        mediaIconButton.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        
        mediaTextPanel = new JXPanel();
        mediaTextPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        mediaTextPanel.setOpaque(false);
        
        mediaHeadingLabel = new HTMLLabel();
        mediaHeadingLabel.setOpenUrlsNatively(false);
        mediaHeadingLabel.setOpaque(false);
        mediaHeadingLabel.setFocusable(false);
        mediaHeadingLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
//        StyleSheet mainStyle = ((HTMLDocument) mediaHeadingLabel.getDocument()).getStyleSheet();
//        String rules = "body { font-family: " + storeStyle.getArtistFont().getFamily() + "; }" +
//            ".title { color: " + headingColor + "; font-size: " + storeStyle.getArtistFont().getSize() + "; }" +
//            "a { color: " + headingColor + "; }";
//        StyleSheet newStyle = new StyleSheet();
//        newStyle.addRule(rules);
//        mainStyle.addStyleSheet(newStyle);
        mediaHeadingLabel.setHtmlFont(storeStyle.getArtistFont());
        mediaHeadingLabel.setHtmlForeground(storeStyle.getArtistForeground());
        mediaHeadingLabel.setHtmlLinkForeground(storeStyle.getArtistForeground());
        
        mediaSubHeadingLabel = new NoDancingHtmlLabel();
        mediaSubHeadingLabel.setFont(storeStyle.getAlbumFont());
        mediaSubHeadingLabel.setForeground(storeStyle.getAlbumForeground());
        
        mediaTextPanel.setLayout(new MigLayout("nogrid, ins 0 0 0 0, gap 0! 0!, novisualpadding"));
        mediaTextPanel.add(mediaHeadingLabel, "left, shrinkprio 200, growx, wmax pref, hidemode 3, wrap");
        mediaTextPanel.add(mediaSubHeadingLabel, "left, shrinkprio 200, growx, hidemode 3");
        
        mediaPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0! 0!, novisualpadding"));
        mediaPanel.add(mediaIconButton, "alignx left, aligny 50%, shrinkprio 0, growprio 0");
        mediaPanel.add(mediaTextPanel, "alignx left, aligny 50%, gapleft 4, growx, shrinkprio 200, growprio 200, pushx 200");
    }

    @Override
    protected Component createTrackComponent(SearchResult result) {
        JXPanel trackPanel = new JXPanel();
        trackPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)));
        
        // TODO create proper implementation with buttons
        
        JLabel label = new JLabel();
        label.setFont(storeStyle.getTrackFont());
        label.setForeground(storeStyle.getTrackForeground());
        label.setText((String) result.getProperty(FilePropertyKey.NAME));
        
        trackPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0! 0!, novisualpadding"));
        trackPanel.add(label, "alignx left, growx");
        System.out.println("prefSize=" + trackPanel.getPreferredSize()); // TODO REMOVE
        
        return trackPanel;
    }

    @Override
    protected void updateAlbum(VisualStoreResult vsr, RowDisplayResult result, boolean editing) {
        albumIconButton.setIcon(vsr.getStoreResult().getAlbumIcon());
        
        switch (result.getConfig()) {
        case HeadingOnly:
            albumSubHeadingLabel.setVisible(false);
            break;
        case HeadingAndSubheading:
            albumSubHeadingLabel.setVisible(true);
            break;
        case HeadingAndMetadata:
            albumSubHeadingLabel.setVisible(false);
            break;
        case HeadingSubHeadingAndMetadata:
        default:
            albumSubHeadingLabel.setVisible(true);
            break;
        }
        
        albumHeadingLabel.setText(result.getHeading());
        albumSubHeadingLabel.setText(result.getSubheading());
        
        showTracksButton.setVisible(vsr.getStoreResult().isAlbum());
    }

    @Override
    protected void updateMedia(VisualStoreResult vsr, RowDisplayResult result, boolean editing) {
        mediaIconButton.setIcon(categoryIconManager.getIcon(vsr));
        
        switch (result.getConfig()) {
        case HeadingOnly:
            mediaSubHeadingLabel.setVisible(false);
            break;
        case HeadingAndSubheading:
            mediaSubHeadingLabel.setVisible(true);
            break;
        case HeadingAndMetadata:
            mediaSubHeadingLabel.setVisible(false);
            break;
        case HeadingSubHeadingAndMetadata:
        default:
            mediaSubHeadingLabel.setVisible(true);
            break;
        }
        
        mediaHeadingLabel.setText(result.getHeading());
        mediaSubHeadingLabel.setText(result.getSubheading());
    }
}
