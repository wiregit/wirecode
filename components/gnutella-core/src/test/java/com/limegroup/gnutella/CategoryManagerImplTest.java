package com.limegroup.gnutella;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.api.Category;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.setting.StringArraySetting;
import org.limewire.util.AssignParameterAction;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.limegroup.gnutella.simpp.SimppListener;
import com.limegroup.gnutella.simpp.SimppManager;

public class CategoryManagerImplTest extends LimeTestCase {
    
    private CategoryManagerImpl cmi;
    private SimppListener simppListener;
    
    public CategoryManagerImplTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(CategoryManagerImplTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        Mockery mockery = new Mockery();
        
        final SimppManager simppManager = mockery.mock(SimppManager.class);        
        final AtomicReference<SimppListener> listenRef = new AtomicReference<SimppListener>();
        cmi = new CategoryManagerImpl();
        mockery.checking(new Expectations() {{
            exactly(1).of(simppManager).addListener(with(any(SimppListener.class)));
            will(new AssignParameterAction<SimppListener>(listenRef));
        }});
        cmi.register(simppManager);
        mockery.assertIsSatisfied();
        simppListener = listenRef.get();
    }
    
    public void testBuiltInDocumentExtensions() {
        checkExtensions(Category.DOCUMENT, 
                "123", "abw", "accdb", "accde", "accdr", "accdt", "ans", "asc", "asp", "bdr",
                "chm", "css", "csv", "dat", "db", "dif", "diz", "doc", "docm", "docx", "dotm",
                "dotx", "dvi", "eml", "eps", "epsf", "fm", "grv", "gsa", "gts", "hlp", "htm",
                "html", "idb", "idx", "iif", "info", "js", "jsp", "kfl", "kwd", "latex", "lif",
                "lit", "log", "man", "mcw", "mht", "mhtml", "mny", "msg", "obi", "odp",
                "ods", "odt", "ofx", "one", "onepkg", "ost", "pages", "pdf", "php", "pot", "potm",
                "potx", "pps", "ppsm", "ppsx", "ppt", "pptm", "pptx", "ps", "pub", "qba", "qbb",
                "qdb", "qbi", "qbm", "qbw", "qbx", "qdf", "qel", "qfp", "qpd", "qph", "qmd", "qsd",
                "rtf", "scd", "sdc", "sdd", "sdp", "sdw", "shw", "sldx", "sxc", "sxd", "sxp",
                "sxw", "t01", "t02", "t03", "t04", "t05", "t06", "t07", "t08", "t09", "t98", "t99",
                "ta0", "ta1", "ta2", "ta3", "ta4", "ta5", "ta6", "ta7", "ta8", "ta9", "tax",
                "tax2008", "tex", "texi", "toc", "tsv", "tvl", "txf", "txt", "wk1", "wk3", "wk4",
                "wks", "wp", "wp5", "wpd", "wps", "wri", "xhtml", "xlam", "xls", "xlsb", "xlsm",
                "xlsx", "xltm", "xltx", "xml", "xsf", "xsn", "qfx", "qif", "bud", "ofc", "pst",
                "mbf", "mn1", "mn2", "mn3", "mn4", "mn5", "mn6", "mn7", "mn8", "mn9", "m10", "m11",
                "m12", "m13", "m14", "m15", "m16", "boe", "box", "bri", "cnm", "dbx", "eml",
                "emlx", "idb", "idx", "maildb", "mbg", "mbs", "mbx", "mht", "msb", "msf", "msg",
                "nws", "pmi", "pmm", "pmx", "tbb", "toc", "vfb", "zmc", "stw", "odm", "ott", "wpt");
    }
    
    public void testBuiltInAudioExtensions() {
        checkExtensions(Category.AUDIO,
                "mp3", "mpa", "mp1", "mpga", "mp2", "ra", "rm",
                "ram", "rmj", "wma", "wav", "m4a", "m4p", "lqt", "ogg", "med", "aif", "aiff",
                "aifc", "au", "snd", "s3m", "aud", "mid", "midi", "rmi", "mod", "kar", "ac3", "shn",
                "fla", "flac", "cda", "mka");
    }
    
