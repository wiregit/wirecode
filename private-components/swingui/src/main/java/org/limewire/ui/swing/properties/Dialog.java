package org.limewire.ui.swing.properties;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXHyperlink;
import org.jdesktop.swingx.JXLabel;
import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.Line;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.util.StringUtils;

public abstract class Dialog extends JDialog {

    private static final String HASH = "Hash";
    protected final JLabel icon = new JLabel();
    protected final JLabel heading = newLabel();
    protected final JLabel filename = newLabel();
    protected final JLabel subheading = newLabel();
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
    protected final DefaultTableModel readOnlyInfoModel = new DefaultTableModel();
    protected final JTable readOnlyInfo = new JTable(readOnlyInfoModel);
    
    protected final JPanel overview;
    protected final JPanel details = new JPanel();
    protected final JPanel location = new JPanel();
    protected Component detailsContainer;
    
    public Dialog() {
        setLayout(new MigLayout("insets 0 3 3 0", "[fill]push[]", "[][][]push[]"));
        JPanel buttons = new JPanel(new MigLayout("", "[][]", "[]"));
        buttons.add(new JButton(new OKAction()));
        buttons.add(new JButton(new CancelAction()));
        add(buttons, "cell 1 3");
        
        overview = new JPanel(new MigLayout("fillx", "[][]push[]", "[][][]"));

        overview.add(icon);
        overview.add(heading);
        overview.add(filename, "wrap");
        overview.add(subheading, "cell 1 1");
        overview.add(fileSize, "wrap");
        overview.add(metadata, "cell 1 2");
        overview.add(copyToClipboard);

        addOverview();
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
        
        setPreferredSize(new Dimension(570,600));
        setModalityType(ModalityType.APPLICATION_MODAL);
        
        setDefaultCloseOperation(2);

        pack();

        setLocationRelativeTo(GuiUtils.getMainFrame());
        
        setVisible(true);
    }

    private void addOverview() {
        add(box("Overview", overview), "cell 0 0, spanx 2");
    }

    private Component box(String string, JComponent component) {
        JPanel panel = new JPanel(new MigLayout("insets 3 3 3 3, fillx", "[fill]", "[][][]"));
        panel.add(new JLabel(tr(string)), "wrap");
        panel.add(Line.createHorizontalLine(), "wrap");
        panel.add(component);
        return panel;
    }

    private void addDetails() {
        detailsContainer = box("Details", details);
        add(detailsContainer, "cell 0 1, spanx 2");
    }

    private void addLocation() {
        add(box("Location", location), "cell 0 2, spanx 2");
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
        addMetadata(HASH, hash.toString(), metadata);
    }

    protected void addDateCreatedMetadata(String dateCreated, Map<String, String> metadata) {
        addMetadata("Date Created", dateCreated, metadata);
    }

    protected void addSampleRateMetadata(String sampleRate, Map<String, String> metadata) {
        addMetadata("Sample Rate", sampleRate, metadata);
    }

    protected void addBitrateMetadata(String bitRate, Map<String, String> metadata) {
        addMetadata("Bitrate", bitRate, metadata);
    }

    protected void addDimensionMetadata(String width, Object height, Map<String, String> metadata) {
        addMetadata("Dimensions", tr("({0}px X {1}px)", width, height), metadata);
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

    private void configureVideoDetailsLayout() {
        details.setLayout(new MigLayout("fillx, nocache", "[20%!][20%!][]", "[][][][][][]"));
        details.add(new JLabel(tr("Title")), "wrap");
        details.add(title, "span, growx, wrap");
        details.add(new JLabel(tr("Genre")));
        details.add(new JLabel(tr("Rating")));
        details.add(new JLabel(tr("Year")), "wrap");
        details.add(genre);
        details.add(rating);
        details.add(year, "growx, wrap");
        details.add(new JLabel(tr("Description")), "wrap");
        details.add(description, "grow, span");
        
        addDetails();
    }
    
    private void configureAudioDetailsLayout() {
        details.setLayout(new MigLayout("fillx, nocache", "[50%!][20%!]0[10%][10%]", "[][][][][][]"));
        details.add(new JLabel(tr("Title")));
        details.add(new JLabel(tr("Artist")), "wrap");
        details.add(title, "growx");
        details.add(artist, "span, growx, wrap");
        details.add(new JLabel(tr("Album")));
        details.add(new JLabel(tr("Genre")));
        details.add(new JLabel(tr("Year")));
        details.add(new JLabel(tr("Track")), "wrap");
        details.add(album, "growx");
        details.add(genre);
        details.add(year);
        details.add(track, "wrap");
        details.add(new JLabel(tr("Description")), "wrap");
        details.add(description, "grow, span");
        
        addDetails();
    }
    
    private void configureImageDetailsLayout() {
        details.setLayout(new MigLayout("fillx", "[]", "[][][][]"));
        details.add(new JLabel(tr("Title")), "wrap");
        details.add(title, "growx, wrap");
        details.add(new JLabel(tr("Description")), "wrap");
        details.add(description, "grow");
        
        addDetails();
    }
    
    private void configureProgramDetailsLayout() {
        details.setLayout(new MigLayout("fillx, nocache", "[20%!][]", "[][][][][][]"));
        details.add(new JLabel(tr("Title")), "wrap");
        details.add(title, "span, growx, wrap");
        details.add(new JLabel(tr("Platform")));
        details.add(new JLabel(tr("Company")), "wrap");
        details.add(platform);
        details.add(company, "growx, wrap");
        details.add(new JLabel(tr("Description")), "wrap");
        details.add(description, "grow, span");
        
        addDetails();
    }
    
    private void configureDocumentDetailsLayout() {
        details.setLayout(new MigLayout("fillx", "[]", "[][][][]"));
        details.add(new JLabel(tr("Author")), "wrap");
        details.add(author, "growx, wrap");
        details.add(new JLabel(tr("Description")), "wrap");
        details.add(description, "grow");
        
        addDetails();
    }

    protected void populateMetadata(PropertiableFile propFile) {
        Map<String, String> metadata = new LinkedHashMap<String, String>();
        switch(propFile.getCategory()) {
            case AUDIO:
                addBitrateMetadata(str(propFile.getProperty(FilePropertyKey.BITRATE)), metadata);
                addSampleRateMetadata(str(propFile.getProperty(FilePropertyKey.SAMPLE_RATE)), metadata);
                break;
            case VIDEO:
                addBitrateMetadata(str(propFile.getProperty(FilePropertyKey.BITRATE)), metadata);
                addDimensionMetadata(str(propFile.getProperty(FilePropertyKey.WIDTH)), str(propFile.getProperty(FilePropertyKey.HEIGHT)), metadata);
                break;
            case IMAGE:
                addDimensionMetadata(str(propFile.getProperty(FilePropertyKey.WIDTH)), str(propFile.getProperty(FilePropertyKey.HEIGHT)), metadata);
                break;
            case DOCUMENT:
                addDateCreatedMetadata(str(propFile.getProperty(FilePropertyKey.DATE_CREATED)), metadata);
        }
        addHashMetadata(propFile.getUrn(), metadata);
        setMetadataText(metadata);
    }

    protected String str(Object property) {
        return property == null ? null : property.toString();
    }
}
