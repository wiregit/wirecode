package com.limegroup.gnutella;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

import org.limewire.core.api.Category;
import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.util.FileUtils;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSortedSet;
import com.google.inject.Inject;
import com.limegroup.gnutella.simpp.SimppListener;
import com.limegroup.gnutella.simpp.SimppManager;

class CategoryManagerImpl implements CategoryManager {

    private final Collection<String> documentExtBuiltIn = ImmutableSortedSet.orderedBy(
            String.CASE_INSENSITIVE_ORDER).add("123", "abw", "accdb", "accde", "accdr", "accdt",
            "ans", "asc", "asp", "bdr", "chm", "css", "csv", "dat", "db", "dif", "diz", "doc",
            "docm", "docx", "dotm", "dotx", "dvi", "eml", "eps", "epsf", "fm", "grv", "gsa", "gts",
            "hlp", "htm", "html", "idb", "idx", "iif", "info", "js", "jsp", "kfl", "kwd", "latex",
            "lif", "lit", "log", "man", "mcw", "mht", "mhtml", "mny", "msg", "obi", "odp",
            "ods", "odt", "ofx", "one", "onepkg", "ost", "pages", "pdf", "php", "pot", "potm",
            "potx", "pps", "ppsm", "ppsx", "ppt", "pptm", "pptx", "ps", "pub", "qba", "qbb", "qdb",
            "qbi", "qbm", "qbw", "qbx", "qdf", "qel", "qfp", "qpd", "qph", "qmd", "qsd", "rtf",
            "scd", "sdc", "sdd", "sdp", "sdw", "shw", "sldx", "sxc", "sxd", "sxp", "sxw", "t01",
            "t02", "t03", "t04", "t05", "t06", "t07", "t08", "t09", "t98", "t99", "ta0", "ta1",
            "ta2", "ta3", "ta4", "ta5", "ta6", "ta7", "ta8", "ta9", "tax", "tax2008", "tex",
            "texi", "toc", "tsv", "tvl", "txf", "txt", "wk1", "wk3", "wk4", "wks", "wp", "wp5",
            "wpd", "wps", "wri", "xhtml", "xlam", "xls", "xlsb", "xlsm", "xlsx", "xltm", "xltx",
            "xml", "xsf", "xsn", "qfx", "qif", "bud", "ofc", "pst", "mbf", "mn1", "mn2", "mn3",
            "mn4", "mn5", "mn6", "mn7", "mn8", "mn9", "m10", "m11", "m12", "m13", "m14", "m15",
            "m16", "boe", "box", "bri", "cnm", "dbx", "eml", "emlx", "idb", "idx", "maildb", "mbg",
            "mbs", "mbx", "mht", "msb", "msf", "msg", "nws", "pmi", "pmm", "pmx", "tbb", "toc",
            "vfb", "zmc", "stw", "odm", "ott", "wpt").build();

    private final Collection<String> audioExtBuiltIn = ImmutableSortedSet.orderedBy(
            String.CASE_INSENSITIVE_ORDER).add("mp3", "mpa", "mp1", "mpga", "mp2", "ra", "rm",
            "ram", "rmj", "wma", "wav", "m4a", "m4p", "lqt", "ogg", "med", "aif", "aiff",
            "aifc", "au", "snd", "s3m", "aud", "mid", "midi", "rmi", "mod", "kar", "ac3", "shn",
            "fla", "flac", "cda", "mka").build();

    private final Collection<String> videoExtBuiltIn = ImmutableSortedSet.orderedBy(
            String.CASE_INSENSITIVE_ORDER).add("mpg", "mpeg", "mpe", "mng", "mpv", "m1v", "vob",
            "mpv2", "mp2v", "m2p", "m2v", "mpgv", "vcd", "mp4", "dv", "dvd", "div", "divx",
            "dvx", "smi", "smil", "rv", "rmm", "rmvb", "avi", "asf", "asx", "wmv",
            "qt", "mov", "fli", "flc", "flx", "flv", "wml", "vrml", "swf", "dcr", "jve", "nsv",
            "mkv", "ogm", "cdg", "srt", "sub", "flv").build();

    private final Collection<String> imageExtBuiltIn = ImmutableSortedSet.orderedBy(
            String.CASE_INSENSITIVE_ORDER).add("gif", "png", "bmp", "jpg", "jpeg", "jpe", "jif",
            "jiff", "jfif", "tif", "tiff", "iff", "lbm", "ilbm", "mac", "drw", "pct", "img",
            "bmp", "dib", "rle", "ico", "ani", "icl", "cur", "emf", "wmf", "pcx", "pcd", "tga",
            "pic", "fig", "psd", "wpg", "dcx", "cpt", "mic", "pbm", "pnm", "ppm", "xbm", "xpm",
            "xwd", "sgi", "fax", "rgb", "ras").build();

