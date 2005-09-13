package com.limegroup.gnutella.i18n;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * @author Admin
 */
public class CountPercent {
    private static final int ACTION_STATISTICS = 0;

    private static final int ACTION_HTML = 1;

    private static final int ACTION_CHECK = 2;

    private static final int ACTION_UPDATE = 3;

    private static final int ACTION_RELEASE = 4;

    private static final int ACTION_NSIS = 5;

    private static final double RELEASE_PERCENTAGE = .65;

    /**
     * Launched from the console with command-line parameters. Usage: java
     * CountPercent [html|check|update [<code>]|release]
     * @param args a possibly empty array of command-line parameters.
     * @throws IOException
     */
    public static void main(String[] args) throws java.io.IOException {
        final int action;
        String code = null;
        if (args != null && args.length > 0) {
            if (args[0].equals("html")) {
                action = ACTION_HTML;
            } else if (args[0].equals("check")) {
                action = ACTION_CHECK;
            } else if (args[0].equals("update")) {
                action = ACTION_UPDATE;
                if (args.length > 1)
                    code = args[1];
            } else if (args[0].equals("release")) {
                action = ACTION_RELEASE;
            } else if (args[0].equals("nsis")) {
                action = ACTION_NSIS;
            } else {
                System.err
                        .println("Usage: java CountPercent [html|check|update [<code>]|release|nsis]");
                return;
            }
        } else
            action = ACTION_STATISTICS;
        new CountPercent(action, code);
    }

    private final DateFormat df;

    private final NumberFormat rc;

    private final NumberFormat pc;

    private final Map/* <String code, LanguageInfo li> */langs;

    private final Set/* <String key> */basicKeys, advancedKeys;

    private final int basicTotal;

