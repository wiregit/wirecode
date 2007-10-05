package com.limegroup.gnutella.gui.options.panes;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;

import com.limegroup.gnutella.util.LimeTestCase;

public class StoreSaveTemplateProcessorTest extends LimeTestCase {

    public StoreSaveTemplateProcessorTest(String name) {
        super(name);
    }
    
    public static Test suite() { 
        return buildTestSuite(StoreSaveTemplateProcessorTest.class);
    }
    
    private final static Map<String,String> SUBSTITUTIONS = new HashMap<String,String>();
    private static final String ALBUM       = "album";
    private static final String ARTIST      = "artist";
    private static final String HOME        = "home";
    private static final String ALBUM_VAR   = "${" + StoreSaveTemplateProcessor.ALBUM_LABEL +   "}";
    private static final String ARTIST_VAR  = "${" + StoreSaveTemplateProcessor.ARTIST_LABEL +  "}";
    private static final String HOME_VAR    = "${" + StoreSaveTemplateProcessor.HOME_LABEL +    "}";

    private static final File OUTDIR = new File(".");
    static {
        SUBSTITUTIONS.put(StoreSaveTemplateProcessor.ALBUM_LABEL,   ALBUM);
        SUBSTITUTIONS.put(StoreSaveTemplateProcessor.ARTIST_LABEL,  ARTIST);
        SUBSTITUTIONS.put(StoreSaveTemplateProcessor.HOME_LABEL,    HOME);
    }
    
    public void testNull() {
        runTest(null, new File("."));
    }
    
    public void testEmpty() {
        runTest("", new File("."));
    }
    
    public void testDot() {
        runTest(".", new File("."));
    }
    
    public void testA() {
        runTest("a", new File(OUTDIR,"a"));
    }
    
    public void testASlashB() {
        runTest("a/b", new File(OUTDIR,"a/b"));
    }
    
    public void testArtistSub() { 
        runTest(ARTIST_VAR, new File(OUTDIR,ARTIST));
    }
    
    public void testAArtistSub() {
        runTest("A" + ARTIST_VAR, new File(OUTDIR,"A" + ARTIST));
    }
    
    public void testArtistSubA() {
        runTest(ARTIST_VAR + "A", new File(OUTDIR,ARTIST + "A"));
    }
    
    public void testAArtistSubA() {
        runTest("A" + ARTIST_VAR + "A", new File(OUTDIR,"A" + ARTIST + "A"));
    }
    
    public void testAlbumSub() {
        runTest(ALBUM_VAR, new File(OUTDIR,ALBUM));
    }
    
    public void testHomeSub() {
        runTest(HOME_VAR, new File(OUTDIR,HOME));
    }
    
    private void runTest(final String template, final File want) {
        //
        // Put whitespace around the braces
        //
        if( template != null ) {
            runTestInternal(template.replace("{", "{ "), want);
            runTestInternal(template.replace("{", " {"), want);
            runTestInternal(template.replace("}", " }"), want);
        }
        runTestInternal(template, want);
    }

    private void runTestInternal(final String template, final File want) {
        try {
            final File have = new StoreSaveTemplateProcessor().getOutputDirectory(template, SUBSTITUTIONS, OUTDIR); 
            assertEquals(template + ":" + want + " != " + have, want, have);
        } catch (StoreSaveTemplateProcessor.IllegalTemplateException e) {
            fail(e.getMessage());
        }
    }
        
}