    private final Collection<String> programOsxLinuxExtBuiltIn = ImmutableSortedSet.orderedBy(
            String.CASE_INSENSITIVE_ORDER).add("app", "bin", "mdb", "sh", "csh", "awk", "pl",
            "rpm", "deb", "gz", "gzip", "z", "bz2", "zoo", "tar", "tgz", "taz", "shar", "hqx",
            "sit", "dmg", "7z", "jar", "zip", "nrg", "cue", "iso", "jnlp", "rar", "sh").build();

    private final Collection<String> programWindowsExtBuiltIn = ImmutableSortedSet.orderedBy(
            String.CASE_INSENSITIVE_ORDER).add("mdb", "exe", "zip", "jar", "cab", "msi", "msp", "arj",
            "rar", "ace", "lzh", "lha", "bin", "nrg", "cue", "iso", "jnlp", "bat", "lnk", "vbs")
            .build();
    
    private final AtomicReference<Collection<String>> audioExts = new AtomicReference<Collection<String>>();
    private final AtomicReference<Collection<String>> videoExts = new AtomicReference<Collection<String>>();
    private final AtomicReference<Collection<String>> imageExts = new AtomicReference<Collection<String>>();
    private final AtomicReference<Collection<String>> documentExts = new AtomicReference<Collection<String>>();
    private final AtomicReference<Collection<String>> programOsxLinuxExts = new AtomicReference<Collection<String>>();
    private final AtomicReference<Collection<String>> programWindowsExts = new AtomicReference<Collection<String>>();
    private final AtomicReference<Collection<String>> programExts = new AtomicReference<Collection<String>>();
    
    private final Predicate<String> audioPred = new CollectionPredicate(audioExts);
    private final Predicate<String> videoPred = new CollectionPredicate(videoExts);
    private final Predicate<String> programPred = new CollectionPredicate(programExts);
    private final Predicate<String> imagePred = new CollectionPredicate(imageExts);
    private final Predicate<String> docPred = new CollectionPredicate(documentExts);
    private final Predicate<String> otherPred = new Predicate<String>() {
        public boolean apply(String input) {
            // This would be a terrible implementation for other categories,
            // but for OTHER it is OK because all we can do is say,
            // "do i belong in another category?... if not -> other"
            return getCategoryForExtension(input) == Category.OTHER;
        };
    };    
    private final Predicate<String> programOsLinuxPred = new CollectionPredicate(programOsxLinuxExts);
    private final Predicate<String> programWindowsPred = new CollectionPredicate(programWindowsExts);
    
    CategoryManagerImpl() {
        rebuildExtensions();
    }
    
    @Inject void register(SimppManager simppManager) {
        // listen on all of simpp, not the specific settings,
        // so that we can rebuild in a batch & look at all new settings
        // at once.
        simppManager.addListener(new SimppListener() {
            @Override
            public void simppUpdated(int newVersion) {
                rebuildExtensions();
            }
        });
    }
    
    /** Rebuilds all extensions so that they contain both built-in & simpp extensions. */
    private void rebuildExtensions() {
        audioExts.set(combineAndCleanup(Category.AUDIO, audioExtBuiltIn, LibrarySettings.ADDITIONAL_AUDIO_EXTS.get()));
        imageExts.set(combineAndCleanup(Category.IMAGE, imageExtBuiltIn, LibrarySettings.ADDITIONAL_IMAGE_EXTS.get()));
        videoExts.set(combineAndCleanup(Category.VIDEO, videoExtBuiltIn, LibrarySettings.ADDITIONAL_VIDEO_EXTS.get()));
        documentExts.set(combineAndCleanup(Category.DOCUMENT, documentExtBuiltIn, LibrarySettings.ADDITIONAL_DOCUMENT_EXTS.get()));
        programOsxLinuxExts.set(combineAndCleanup(Category.PROGRAM, programOsxLinuxExtBuiltIn,
                LibrarySettings.ADDITIONAL_PROGRAM_OSX_LINUX_EXTS.get()));
        programWindowsExts.set(combineAndCleanup(Category.PROGRAM, programWindowsExtBuiltIn,
                LibrarySettings.ADDITIONAL_PROGRAM_WINDOWS_EXTS.get()));
        
        programExts.set(ImmutableSortedSet.orderedBy(String.CASE_INSENSITIVE_ORDER).addAll(
                programOsxLinuxExts.get()).addAll(programWindowsExts.get()).build());
    }
    