    /**
     * @param action
     * @param code
     * @throws IOException
     */
    CountPercent(int action, String code) throws java.io.IOException {
        df = DateFormat.getDateInstance(DateFormat.LONG, Locale.US);

        rc = NumberFormat.getNumberInstance(Locale.US);
        rc.setMinimumIntegerDigits(4);
        rc.setMaximumIntegerDigits(4);
        rc.setGroupingUsed(false);

        pc = NumberFormat.getPercentInstance(Locale.US);
        pc.setMinimumFractionDigits(2);
        pc.setMaximumFractionDigits(2);
        pc.setMaximumIntegerDigits(3);

        File root = new File(".");
        LanguageLoader loader = new LanguageLoader(root);

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
        case ACTION_RELEASE:
        // release does an update, ensuring bad keys are gone
        // & native2ascii is done.
        case ACTION_NSIS:
        // nsis does an update too.
        case ACTION_UPDATE:
            loader.extendVariantLanguages();
            Set validKeys = new HashSet();
            validKeys.addAll(basicKeys);
            validKeys.addAll(advancedKeys);
            loader.retainKeys(validKeys);
            List lines = loader.getEnglishLines();
            LanguageUpdater updater = new LanguageUpdater(root, langs, lines);
            if (action == ACTION_RELEASE || action == ACTION_NSIS)
                updater.setSilent(true);
            if (code == null)
                updater.updateAllLanguages();
            else {
                LanguageInfo info = (LanguageInfo) langs.get(code);
                updater.updateLanguage(info);
            }

            if (action == ACTION_RELEASE) {
                loader.retainKeys(basicKeys);
                release(root);
            } else if (action == ACTION_NSIS) {
                loader.retainKeys(basicKeys);
                nsis();
            }

            break;
        }
    }

    /**
     * Check and list extra or badly names names found in resources. Use the
     * default (English) basic and extended resource keys.
     */
    private void checkBadKeys() {
        System.out.println("List of extra or badly named resource keys:");
        System.out.println("-------------------------------------------");

        for (Iterator i = langs.entrySet().iterator(); i.hasNext();) {
            final Map.Entry entry = (Map.Entry) i.next();
            final String code = (String) entry.getKey();
            final LanguageInfo li = (LanguageInfo) entry.getValue();
            final Properties props = li.getProperties();
            props.keySet().removeAll(basicKeys);
            props.keySet().removeAll(advancedKeys);
            if (props.size() != 0) {
                System.out.println("(" + code + ") " + li.getName() + ": "
                        + li.getFileName());
                props.list(System.out);
                System.out
                        .println("-------------------------------------------");
            }
        }
    }

    /**
     * Prints statistics about the number of translated resources.
     */
    private void printStatistics() {
        System.out.println("Total Number of Resources: " + basicTotal);
        System.out.println("---------------------------------");
        System.out.println();

        for (Iterator i = langs.entrySet().iterator(); i.hasNext();) {
            final Map.Entry entry = (Map.Entry) i.next();
            final String code = (String) entry.getKey();
            final LanguageInfo li = (LanguageInfo) entry.getValue();
            final Properties props = li.getProperties();
            final int count = props.size();
            final double percentage = (double) count / (double) basicTotal;
            System.out.print("(" + code + ") " + pc.format(percentage)
                    + ", count: " + rc.format(count) + " [" + li.getName()
                    + ": ");
            try {
                final byte[] langName = li.toString().getBytes("UTF-8");
                System.out.write(langName, 0, langName.length);
            } catch (java.io.UnsupportedEncodingException uee) {
                // shouldn't occur
            }
            System.out.println("]");
        }
    }

    /**
     * Releases the properties.
     */
    private void release(File root) {
        // First gather statistics on which languages have a suitable %
        // translated.
        List validLangs = new LinkedList();
        for (Iterator i = langs.values().iterator(); i.hasNext();) {
            LanguageInfo li = (LanguageInfo) i.next();
            int count = li.getProperties().size();
            double percentage = (double) count / (double) basicTotal;
            if (percentage >= RELEASE_PERCENTAGE)
                validLangs.add(li);
        }

        // Now that we've got a list of valid languages, go through
        // and copy'm to a release dir.
        File release = new File(root, "release");
        deleteAll(release);
        copy(root, release, new ReleaseFilter(validLangs));
    }

    /**
     * Lists all the languages that are of release-quality & have an NSIS name.
     */
    private void nsis() {
        System.out.println("English");
        for (Iterator i = langs.values().iterator(); i.hasNext();) {
            LanguageInfo li = (LanguageInfo) i.next();
            int count = li.getProperties().size();
            double percentage = (double) count / (double) basicTotal;
            if (percentage >= RELEASE_PERCENTAGE) {
                String name = li.getNSISName();
                if (!name.equals(""))
                    System.out.println(name);
            }
        }
    }

    /**
     * Recursively copies all files in root to dir that match Filter.
     */
    private void copy(File root, File dir, FileFilter filter) {
        File[] files = root.listFiles(filter);
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            if (f.isDirectory())
                copy(f, new File(dir, f.getName()), filter);
            else
                copy(f, dir);
        }
    }

    /**
     * Recursively deletes all files in a directory.
     */
    private void deleteAll(File f) {
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            for (int i = 0; i < files.length; i++)
                deleteAll(files[i]);
        }
        f.delete();
    }

    /**
     * Copies file to dir, ignoring any lines that are comments.
     */
    private void copy(File src, File dst) {
        dst.mkdirs();
        dst = new File(dst, src.getName());

        BufferedReader reader = null;
        BufferedWriter writer = null;
        try {
            reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(src), "ISO-8859-1"));
            writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(dst), "ISO-8859-1"));
            String read;
            while ((read = reader.readLine()) != null) {
                Line line = new Line(read);
                if (line.isComment())
                    continue;
                writer.write(read);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException e) {
                }
            if (writer != null) {
                try {
                    writer.flush();
                } catch (IOException e) {
                }
                try {
                    writer.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * Filter for releasing files.
     */
    private static class ReleaseFilter implements FileFilter {
        /**
         * Comment for <code>validLangs</code>
         */
        final List validLangs;

        /**
         * @param valid
         */
        ReleaseFilter(List valid) {
            validLangs = valid;
        }

        /**
         * @see java.io.FileFilter#accept(java.io.File)
         */
        public boolean accept(File f) {
            if (f.isDirectory())
                return true;
            String name = f.getName();
            if (!name.endsWith(".properties"))
                return false;
            int idxU;
            if ((idxU = name.indexOf('_')) == -1)
                return true; // base resource.
            String code = name.substring(idxU + 1, name.indexOf("."));
            if (code.equals("en")) // always valid.
                return true;

            // check to see if the code is in the list of valid codes.
            for (Iterator i = validLangs.iterator(); i.hasNext();) {
                LanguageInfo li = (LanguageInfo) i.next();
                // if its the base of a variant or the variant itself,
                // accept it. (need bases so the variant works.)
                if (code.equals(li.getBaseCode()) || code.equals(li.getCode()))
                    return true;
            }
            return false;
        }
    }
}
