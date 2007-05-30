package org.limewire.store.storeserver.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

/**
 * Scan all the files in the project and assures (kind of) that no one
 * uses any class outside the org.limewire.store.storeserver.api package
 * in this component.
 * 
 * @author jpalm
 */
public class EnsureFolksOnlyUseTheAPIPackageTest extends TestCase {

    public void testAll() {
        
        // Form the package we want
        String pkgName = getClass().getPackage().getName();
        int ilastDot = pkgName.lastIndexOf(".");
        final String basePkg = pkgName.substring(0, ilastDot);
        final String apiPkg = basePkg + ".api";
        
        // Search for all the files
        List<File> q = new LinkedList<File>();
        q.add(new File("core"));
        q.add(new File("gui"));
        File comps = new File("components");
        for (File f : comps.listFiles()) {
            if (!f.getName().equals("storeserver")) q.add(f);
        }
        final Pattern p = Pattern.compile("import\\s+(\\S+);");
        while (!q.isEmpty()) {
            File f = q.remove(0);
            if (f.isDirectory()) {
                File[] fs = f.listFiles();
                for (File file : fs) q.add(file);
            } else {
                if (f.getName().endsWith(".java")) {
                    try {
                        BufferedReader in = new BufferedReader(new FileReader(f));
                        String line;
                        while ((line = in.readLine()) != null) {
                            Matcher m = p.matcher(line);
                            if (m.matches()) {
                                String pkg = m.group(1);
                                assertFalse(f.getAbsolutePath() + ":" + pkg, 
                                            pkg.startsWith(basePkg) && !pkg.contains(apiPkg));
                            }
                        }
                        in.close();
                    } catch (IOException e) { e.printStackTrace(); }
                }
            }
        }
    }
}