    public void testBuiltInVideoExtensions() {
        checkExtensions(Category.VIDEO, "mpg", "mpeg", "mpe", "mng", "mpv", "m1v", "vob", 
                "mpv2", "mp2v", "m2p", "m2v", "mpgv", "vcd", "mp4", "dv", "dvd", "div", "divx",
                "dvx", "smi", "smil", "rv", "rmm", "rmvb", "avi", "asf", "asx", "wmv",
                "qt", "mov", "fli", "flc", "flx", "flv", "wml", "vrml", "swf", "dcr", "jve", "nsv",
                "mkv", "ogm", "cdg", "srt", "sub", "flv");
    }
    
    public void testBuiltInImageExtensions() {
        checkExtensions(Category.IMAGE, "gif", "png", "bmp", "jpg", "jpeg", "jpe", "jif",
            "jiff", "jfif", "tif", "tiff", "iff", "lbm", "ilbm", "mac", "drw", "pct", "img",
            "bmp", "dib", "rle", "ico", "ani", "icl", "cur", "emf", "wmf", "pcx", "pcd", "tga",
            "pic", "fig", "psd", "wpg", "dcx", "cpt", "mic", "pbm", "pnm", "ppm", "xbm", "xpm",
            "xwd", "sgi", "fax", "rgb", "ras");
    }
    
    public void testBuiltInProgramExtensions() {
        checkExtensions(Category.PROGRAM, "app", "bin", "mdb", "sh", "csh", "awk", "pl", "rpm",
                "deb", "gz", "gzip", "z", "bz2", "zoo", "tar", "tgz", "taz", "shar", "hqx", "sit",
                "dmg", "7z", "jar", "zip", "nrg", "cue", "iso", "jnlp", "rar", "sh", "mdb", "exe",
                "zip", "jar", "cab", "msi", "msp", "arj", "rar", "ace", "lzh", "lha", "bin", "nrg",
                "cue", "iso", "jnlp", "bat", "lnk", "vbs");
    }
    
    public void testBuiltInOsxLinuxPrograms() {
        Set<String> expecting = ImmutableSortedSet.orderedBy(String.CASE_INSENSITIVE_ORDER).add(
                "app", "bin", "mdb", "sh", "csh", "awk", "pl", "rpm", "deb", "gz", "gzip", "z",
                "bz2", "zoo", "tar", "tgz", "taz", "shar", "hqx", "sit", "dmg", "7z", "jar", "zip",
                "nrg", "cue", "iso", "jnlp", "rar", "sh").build();
        for(String e : expecting) {
            assertTrue("failed for: " + e, cmi.getOsxAndLinuxProgramsFilter().apply(e));
        }
    }
    
    public void testBuiltInWindowsPrograms() {
        Set<String> expecting = ImmutableSortedSet.orderedBy(String.CASE_INSENSITIVE_ORDER).add(
                "mdb", "exe", "zip", "jar", "cab", "msi", "msp", "arj", "rar", "ace", "lzh", "lha",
                "bin", "nrg", "cue", "iso", "jnlp", "bat", "lnk", "vbs").build();
        for(String e : expecting) {
            assertTrue("failed for: " + e, cmi.getWindowsProgramsFilter().apply(e));
        }
    }
    
    public void testRemoteAudio() {
        checkRemote(LibrarySettings.ADDITIONAL_AUDIO_EXTS, Category.AUDIO,
                LibrarySettings.ADDITIONAL_DOCUMENT_EXTS,
                LibrarySettings.ADDITIONAL_IMAGE_EXTS,
                LibrarySettings.ADDITIONAL_PROGRAM_OSX_LINUX_EXTS,
                LibrarySettings.ADDITIONAL_PROGRAM_WINDOWS_EXTS,
                LibrarySettings.ADDITIONAL_VIDEO_EXTS);
    }
    
    public void testRemoteDocument() {
        checkRemote(LibrarySettings.ADDITIONAL_DOCUMENT_EXTS, Category.DOCUMENT,
                LibrarySettings.ADDITIONAL_AUDIO_EXTS,
                LibrarySettings.ADDITIONAL_IMAGE_EXTS,
                LibrarySettings.ADDITIONAL_PROGRAM_OSX_LINUX_EXTS,
                LibrarySettings.ADDITIONAL_PROGRAM_WINDOWS_EXTS,
                LibrarySettings.ADDITIONAL_VIDEO_EXTS);
    }
    
