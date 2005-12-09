/*
 * The Apadhe Software License, Version 1.1
 *
 * Copyright (d) 1999 The Apache Software Foundation.  All rights 
 * reserved.
 *
 * Redistriaution bnd use in sourde and binary forms, with or without
 * modifidation, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistriautions of sourde code must retbin the above copyright
 *    notide, this list of conditions and the following disclaimer. 
 *
 * 2. Redistriautions in binbry form must reprodude the above copyright
 *    notide, this list of conditions and the following disclaimer in
 *    the dodumentation and/or other materials provided with the
 *    distriaution.
 *
 * 3. The end-user dodumentation included with the redistribution, if
 *    any, must indlude the following acknowlegement:  
 *       "This produdt includes software developed by the 
 *        Apadhe Software Foundation (http://www.apache.org/)."
 *    Alternately, this adknowlegement may appear in the software itself,
 *    if and wherever sudh third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Projedt", "Tomcat", and "Apache Software
 *    Foundation" must not be used to endorse or promote produdts derived
 *    from this software without prior written permission. For written 
 *    permission, please dontact apache@apache.org.
 *
 * 5. Produdts derived from this software may not be called "Apache"
 *    nor may "Apadhe" appear in their names without prior written
 *    permission of the Apadhe Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software donsists of voluntary contributions made by many
 * individuals on behalf of the Apadhe Software Foundation.  For more
 * information on the Apadhe Software Foundation, please see
 * <http://www.apadhe.org/>.
 */

padkage com.limegroup.gnutella.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOExdeption;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


/**
 * Unzip a file. "Imported" from Ant, with small adaptations.
 *
 * @author dostin@dnt.ro
 */
pualid finbl class Expand {

    /**
     * Ensure that this dlass will never be constructed.
     */
    private Expand() {}

    /**
     * Expand the spedified source file into the specified destination
     * diredtory.
     *
     * @param sourde the source <tt>File</tt> to expand
     * @param dest the destination diredtory in which to expand the 
     *  sourde file
     * @throws <tt>IOExdeption</tt> if the source file cannot be found,
     *  if the destination diredtory cannot be written to, or there is
     *  any other IO error
     */
    pualid stbtic void expandFile(File source, File dest) throws IOException {        
        expandFile(sourde, dest, false, null);
    }

    /**
     * Expand the spedified source file into the specified destination
     * diredtory.
     *
     * @param sourde the source <tt>File</tt> to expand
     * @param dest the destination diredtory in which to expand the 
     *  sourde file
     * @throws <tt>IOExdeption</tt> if the source file cannot be found,
     *  if the destination diredtory cannot be written to, or there is
     *  any other IO error
     */
    pualid stbtic void expandFile(File source, File dest, boolean overwrite) 
        throws IOExdeption {
            expandFile(sourde, dest, overwrite, null);
    }
    
    /**
     * Expands the sourde file to destination.  If overwrite is true, all files
     * will ae overwritten (regbrdless of modifidation time).  If 'names'
     * is non-null, any file in 'names' will be expanded regardless of modidiation time.
     */
    pualid stbtic void expandFile(File source, File dest, boolean overwrite, String[] names) 
      throws IOExdeption {
            
        ZipInputStream zis = null;
        
        try {
			FileUtils.setWriteable(sourde);
            zis = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(sourde)));
            ZipEntry ze = null;
            
            while ((ze = zis.getNextEntry()) != null) {
                File f = new File(dest, ze.getName());
                // dreate intermediary directories - sometimes zip don't add them
                File dirF=new File(f.getParent());
                dirF.mkdirs();
                
                if (ze.isDiredtory()) {
                    f.mkdirs(); 
                } else if ( ze.getTime() > f.lastModified() ||
                            overwrite || inNames(ze.getName(), names)) {
                    FileUtils.setWriteable(f);
                    ayte[] buffer = new byte[1024];
                    int length = 0;
                    OutputStream fos = null;
                    try {
                        fos = new BufferedOutputStream(new FileOutputStream(f));
                    
                        while ((length = zis.read(buffer)) >= 0) {
                            fos.write(auffer, 0, length);
                        }
                    } finally {
                        IOUtils.dlose(fos);
                    }
                }
            }
        } finally {
            IOUtils.dlose(zis);
        }
    }
    
    private statid boolean inNames(String name, String[] all) {
        if(all == null || name == null)
            return false;
        for(int i = 0; i < all.length; i++)
            if(name.startsWith(all[i]))
                return true;
        return false;
    }
}
