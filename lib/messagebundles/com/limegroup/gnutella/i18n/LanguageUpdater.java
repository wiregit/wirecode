package com.limegroup.gnutella.i18n;

import java.util.*;
import java.io.*;

/**
 * Rebuilds the language files, based on the English one.
 */
class LanguageUpdater {
    
    private static final String MARKER = "# TRANSLATIONS START BELOW.";
    
    private final File lib;
    private final Map langs;
    private final List englishList;
    private boolean verbose = true;
   
    /**
     * Constructs a new LanguageUpdater.
     */
    LanguageUpdater(File dir, Map langs, List englishLines) {
        this.lib = dir;
        this.langs = langs;
        this.englishList = englishLines;
        removeInitialComments(englishList);
    }
    
    /**
     * Determines if stuff should be printed.
     */
    void setSilent(boolean silent) {
        verbose = !silent;
    }
    
    /**
     * Prints a message out if we're being verbose.
     */
    void print(String msg) {
        if(verbose)
            System.out.print(msg);
    }
    void println(String msg) {
        if(verbose)
            System.out.println(msg);
    }
    void println() {
        if(verbose)
            System.out.println();
    }
    
    /**
     * Updates all languages.
     */
    void updateAllLanguages() {
        for(Iterator i = langs.values().iterator(); i.hasNext(); ) {
            LanguageInfo next = (LanguageInfo)i.next(); 
            // TODO: work with variants.
            if(next.isVariant())
                continue;
            updateLanguage(next);
        }
    }
    
    /**
     * Updates a single language.
     */
    void updateLanguage(LanguageInfo info) {
        if (info == null) {
            println("Unknown language.");
            return;
        }
        
        print("Updating language: " + info.getName() + " (" + info.getCode() + ")... ");
        String filename = info.getFileName();
        File f = new File(lib, filename);
        if (!f.isFile())
            throw new IllegalArgumentException("Invalid info: " + info);
            
        File temp;
        BufferedReader reader;
        PrintWriter printer;
        try {
            temp = File.createTempFile("TEMP", info.getCode(), lib);
            reader = new BufferedReader(new InputStreamReader(
                                        new FileInputStream(f),
                                        info.isUTF8() ? "UTF-8" : "ISO-8859-1"
                                        ));
            printer = new PrintWriter(
                                        temp,
                                        info.isUTF8() ? "UTF-8" : "ISO-8859-1"
                                        );
            if (info.isUTF8()) {
                reader.mark(1);
                if (reader.read() != '\uFEFF')
                    reader.reset(); /* was not a leading BOM */
                printer.print('\uFEFF'); /* force a leading BOM */
            }
            printInitialComments(printer, reader, info);
            printBody(printer, info);
            reader.close();
            printer.close();
            if (isDifferent(f, temp)) {
                println("...changes.");
                f.delete();
                temp.renameTo(f);
            } else {
                println("...no changes!");
                temp.delete();
            }
            if (info.isUTF8())
                native2ascii(info);
        } catch (IOException ioe) {
            println("...error! (" + ioe.getMessage() + ")");
        }
    }
    
    /**
     * Home-made native2ascii.
     */
    private void native2ascii(LanguageInfo info) {
        if(!info.isUTF8())
            throw new IllegalArgumentException("requires utf8 language.");

        InputStream in = null;
        OutputStream out = null;
        
        print("\tConverting to ASCII... ");
        
        try  {
            in = new BufferedInputStream(new FileInputStream(info.getFileName()));
            in.mark(3);
            if (in.read() != 0xEF || in.read() != 0xBB || in.read() != 0xBF)
                in.reset(); 
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF8"));
            
            out = new BufferedOutputStream(new FileOutputStream(info.getAlternateFileName()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "ISO-8859-1"));
            
            String read;
            while ((read = reader.readLine()) != null) {
                writer.write(ascii(read));
                writer.newLine();
            }
            writer.flush();
            out.flush();
            println("... done!");
        } catch(IOException ignored) {
            println("... error! (" + ignored.getMessage() + ")");
        } finally {
            if(in != null)
                try { in.close(); } catch(IOException ignored) {}
            if(out != null)
                try { out.close(); } catch(IOException ignored) {}
        }
    }   