    public void testRemoteImage() {
        checkRemote(LibrarySettings.ADDITIONAL_IMAGE_EXTS, Category.IMAGE,
                LibrarySettings.ADDITIONAL_AUDIO_EXTS,
                LibrarySettings.ADDITIONAL_DOCUMENT_EXTS,
                LibrarySettings.ADDITIONAL_PROGRAM_OSX_LINUX_EXTS,
                LibrarySettings.ADDITIONAL_PROGRAM_WINDOWS_EXTS,
                LibrarySettings.ADDITIONAL_VIDEO_EXTS);
    }
    
    public void testRemoteVideo() {
        checkRemote(LibrarySettings.ADDITIONAL_VIDEO_EXTS, Category.VIDEO,
                LibrarySettings.ADDITIONAL_AUDIO_EXTS,
                LibrarySettings.ADDITIONAL_DOCUMENT_EXTS,
                LibrarySettings.ADDITIONAL_IMAGE_EXTS,
                LibrarySettings.ADDITIONAL_PROGRAM_OSX_LINUX_EXTS,
                LibrarySettings.ADDITIONAL_PROGRAM_WINDOWS_EXTS);
    }
    
    public void testRemoteProgramOsLinux() {
        checkRemote(LibrarySettings.ADDITIONAL_PROGRAM_OSX_LINUX_EXTS, Category.PROGRAM,
                LibrarySettings.ADDITIONAL_AUDIO_EXTS,
                LibrarySettings.ADDITIONAL_DOCUMENT_EXTS,
                LibrarySettings.ADDITIONAL_IMAGE_EXTS,
                LibrarySettings.ADDITIONAL_VIDEO_EXTS);
        
    }
    
    public void testRemoteProgramWindows() {
        checkRemote(LibrarySettings.ADDITIONAL_PROGRAM_WINDOWS_EXTS, Category.PROGRAM,
                LibrarySettings.ADDITIONAL_AUDIO_EXTS,
                LibrarySettings.ADDITIONAL_DOCUMENT_EXTS,
                LibrarySettings.ADDITIONAL_IMAGE_EXTS,
                LibrarySettings.ADDITIONAL_VIDEO_EXTS);
    }
    
    public void testProgramsCanShareRemoteExtensions() {
        Collection<String> extensionsBefore = ImmutableSortedSet.copyOf(
                String.CASE_INSENSITIVE_ORDER, cmi.getExtensionsForCategory(Category.PROGRAM));
        
        String osxLinuxExt = "osxLinuxExt";
        String windowsExt = "windowsExt";
        String sharedExt = "sharedExt";
        
        assertFalse(extensionsBefore.contains(osxLinuxExt));
        assertFalse(extensionsBefore.contains(windowsExt));
        assertFalse(extensionsBefore.contains(sharedExt));
        
        LibrarySettings.ADDITIONAL_PROGRAM_OSX_LINUX_EXTS.set(new String[] { osxLinuxExt, sharedExt } );
        LibrarySettings.ADDITIONAL_PROGRAM_WINDOWS_EXTS.set(new String[] { windowsExt, sharedExt } );
        simppListener.simppUpdated();
        
        assertEquals(Category.PROGRAM, cmi.getCategoryForExtension(osxLinuxExt));
        assertEquals(Category.PROGRAM, cmi.getCategoryForFilename("a file." + osxLinuxExt));
        assertEquals(Category.PROGRAM, cmi.getCategoryForFile(new File("a file." + osxLinuxExt)));
        assertTrue(cmi.getExtensionFilterForCategory(Category.PROGRAM).apply(osxLinuxExt));
        assertTrue(cmi.getOsxAndLinuxProgramsFilter().apply(osxLinuxExt));
        assertFalse(cmi.getWindowsProgramsFilter().apply(osxLinuxExt));
        
        assertEquals(Category.PROGRAM, cmi.getCategoryForExtension(windowsExt));
        assertEquals(Category.PROGRAM, cmi.getCategoryForFilename("a file." + windowsExt));
        assertEquals(Category.PROGRAM, cmi.getCategoryForFile(new File("a file." + windowsExt)));
        assertTrue(cmi.getExtensionFilterForCategory(Category.PROGRAM).apply(windowsExt));
        assertFalse(cmi.getOsxAndLinuxProgramsFilter().apply(windowsExt));
        assertTrue(cmi.getWindowsProgramsFilter().apply(windowsExt));
        
        assertEquals(Category.PROGRAM, cmi.getCategoryForExtension(sharedExt));
        assertEquals(Category.PROGRAM, cmi.getCategoryForFilename("a file." + sharedExt));
        assertEquals(Category.PROGRAM, cmi.getCategoryForFile(new File("a file." + sharedExt)));
        assertTrue(cmi.getExtensionFilterForCategory(Category.PROGRAM).apply(sharedExt));
        assertTrue(cmi.getOsxAndLinuxProgramsFilter().apply(sharedExt));
        assertTrue(cmi.getWindowsProgramsFilter().apply(sharedExt));
        
    }
    
