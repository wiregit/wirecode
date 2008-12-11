package org.limewire.ui.swing.properties;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.JTextComponent;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXHyperlink;
import org.jdesktop.swingx.JXLabel;
import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.components.Line;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.util.StringUtils;

public abstract class Dialog extends LimeJDialog {
    private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("M/d/yyyy");
    private static final String AUTHOR = "Author";
    private static final String COMPANY = "Company";
    private static final String PLATFORM = "Platform";
    private static final String TRACK = "Track";
    private static final String ALBUM = "Album";
    private static final String ARTIST = "Artist";
    private static final String DESCRIPTION = "Description";
    private static final String YEAR = "Year";
    private static final String RATING = "Rating";
    private static final String GENRE = "Genre";
    private static final String TITLE = "Title";
    private static final String HASH = "Hash";
    protected final JLabel icon = new JLabel();
    protected final JLabel heading = newLabel();
    protected final JLabel filename = newLabel();
    protected final JLabel fileSize = new JLabel();
    protected final JLabel metadata = newLabel();
    protected final JXHyperlink copyToClipboard = new JXHyperlink();
    protected final JTextField title = new JTextField();
    protected final JComboBox genre = new JComboBox();
    protected final JComboBox rating = new JComboBox();
    protected final JTextField year = new JTextField();
    protected final JTextArea description = newTextArea();
    protected final JTextField artist = new JTextField();
    protected final JTextField album = new JTextField();
    protected final JTextField track = new JTextField();
    protected final JTextField author = new JTextField();
    protected final JComboBox platform = new JComboBox();
    protected final JTextField company = new JTextField();
    protected final DefaultTableModel readOnlyInfoModel = new ReadOnlyTableModel();
    protected final JLabel fileLocation = newLabel();
    protected final JXHyperlink locateOnDisk = new JXHyperlink();
    protected final JXHyperlink locateInLibrary = new JXHyperlink();
    protected final JTable readOnlyInfo = new JTable(readOnlyInfoModel);
    
    protected final JPanel overview;
    protected final JPanel details = newPanel();
    protected final JPanel location = newPanel();
    protected Component detailsContainer;
    protected final JPanel mainPanel;
    
    public Dialog() {
        GuiUtils.assignResources(this);
        
        mainPanel = new JPanel(new MigLayout("insets 0 3 3 0", "[fill]push[]", "[][][][]push[]"));
        
        add(mainPanel);
        mainPanel.setBackground(Color.LIGHT_GRAY);
        
        setFont(getMediumFont(), heading, filename, fileSize);
        setFont(getSmallFont(), metadata, copyToClipboard, locateOnDisk, locateInLibrary,
                title, genre, rating, year, description, artist, album, track, author, platform,
                company, fileLocation);
        
        JPanel buttons = newPanel(new MigLayout("", "[][]", "[]"));
        buttons.add(new JButton(new OKAction()));
        buttons.add(new JButton(new CancelAction()));
        mainPanel.add(buttons, "cell 1 4");
        
        overview = newPanel(new MigLayout("fillx", "[][]push[]", "[][][]"));

        overview.add(icon);
        overview.add(heading);
        overview.add(copyToClipboard, "wrap");
        overview.add(metadata, "cell 1 2");

        addOverview();
    }
    
    protected JPanel newPanel(LayoutManager manager) {
        JPanel panel = newPanel();
        panel.setLayout(manager);
        return panel;
    }