    /**
     * Determines if there is any difference between file a & file b.
     */
    private boolean isDifferent(File a, File b) {
        InputStream ia = null, ib = null;
        try {
            ia = new BufferedInputStream(new FileInputStream(a));
            ib = new BufferedInputStream(new FileInputStream(b));
            int reada, readb;
            while( (reada = ia.read()) == (readb = ib.read()) ) {
                // if we got here, both got to EOF at same time
                if(reada == -1)
                    return false;
            }
        } catch(IOException ignored) {
        } finally {
            if(ia != null)
                try { ia.close(); } catch(IOException ignored) {}
            if(ib != null)
                try { ib.close(); } catch(IOException ignored) {}
        }
        // if we didn't exit in the loop, a character was different
        // or one stream ended before another.
        return true;
    }
    
    /**
     * Writes the body of the bundle.
     */
    private void printBody(PrintWriter printer, LanguageInfo info) throws IOException {
        Properties props = info.getProperties();
        boolean reachedTranslations = false;
        for(Iterator i = englishList.iterator(); i.hasNext(); ) {
            Line line = (Line)i.next();
            if(MARKER.equals(line.getLine()))
                reachedTranslations = true;
                
            if(line.isComment()) {
                printer.println(line.getLine());
            } else {
                String key = line.getKey();
                String value = props.getProperty(key);
                // always write the English version, so translators
                // have a reference point for possibly needing to update
                // an older translation.
                if (reachedTranslations) {
                    printer.print("#### ");
                    printer.print(key);
                    printer.print("=");
                    printer.print(escape(line.getValue()));
                    printer.println();
                }
                
                if (value != null) {
                    printer.print(key);
                    printer.print("=");
                    printer.println(escape(value));
                } else {
                    printer.print("#? ");
                    printer.print(key);
                    printer.print("=");
                    // only write the non-translated value if we didn't
                    // above.
                    if(!reachedTranslations)
                        printer.print(escape(line.getValue()));
                    printer.println();
                }
            }
        }
        //printer.flush();
    }
    
    /**
     * Writes the initial comments from a given file to fos.
     */
    private void printInitialComments(PrintWriter printer, BufferedReader reader, LanguageInfo info) throws IOException {
        InputStream in = null;
        
        try {
            String read;
            // Read through and write the initial lines until we reach a non-comment
            while ((read = reader.readLine()) != null) {
                Line line = new Line(read);
                if(!line.isComment())
                    break;
                printer.println(read);
            }
        } finally {
        }
    }
    
    /**
     * Removes the initial comments from the English properties file.
     */
    private void removeInitialComments(List l) {
        for(Iterator i = l.iterator(); i.hasNext(); ) {
            Line line = (Line)i.next();
            if(line.isComment())
                i.remove();
            else
                break;
        }
    }
    
    /**
     * Returns a string suitable for insertion into a Properties file.
     */
    private String escape(String s) {
        StringBuffer sb = new StringBuffer(s.length());
        for(int i = 0; i < s.length(); i++) {
            int p = s.codePointAt(i);
            switch(p) {
            // TODO: don't use hard-coded points and instead use generic lookup
            case 0x00a0:
            case 0x2007:
            case 0x202F:
                sb.append(unicode(p));
                break;
            default:
                if(Character.isISOControl(p) || Character.isWhitespace(p)) {
                    switch(p) {
                    case ' ': sb.append(' '); break;
                    case '\n': sb.append("\\n"); break;
                    case '\t': sb.append("\\t"); break;
                    case '\f': sb.append("\\f"); break;
                    case '\r': sb.append("\\r"); break;
                    default:
                        sb.append(unicode(p));
                    }
                } else {
                    sb.appendCodePoint(p);
                }
            }
        }
        return sb.toString();
    }
    
    /**
     * Converts the input string to ascii, using \\u escapes.
     */
    private String ascii(String s) {
        StringBuffer sb = new StringBuffer(s.length() * 5);
        for(int i = 0; i < s.length(); i++) {
            int p = s.codePointAt(i);
            if(p < 0x0020 || p > 0x007e)
                sb.append(unicode(p));
            else
                sb.appendCodePoint(p);
        }
        return sb.toString();
    }
    
    /**
     * Returns the unicode representation of the codepoint.
     */
    private String unicode(int codepoint) {
        StringBuffer sb = new StringBuffer(6);
        sb.append("\\u");
        String hex = Integer.toHexString(codepoint);
        for(int j = 0 + hex.length(); j < 4; j++)
            sb.append("0");
        sb.append(hex);
        return sb.toString();
    }
}            