    private void checkRemote(StringArraySetting setting, Category category, StringArraySetting... otherSettings) {
        Collection<String> extensionsBefore = ImmutableSortedSet.copyOf(
                String.CASE_INSENSITIVE_ORDER, cmi.getExtensionsForCategory(category));
        
        String unique = "unique1";
        String dup = "sharedDup";
        List<String> remoteDups = new ArrayList<String>();
        List<String> builtInDups = new ArrayList<String>();
        assertFalse(extensionsBefore.contains(unique));
        assertFalse(extensionsBefore.contains(dup));
        
        for(Category otherCategory : Category.values()) {
            // ignore this category && other.
            if(otherCategory == category || otherCategory == Category.OTHER) {
                continue;
            }
            // add an extension that belonged in another category,
            // make sure it doesn't become our category later on.
            builtInDups.add(cmi.getExtensionsForCategory(otherCategory).iterator().next());
        }
        
        // Add some duplicates that only exist in a single setting to make sure
        // they're removed properly (and that we don't only remove duplicates
        // that exist in all settings).
        int count = 0;
        for(StringArraySetting other : otherSettings) {
            String remoteDup = "remoteDup" + count++;
            remoteDups.add(remoteDup);
            other.set(new String[] { dup, remoteDup } );
        }
        
        // Make sure that before we notify, modifying the setting doesn't change
        // anything
        setting.set(ImmutableList.<String> builder().add(unique).add(dup).addAll(builtInDups)
                .addAll(remoteDups).build().toArray(new String[0]));
        assertEquals(Category.OTHER, cmi.getCategoryForExtension(unique));
        
        // notify, make sure it took.
        simppListener.simppUpdated();
        assertEquals(category, cmi.getCategoryForExtension(unique));
        assertEquals(category, cmi.getCategoryForFilename("a file." + unique));
        assertEquals(category, cmi.getCategoryForFile(new File("a file." + unique)));
        assertTrue(cmi.getExtensionFilterForCategory(category).apply(unique));
        
        assertEquals(ImmutableSortedSet.orderedBy(String.CASE_INSENSITIVE_ORDER).addAll(
                extensionsBefore).add(unique).build(), cmi.getExtensionsForCategory(category));
        
        // Built in dups shouldn't be in this category, but *should* be in the original category
        for(String otherDup : builtInDups) {
            assertNotEquals(category, cmi.getCategoryForExtension(otherDup));
            assertNotEquals(Category.OTHER, cmi.getCategoryForExtension(otherDup));
        }
        // All the other dups are shared among remote settings & should be still
        // in OTHER
        assertEquals(Category.OTHER, cmi.getCategoryForExtension(dup));
        for(String remoteDup : remoteDups) {
            assertEquals(Category.OTHER, cmi.getCategoryForExtension(remoteDup));    
        }
        
        // If we wipe it out, it disappears.
        setting.set(new String[0]);
        simppListener.simppUpdated();
        assertEquals(Category.OTHER, cmi.getCategoryForExtension(unique));
        assertEquals(extensionsBefore, cmi.getExtensionsForCategory(category));
    }
        
    private void checkExtensions(Category category, String... exts) {
        Set<String> expecting = ImmutableSortedSet.orderedBy(String.CASE_INSENSITIVE_ORDER).add(exts).build();
        for(String e : expecting) {
            assertEquals("failed for: " + e, category, cmi.getCategoryForExtension(e));
            assertEquals("failed for: " + e, category, cmi.getCategoryForFilename("a file." + e));
            assertEquals("failed for: " + e, category, cmi.getCategoryForFile(new File("a file." + e)));
            assertTrue("failed for: " + e, cmi.getExtensionFilterForCategory(category).apply(e));
        }
        assertEquals(expecting, cmi.getExtensionsForCategory(category));
    }
    
}
