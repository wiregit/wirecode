package com.limegroup.gnutella.i18n;

import java.text.*;
import java.util.*;
import java.io.*;

public class CountPercent {
    private static final int ACTION_STATISTICS = 0;
    private static final int ACTION_HTML = 1;
    private static final int ACTION_CHECK = 2;
    
    public static void main(String[] args) throws java.io.IOException {
        final int action;
        if (args != null && args.length > 0) {
            if (args[0].equals("html"))
                action = ACTION_HTML;
            else if (args[0].equals("check"))
                action = ACTION_CHECK;
            else {
                System.err.println("Usage: java CountPercent [html|check]");
                return;
            }
        } else
            action = ACTION_STATISTICS;
        new CountPercent(action);
    }
    
    private final DateFormat df;
    private final NumberFormat rc;
    private final NumberFormat pc;
    private final Map/*<String code, LanguageInfo li>*/ langs;
    private final Set/*<String key>*/ basicKeys, advancedKeys;
    private final int basicTotal;

    CountPercent(int action) throws java.io.IOException {
        df = DateFormat.getDateInstance(DateFormat.LONG, Locale.US);

        rc = NumberFormat.getNumberInstance(Locale.US);
        rc.setMinimumIntegerDigits(4);
        rc.setMaximumIntegerDigits(4);
        rc.setGroupingUsed(false);
        
        pc = NumberFormat.getPercentInstance(Locale.US);
        pc.setMinimumFractionDigits(2);
        pc.setMaximumFractionDigits(2);
        pc.setMaximumIntegerDigits(3);
        
        LanguageLoader loader = new LanguageLoader(new File("."));

        Properties defaultProps = loader.getDefaultProperties();
        advancedKeys = loader.getAdvancedKeys();
        defaultProps.keySet().removeAll(advancedKeys);
        basicKeys = defaultProps.keySet();
        basicTotal = basicKeys.size();

        langs = loader.loadLanguages();
        switch (action) {
        case ACTION_CHECK:
            checkBadKeys();
            break;
        case ACTION_STATISTICS:
            loader.extendVariantLanguages();
            loader.retainKeys(basicKeys);
            pc.setMinimumIntegerDigits(3);
            printStatistics();
            break;
        case ACTION_HTML:
            loader.extendVariantLanguages();
            loader.retainKeys(basicKeys);
            HTMLOutput html = new HTMLOutput(df, pc, langs, basicTotal);
            html.printHTML(System.out);
            break;
        }
    }
    
    /**
     * Check and list extra or badly names names found in resources.
     * Use the default (English) basic and extended resource keys.
     */
    private void checkBadKeys() {
        System.out.println("List of extra or badly named resource keys:");
        System.out.println("-------------------------------------------");
        
        for (Iterator i = langs.entrySet().iterator(); i.hasNext(); ) {
            final Map.Entry entry = (Map.Entry)i.next();
            final String code = (String)entry.getKey();
            final LanguageInfo li = (LanguageInfo)entry.getValue();
            final Properties props = li.getProperties();
            props.keySet().removeAll(basicKeys);
            props.keySet().removeAll(advancedKeys);
            if (props.size() != 0) {
                System.out.println("(" + code + ") " + li.getName() + ": " + li.getFileName());
                props.list(System.out);
                System.out.println("-------------------------------------------");
            }
        }
    }
    
    
    private void printStatistics() {
        System.out.println("Total Number of Resources: " + basicTotal);
        System.out.println("---------------------------------");
        System.out.println();
        
        for (Iterator i = langs.entrySet().iterator(); i.hasNext(); ) {
            final Map.Entry entry = (Map.Entry)i.next();
            final String code = (String)entry.getKey();
            final LanguageInfo li = (LanguageInfo)entry.getValue();
            final Properties props = li.getProperties();
            final int count = props.size();
            final double percentage = (double)count / (double)basicTotal;
            System.out.print(
                "(" + code + ") " +
                pc.format(percentage) +
                ", count: " + rc.format(count) +
                " (" + li.getName() + ": ");
            try {
                final byte[] langName = li.toString().getBytes("UTF-8");
                System.out.write(langName, 0, langName.length);
            } catch (java.io.UnsupportedEncodingException uee) {
                //shouldn't occur
            }
            System.out.println(")");
        }
    }
}