    protected JPanel newPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        return panel;
    }
    
    protected abstract Font getSmallFont();
    protected abstract Font getMediumFont();
    protected abstract Font getLargeFont();

    protected void setFont(Font font, JComponent... components) {
        for(JComponent comp : components) {
            comp.setFont(font);
        }
    }

    private JLabel newLabel() {
        JXLabel label = new JXLabel();
        label.setLineWrap(true);
        return label;
    }

    private JTextArea newTextArea() {
        JTextArea area = new JTextArea();
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        return area;
    }

    protected void showDialog(String fileName, Category category) {
        setTitle(tr("\"{0}\" properties", fileName));
        
        switch(category) {
        case VIDEO:
            configureVideoDetailsLayout();
            break;
        case AUDIO:
            configureAudioDetailsLayout();
            break;
        case DOCUMENT:
            configureDocumentDetailsLayout();
            break;
        case IMAGE:
            configureImageDetailsLayout();
            break;
        case PROGRAM:
            configureProgramDetailsLayout();
            break;
        case OTHER:
            //no-op: Does not display details
        }
        
        addLocation();
        
        setPreferredSize(new Dimension(620,650));
        setModalityType(ModalityType.APPLICATION_MODAL);
        
        setDefaultCloseOperation(2);

        pack();

        setLocationRelativeTo(GuiUtils.getMainFrame());
        
        setVisible(true);
    }

    private void addOverview() {
        mainPanel.add(box("Overview", overview), "cell 0 0, spanx 2");
    }

    protected Component box(String string, JComponent component) {
        return box(string, null, component);
    }

    protected Component box(String string, JComponent bannerComponent, JComponent bodyComponent) {
        
        JPanel panel = new JPanel(new MigLayout("insets 3 3 3 3, fillx", "[fill]push[]", "[][][]"));
        panel.setOpaque(false);
        JLabel label = new JLabel(tr(string));
        label.setFont(getLargeFont());
        panel.add(label, bannerComponent == null ? "wrap" : "");
        if (bannerComponent != null) {
            panel.add(bannerComponent, "wrap");
        }
        panel.add(Line.createHorizontalLine(), "span, wrap");
        panel.add(bodyComponent, "span");
        return panel;
    }

    private void addDetails() {
        detailsContainer = box("Details", details);
        mainPanel.add(detailsContainer, "cell 0 1, spanx 2");
    }

    private void addLocation() {
        mainPanel.add(box("Location", location), "cell 0 2, spanx 2");
    }

    private class OKAction extends AbstractAction {
        public OKAction() {
            super(tr("OK"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setVisible(false);
            commit();
        }
    }

    private class CancelAction extends AbstractAction {
        public CancelAction() {
            super(tr("Cancel"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setVisible(false);
        }
    }

    protected abstract void commit();

    protected void addHashMetadata(URN hash, Map<String, String> metadata) {
        addMetadata(HASH, (hash != null) ? hash.toString() : null, metadata);
    }

    protected void addDateCreatedMetadata(String dateCreated, Map<String, String> metadata) {
        addMetadata("Date Created", dateCreated, metadata);
    }

    protected void addBitrateMetadata(String bitRate, Map<String, String> metadata) {
        addMetadata("Bitrate", bitRate, metadata);
    }

    protected void addDimensionMetadata(String width, Object height, Map<String, String> metadata) {
        if (height != null && width != null) {
            addMetadata("Dimensions", tr("({0}px X {1}px)", width, height), metadata);
        }
    }

    private void addMetadata(String label, String value, Map<String, String> metadata) {
        if (!StringUtils.isEmpty(value)) {
            metadata.put(tr(label), value);
        }
    }

    protected void setMetadataText(Map<String, String> metadata) {
        StringBuilder bldr = new StringBuilder().append("<html>");
        for(String key : metadata.keySet()) {
            bldr.append("<b>").append(key).append("</b>:&nbsp;").append(metadata.get(key)).append("<br/>");
        }
        bldr.append("</html>");
        this.metadata.setText(bldr.toString());
    }
    
    private JLabel newSmallLabel(String text) {
        JLabel label = new JLabel(tr(text));
        label.setFont(getSmallFont());
        return label;
    }

    private void configureVideoDetailsLayout() {
        details.setLayout(new MigLayout("fillx, nocache", "[20%!][20%!][]", "[][][][][][]"));
        details.add(newSmallLabel(TITLE), "wrap");
        details.add(title, "span, growx, wrap");
        details.add(newSmallLabel(GENRE));
        details.add(newSmallLabel(RATING));
        details.add(newSmallLabel(YEAR), "wrap");
        details.add(genre);
        details.add(rating);
        details.add(year, "growx, wrap");
        details.add(newSmallLabel(DESCRIPTION), "wrap");
        details.add(description, "grow, span");
        
        addDetails();
    }
    
    private void configureAudioDetailsLayout() {
        details.setLayout(new MigLayout("fillx, nocache", "[50%!][20%!]0[10%][10%]", "[][][][][][]"));
        details.add(newSmallLabel(TITLE));
        details.add(newSmallLabel(ARTIST), "wrap");
        details.add(title, "growx");
        details.add(artist, "span, growx, wrap");
        details.add(newSmallLabel(ALBUM));
        details.add(newSmallLabel(GENRE));
        details.add(newSmallLabel(YEAR));
        details.add(newSmallLabel(TRACK), "wrap");
        details.add(album, "growx");
        details.add(genre);
        details.add(year, "wmin 60");
        details.add(track, "wrap, wmin 40");
        details.add(newSmallLabel(DESCRIPTION), "wrap");
        details.add(description, "grow, span");
        
        addDetails();
    }
    
    private void configureImageDetailsLayout() {
        details.setLayout(new MigLayout("fillx", "[]", "[][][][]"));
        details.add(newSmallLabel(TITLE), "wrap");
        details.add(title, "growx, wrap");
        details.add(newSmallLabel(DESCRIPTION), "wrap");
        details.add(description, "grow");
        
        addDetails();
    }
    
    private void configureProgramDetailsLayout() {
        details.setLayout(new MigLayout("fillx, nocache", "[30%!][]", "[][][][][][]"));
        details.add(newSmallLabel(TITLE), "wrap");
        details.add(title, "span, growx, wrap");
        details.add(newSmallLabel(PLATFORM));
        details.add(newSmallLabel(COMPANY), "wrap");
        details.add(platform);
        details.add(company, "growx, wrap");
        details.add(newSmallLabel(DESCRIPTION), "wrap");
        details.add(description, "grow, span");
        
        addDetails();
    }
    
    private void configureDocumentDetailsLayout() {
        details.setLayout(new MigLayout("fillx", "[]", "[][][][]"));
        details.add(newSmallLabel(AUTHOR), "wrap");
        details.add(author, "growx, wrap");
        details.add(newSmallLabel(DESCRIPTION), "wrap");
        details.add(description, "grow");
        
        addDetails();
    }

    protected void populateMetadata(PropertiableFile propFile) {
        Map<String, String> metadata = new LinkedHashMap<String, String>();
        switch(propFile.getCategory()) {
            case AUDIO:
                addBitrateMetadata(str(propFile.getProperty(FilePropertyKey.BITRATE)), metadata);
                break;
            case VIDEO:
                addBitrateMetadata(str(propFile.getProperty(FilePropertyKey.BITRATE)), metadata);
                addDimensionMetadata(str(propFile.getProperty(FilePropertyKey.WIDTH)), str(propFile.getProperty(FilePropertyKey.HEIGHT)), metadata);
                break;
            case IMAGE:
                addDimensionMetadata(str(propFile.getProperty(FilePropertyKey.WIDTH)), str(propFile.getProperty(FilePropertyKey.HEIGHT)), metadata);
                break;
            case DOCUMENT:
                addDateCreatedMetadata(convertDate(propFile), metadata);
                //TODO: parse TOPIC property
                break;
        }
        addHashMetadata(propFile.getUrn(), metadata);
        setMetadataText(metadata);
    }
    
    protected String convertDate(PropertiableFile propertiable) {
        Object time = propertiable.getProperty(FilePropertyKey.DATE_CREATED);
        if (time != null  && time instanceof Long) {
            return DATE_FORMAT.format(new java.util.Date((Long) time));
        }
        return "";
    }

    protected String str(Object property) {
        return property == null ? null : property.toString();
    }

    protected void disableEdit(JTextComponent... comps) {
        for (JTextComponent comp : comps) {
            comp.setEditable(false);
        }
    }

    protected void disableEdit(JComboBox... combos) {
        for(JComboBox combo : combos) {
            combo.setEditable(false);
        }
    }
    
    private static class ReadOnlyTableModel extends DefaultTableModel {

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }
}
