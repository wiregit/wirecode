package org.limewire.ui.swing.properties;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Component;
import java.awt.event.ActionEvent;

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
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.Line;

public abstract class Dialog extends JDialog {

    protected final JLabel headingLabel = new JLabel();
    protected final JLabel filename = new JLabel();
    protected final JLabel subheading = new JLabel();
    protected final JLabel fileSize = new JLabel();
    protected final JLabel metadata = new JLabel();
    protected final JXHyperlink copyToClipboard = new JXHyperlink();
    protected final JTextField title = new JTextField();
    protected final JComboBox genre = new JComboBox();
    protected final JComboBox rating = new JComboBox();
    protected final JTextField year = new JTextField();
    protected final JTextArea description = new JTextArea();
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
    
    public Dialog() {
        setLayout(new MigLayout("insets 0 3 3 0", "[fill]push[]", "[][][]push[]"));
        JPanel buttons = new JPanel(new MigLayout("", "[][]0", "[]0"));
        buttons.add(new JButton(new OKAction()));
        buttons.add(new JButton(new CancelAction()));
        add(buttons, "cell 1 3");
        
        overview = new JPanel(new MigLayout("fillx", "[]push[]", "[][][]"));

        overview.add(headingLabel);
        overview.add(filename, "wrap");
        overview.add(subheading);
        overview.add(fileSize, "wrap");
        overview.add(metadata);
        overview.add(copyToClipboard);

        addOverview();
    }

    protected void showDialog(String fileName) {
        setTitle(tr("\"{0}\" properties", fileName));
        pack();

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

    protected  void addDetails() {
        add(box("Details", details), "cell 0 1, spanx 2");
    }

    protected void addLocation() {
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
}