    private Collection<String> combineAndCleanup(Category category, Collection<String> builtIn, String[] remote) {
        Set<String> remoteSet = new TreeSet<String>(Arrays.asList(remote));
        // remove everything that's built-in
        remoteSet.removeAll(audioExtBuiltIn);
        remoteSet.removeAll(videoExtBuiltIn);
        remoteSet.removeAll(imageExtBuiltIn);
        remoteSet.removeAll(documentExtBuiltIn);
        remoteSet.removeAll(programOsxLinuxExtBuiltIn);
        remoteSet.removeAll(programWindowsExtBuiltIn);
        
        Collection<String> rAudio = Arrays.asList(LibrarySettings.ADDITIONAL_AUDIO_EXTS.get());
        Collection<String> rDoc = Arrays.asList(LibrarySettings.ADDITIONAL_DOCUMENT_EXTS.get());
        Collection<String> rImage = Arrays.asList(LibrarySettings.ADDITIONAL_IMAGE_EXTS.get());
        Collection<String> rProgramOsxLinux = Arrays.asList(LibrarySettings.ADDITIONAL_PROGRAM_OSX_LINUX_EXTS.get());
        Collection<String> rProgramWindows = Arrays.asList(LibrarySettings.ADDITIONAL_PROGRAM_WINDOWS_EXTS.get());
        Collection<String> rVideo = Arrays.asList(LibrarySettings.ADDITIONAL_VIDEO_EXTS.get());
        
        // Remove the stuff from the other remote extensions,
        // otherwise we can end up with two different categories having
        // the same extension
        switch(category) {
        case AUDIO:
            remoteSet.removeAll(rDoc);
            remoteSet.removeAll(rImage);
            remoteSet.removeAll(rProgramOsxLinux);
            remoteSet.removeAll(rProgramWindows);
            remoteSet.removeAll(rVideo);
            break;
        case DOCUMENT:
            remoteSet.removeAll(rAudio);
            remoteSet.removeAll(rImage);
            remoteSet.removeAll(rProgramOsxLinux);
            remoteSet.removeAll(rProgramWindows);
            remoteSet.removeAll(rVideo);
            break;
        case IMAGE:
            remoteSet.removeAll(rAudio);
            remoteSet.removeAll(rDoc);
            remoteSet.removeAll(rProgramOsxLinux);
            remoteSet.removeAll(rProgramWindows);
            remoteSet.removeAll(rVideo);
            break;
        case PROGRAM:
            // Programs is a little special, because it's OK to share extensions btw
            // osx/linux & Windows program things. So we don't remove either OS specific
            // program from the other.
            remoteSet.removeAll(rAudio);
            remoteSet.removeAll(rDoc);
            remoteSet.removeAll(rImage);
            remoteSet.removeAll(rVideo);
            break;
        case VIDEO:
            remoteSet.removeAll(rAudio);
            remoteSet.removeAll(rImage);
            remoteSet.removeAll(rDoc);
            remoteSet.removeAll(rProgramOsxLinux);
            remoteSet.removeAll(rProgramWindows);
            break;
        default:    
            throw new IllegalStateException(category.toString());
        }

        return ImmutableSortedSet.orderedBy(String.CASE_INSENSITIVE_ORDER).
            addAll(builtIn).addAll(remoteSet).build();
    }

    @Override
    public Category getCategoryForExtension(String extension) {
        // note: the extension sets are all case insensitive,
        // so... no lowercasing required.
        if(audioExts.get().contains(extension)) {
            return Category.AUDIO;
        } else if(videoExts.get().contains(extension)) {
            return Category.VIDEO;
        } else if(programExts.get().contains(extension)) {
            return Category.PROGRAM;
        } else if(imageExts.get().contains(extension)) {
            return Category.IMAGE;
        } else if(documentExts.get().contains(extension)) {
            return Category.DOCUMENT;
        } else {
            return Category.OTHER;
        }
    }
    
    @Override
    public Category getCategoryForFilename(String filename) {
        String extension = FileUtils.getFileExtension(filename);
        return getCategoryForExtension(extension);
    }

    @Override
    public Category getCategoryForFile(File file) {
        // note: the extension sets are all case insensitive,
        // so... no lowercasing required.        
        String extension = FileUtils.getFileExtension(file);
        return getCategoryForExtension(extension);
    }

    @Override
    public Collection<String> getExtensionsForCategory(Category category) {
        switch(category) {
        case AUDIO:
            return audioExts.get();
        case DOCUMENT:
            return documentExts.get();
        case IMAGE:
            return imageExts.get();
        case PROGRAM:
            return programExts.get();
        case VIDEO:
            return videoExts.get();
        case OTHER:
            return Collections.emptySet();
        }
        
        throw new IllegalStateException("invalid category: " + category);
    }

    @Override
    public Predicate<String> getExtensionFilterForCategory(Category category) {
        switch(category) {
        case AUDIO:
            return audioPred;
        case DOCUMENT:
            return docPred;
        case IMAGE:
            return imagePred;
        case PROGRAM:
            return programPred;
        case VIDEO:
            return videoPred;
        case OTHER:
            return otherPred;
        }
        
        throw new IllegalStateException("invalid category: " + category);
    }

    @Override
    public Predicate<String> getOsxAndLinuxProgramsFilter() {
        return programOsLinuxPred;
    }

    @Override
    public Predicate<String> getWindowsProgramsFilter() {
        return programWindowsPred;
    }
    
    private static final class CollectionPredicate implements Predicate<String> {
        private final AtomicReference<Collection<String>> delegate;
        
        public CollectionPredicate(AtomicReference<Collection<String>> set) {
            this.delegate = set;
        }
        
        @Override
        public boolean apply(String input) {
            return delegate.get().contains(input);
        }
    }
}
