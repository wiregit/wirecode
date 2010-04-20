package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.StringUtils;

import com.google.common.collect.ImmutableSet;

public class ConfigureIpFiltersDialog extends LimeJDialog {
    
    private final JTextField blacklist = new JTextField(20);
    
    private final JTextField whitelist = new JTextField(20);
    
    private final static String[] countryCode = {
        "AP","EU","AD","AE","AF","AG","AI","AL","AM","AN","AO","AQ","AR",
        "AS","AT","AU","AW","AZ","BA","BB","BD","BE","BF","BG","BH","BI","BJ",
        "BM","BN","BO","BR","BS","BT","BV","BW","BY","BZ","CA","CC","CD","CF",
        "CG","CH","CI","CK","CL","CM","CN","CO","CR","CU","CV","CX","CY","CZ",
        "DE","DJ","DK","DM","DO","DZ","EC","EE","EG","EH","ER","ES","ET","FI",
        "FJ","FK","FM","FO","FR","FX","GA","GB","GD","GE","GF","GH","GI","GL",
        "GM","GN","GP","GQ","GR","GS","GT","GU","GW","GY","HK","HM","HN","HR",
        "HT","HU","ID","IE","IL","IN","IO","IQ","IR","IS","IT","JM","JO","JP",
        "KE","KG","KH","KI","KM","KN","KP","KR","KW","KY","KZ","LA","LB","LC",
        "LI","LK","LR","LS","LT","LU","LV","LY","MA","MC","MD","MG","MH","MK",
        "ML","MM","MN","MO","MP","MQ","MR","MS","MT","MU","MV","MW","MX","MY",
        "MZ","NA","NC","NE","NF","NG","NI","NL","NO","NP","NR","NU","NZ","OM",
        "PA","PE","PF","PG","PH","PK","PL","PM","PN","PR","PS","PT","PW","PY",
        "QA","RE","RO","RU","RW","SA","SB","SC","SD","SE","SG","SH","SI","SJ",
        "SK","SL","SM","SN","SO","SR","ST","SV","SY","SZ","TC","TD","TF","TG",
        "TH","TJ","TK","TM","TN","TO","TL","TR","TT","TV","TW","TZ","UA","UG",
        "UM","US","UY","UZ","VA","VC","VE","VG","VI","VN","VU","WF","WS","YE",
        "YT","RS","ZA","ZM","ME","ZW","A1","A2","O1","AX","GG","IM","JE","BL",
        "MF"};
    
    static {
        Arrays.sort(countryCode);
    }
    
    private final static Set<String> countryCodes = ImmutableSet.of(countryCode);
    
    public ConfigureIpFiltersDialog() {
        super(GuiUtils.getMainFrame(), I18n.tr("Configure ips"));
        setModal(true);
        JPanel container = new JPanel(new MigLayout());
        container.add(new JLabel(I18n.tr("Don't connect to peers from:")));
        container.add(blacklist, "span, wrap");
        container.add(new JLabel(I18n.tr("Only connect to peers from:")));
        container.add(whitelist, "span, wrap");
        container.add(new MultiLineLabel(I18n.tr("Valid country codes are:\n\n{0}\n", StringUtils.explode(countryCode, ", ")), 400), "span, wrap");
        container.add(new JButton(new OKAction()), "cell 3 3, alignx right");
        container.add(new JButton(new CancelAction()), "cell 3 3, alignx right");
        getContentPane().add(container);
        pack();
        setLocationRelativeTo(GuiUtils.getMainFrame());
        load();
    }
    
    private void load() {
        blacklist.setText(StringUtils.explode(ConnectionSettings.FILTERED_COUNTRIES.get(), ", "));
        whitelist.setText(StringUtils.explode(ConnectionSettings.ALLOWED_COUNTRIES.get(), ", "));
    }

    public static void main(String[] args) {
        new ConfigureIpFiltersDialog().setVisible(true);
    }
    
    private void save() {
        Set<String> filtered = sanitize(blacklist.getText());
        Set<String> whitelisted = sanitize(whitelist.getText());
        Set<String> intersection = new HashSet<String>(filtered);
        intersection.retainAll(whitelisted);
        assert intersection.isEmpty();
        ConnectionSettings.FILTERED_COUNTRIES.set(filtered);
        ConnectionSettings.ALLOWED_COUNTRIES.set(whitelisted);
    }
    
    Set<String> sanitize(String values) {
        String[] split = values.split(",");
        Set<String> results = new LinkedHashSet<String>();
        for (String value : split) {
            value = value.trim().toUpperCase(Locale.US);
            if (countryCodes.contains(value)) {
                results.add(value);
            }
        }
        return results;
    }
    
    private class OKAction extends AbstractAction {
        
        public OKAction() {
            super(I18n.tr("OK"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            save();
            dispose();
        }
    }
    
    private class CancelAction extends AbstractAction {
        
        public CancelAction() {
            super(I18n.tr("Cancel"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            dispose();
        }
        
    }
}
