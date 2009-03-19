package org.limewire.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.limewire.io.IOUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Module;

public class TestUtils {
    
    /**
     * Gets a resource using the class loader
     * or the system class loader.
     */
    public static URL getResource(String location) {
        ClassLoader cl = TestUtils.class.getClassLoader();            
        URL resource = null;
    
        if(cl == null) {
            resource = ClassLoader.getSystemResource(location);
        } else {
            resource = cl.getResource(location);
        }
        
        return resource;
    }
    
    /**
     * Gets a resource stream using the class loader
     * or the system class loader.
     */
    public static InputStream getResourceAsStream(String location) {
        ClassLoader cl = TestUtils.class.getClassLoader();            
        InputStream resource = null;
    
        if(cl == null) {
            resource = ClassLoader.getSystemResourceAsStream(location);
        } else {
            resource = cl.getResourceAsStream(location);
        }
        
        return resource;
    }

    /**
     * Gets a resource file using the class loader
     * or the system class loader.
     */
    public static File getResourceFile(String location) {           
        URL resource = getResource(location);
        
        if( resource == null ) {
            // note: this will probably not work,
            // but it will ultimately trigger a better exception
            // than returning null.
            return new File(location);
        }
        
        // if the resource exited inside a jar, let's create a temporary file for the world to use.
        // (we must convert path characters to the same character in order to check) 
        String resourceString = resource.toString();
        resourceString = resourceString.replace("\\", "/");
        location = location.replace("\\", "/");
        if(resourceString.startsWith("jar:file:") && resourceString.endsWith("!/" + location)) {
            File tmpFile;
            try {
                tmpFile = File.createTempFile("jarTmp", "lwTestTmp");
                tmpFile.deleteOnExit();
                
                InputStream stream = resource.openStream();
                saveStream(stream, tmpFile);
                return tmpFile;
            } catch(Throwable iox) {
                throw new RuntimeException("failed to expand jar resource file: " + location, iox);
            }
        }
        
        if(resourceString.startsWith("jar:file")) {
            throw new RuntimeException("resource[" + location + "] is inside jar and cannot be expanded");
        }
        
        //NOTE: The resource URL will contain %20 instead of spaces.
        // This is by design, but will not work when trying to make a file.
        // See BugParadeID: 4466485
        //(http://developer.java.sun.com/developer/bugParade/bugs/4466485.html)
        // The recommended workaround is to use the URI class, but that doesn't
        // exist until Java 1.4.  So, we can't use it here.
        // Thus, we manually have to parse out the %20s from the URL
        return new File( decode(resource.getFile()) );
    }
    
    /**
     * This method will find a jar with the given resource path and will extract the
     * contents from the first jar it finds that matches that path into a unique temporary
     * directory. Callers should delete the directory when they are done using it.
     * 
     * If the resource is found in the classpath as not a jar, then the resources are instead 
     * copied to the unique temp directory.
     */
    public static File extractResourceDirectory(String location) throws IOException {
        File efile = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
        URL url = ClassLoader.getSystemResource(location);
        
        URLConnection urlConnection = url.openConnection();
        
        if(JarURLConnection.class.isInstance(urlConnection)) {
            //if it is a jar file we want to extract files in the directory we need
            JarURLConnection jarConnection = (JarURLConnection)urlConnection;
            JarFile jarFile = jarConnection.getJarFile();
            for(Enumeration<JarEntry> entires =  jarFile.entries(); entires.hasMoreElements();) {
                JarEntry entry = entires.nextElement();
                String name = entry.getName();
                if(!entry.isDirectory() && name.startsWith(location)) {
                    InputStream in = null;
                    OutputStream out = null;
                    try {
                        in =  new BufferedInputStream(jarFile.getInputStream(entry));
                        File file = new File(name);
                        File outFile  = new File(efile, file.getName());
                        outFile.getParentFile().mkdirs();
                        out =  new BufferedOutputStream(new FileOutputStream(outFile));
                        FileUtils.write(in, out);
                    } finally {
                        IOUtils.close(in);
                        IOUtils.close(out);
                    }
                }
            }
            
        } else {
            //if it is on the filesystem already we can just copy the directory we need
            File resourceDir = getResourceFile(location);
            FileUtils.copyDirectory(resourceDir, efile);
        }
        
        return efile;
    }
    
    /**
     * Copy of CommonUtils.saveStream
     */
    private static void saveStream(InputStream inStream, File newFile) throws IOException {
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;            
        try {
            //buffer the streams to improve I/O performance
            final int bufferSize = 2048;
            bis = new BufferedInputStream(inStream, bufferSize);
            bos = new BufferedOutputStream(new FileOutputStream(newFile), bufferSize);
            byte[] buffer = new byte[bufferSize];
            int c = 0;
            
            do { //read and write in chunks of buffer size until EOF reached
                c = bis.read(buffer, 0, bufferSize);
                if (c > 0)
                    bos.write(buffer, 0, c);
            } while (c == bufferSize); //(# of bytes read)c will = bufferSize until EOF
            
            bos.flush();
        } catch(IOException e) {
            //if there is any error, delete any portion of file that did write
            newFile.delete();
        } finally {
            if(bis != null) {
                try {
                    bis.close();
                } catch(Throwable ignored) {}
            }
            if(bos != null) {
                try {
                    bos.close();
                } catch(Throwable ignored) {}
            }
        } 
    }
    
    private static String decode(String s) {
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '+':
                    sb.append(' ');
                    break;
                case '%':
                    try {
                        sb.append((char)Integer.parseInt(
                                        s.substring(i+1,i+3),16));
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(s);
                    }
                    i += 2;
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        // Undo conversion to external encoding
        String result = sb.toString();
        try {
            byte[] inputBytes = result.getBytes("8859_1");
            result = new String(inputBytes);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public static File getResourceInPackage(String resourceName, Class nearResource) {
        String name = nearResource.getPackage().getName().replace(".", "/");
        return getResourceFile(name + "/" + resourceName);
    }

    public static <T, F extends T> Module createInstanceModule(final Class<T> interfaze, final F implemenation) {
        return new AbstractModule() {
            @Override
            protected void configure() {
                bind(interfaze).toInstance(implemenation);
            }
        };
    }
    
    /**
     * Experimental fluent interface to binding test classes. 
     */
    public static To bind(Class... interfaces) {
        return new To(interfaces);
    }
    
    public static class To {
        
        private final Class[] interfaces;

        public To(Class...interfaces) {
            this.interfaces = interfaces;
        }
        
        public Module to(final Class... implementations) {
            if (interfaces.length != implementations.length) {
                throw new IllegalArgumentException("length of interfaces doesn't not match implementations");
            }
            return new AbstractModule() {
                @SuppressWarnings("unchecked")
                @Override
                protected void configure() {
                    for (int i = 0; i < interfaces.length; i++) {
                        bind(interfaces[i]).to(implementations[i]);
                    }
                }
            };
        }
        
        public Module toInstances(final Object...instances) {
            if (interfaces.length != instances.length) {
                throw new IllegalArgumentException("length of interfaces doesn't not match instances");
            }
            return new AbstractModule() {
                @SuppressWarnings("unchecked")
                @Override
                protected void configure() {
                    for (int i = 0; i < interfaces.length; i++) {
                        bind(interfaces[i]).toInstance(instances[i]);
                    }
                }
            };
        }
    }
}
