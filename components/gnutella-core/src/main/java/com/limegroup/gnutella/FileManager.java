/**
 * auth: rsoule
 * file: FileManager.java
 * desc: This class will keep track of all the files that
 *       may be shared through the client.  It keeps them
 *       in the list _files.  There are methods for adding
 *       one file, or a whole directory.
 *
 * Updated by Sumeet Thadani 8/17/2000. Changed the search method so that
 * searches are possible with Regular Expressions. Imported necessary package
 */

package com.limegroup.gnutella;

import java.io.*;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.gml.GMLDocument;
import com.limegroup.gnutella.gml.GMLParseException;
import com.limegroup.gnutella.gml.GMLReplyCollection;
import com.limegroup.gnutella.gml.GMLReplyRepository;
import com.limegroup.gnutella.gml.GMLTemplateRepository;
import com.limegroup.gnutella.gml.TemplateNotFoundException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.xml.sax.InputSource;

public class FileManager{
    /** the total size of all files, in bytes.
     *  INVARIANT: _size=sum of all size of the elements of _files */
    private int _size;
    /** the total number of files.  INVARIANT: _numFiles==number of
     *  elements of _files that are not null. */
    private int _numFiles;
    /** the list of shareable files.  An entry is null if it is no longer
     *  shared.  INVARIANT: for all i, f[i]==null, or f[i].index==i and
     *  f[i]._path is in the shared folder with the shareable extension.
     *  LOCKING: obtain this before modifying. */
    private List /* of FileDesc */ _files;
    private String[] _extensions;
    private GMLTemplateRepository _templateRepository;
    private GMLReplyRepository _replyRepository;

    private static FileManager _instance = new FileManager();

    private Set _sharedDirectories;

    private FileManager() {
        // We'll initialize all the instance variables so that the FileManager
        // is ready once the constructor completes, even though the
        // thread launched at the end of the constructor will immediately
        // overwrite all these variables
        _size = 0;
        _numFiles = 0;
        _files = new ArrayList();
        _extensions = new String[0];
        _sharedDirectories = new HashSet();
        // These two variables are cleared, not overwritten
        _templateRepository = new GMLTemplateRepository();
        _replyRepository = new GMLReplyRepository();

        // Launch a thread to asynchronously scan directories to load files
        Thread loadSettingsThread =
            new Thread()
            {
                public void run()
                {
                    loadSettings();
                }
            };
        // Not a daemon thread -- terminating early is fine
        loadSettingsThread.start();
    }

    public static FileManager instance() {
        return _instance;
    }

    public GMLTemplateRepository getTemplateRepository() {
        return _templateRepository;
    }

    /**
     * @return a copy of the collection of GML replies stored in the reply
     *         repository for the given file.  Does not include the mp3 reply,
     *         if there is one.
     */
    public GMLReplyCollection getReplyCollection(File file) {
        String id = getReplyCollectionIdentifier(file);
        GMLReplyCollection replyCollection =
            _replyRepository.getReplyCollection(id);
        return replyCollection.createClone();
    }

    /**
     * Set the Reply Collection associated with <CODE>file</CODE>.
     * @throws IOException if the reply repository can't be written to disk
     */
    public void setReplyCollection(GMLReplyCollection replyCollection,
                                   File file) throws IOException {
        String id = getReplyCollectionIdentifier(file);
        _replyRepository.setReplyCollection(replyCollection, id);
        String replyFileName = SettingsManager.instance().getGMLReplyFile();
        FileWriter replyFileWriter = new FileWriter(replyFileName);
        _replyRepository.write(replyFileWriter);
        replyFileWriter.close();
    }

    /**
     * @return the identifier used for getting the file's metadata out of
     * the reply repository
     */
    private static String getReplyCollectionIdentifier(File file) {
        // Get the canoncial path, using the absolute path as a backup.
        try {
            return file.getCanonicalPath();
        } catch(IOException e2) {
            return file.getAbsolutePath();
        }
    }

    /** Returns the size of all files, in <b>bytes</b>. */
    public int getSize() {return _size;}
    public int getNumFiles() {return _numFiles;}

    /**
     * Returns the file descriptor with the given index.  Throws
     * IndexOutOfBoundsException if the index is not valid, either because the
     * file was never shared or was "unshared".<p>
     *
     * Design note: this is slightly unusual use of IndexOutOfBoundsException.
     * For example, get(0) and get(2) may throw an exception but get(1) may not.
     * NoSuchElementException was considered as an alernative, but this can
     * create ambiguity problems between java.util and com.sun.java.util.
     */
    public FileDesc get(int i) throws IndexOutOfBoundsException {
        FileDesc ret=(FileDesc)_files.get(i);
        if (ret==null)
            throw new IndexOutOfBoundsException();
        return ret;
    }

    /**
     * Returns an array of all responses matching the given request, or null if
     * there are no responses.<p>
     *
     * Design note: this method returns null instead of an empty array to avoid
     * allocations in the common case of no matches.)
     */
    public Response[] query(QueryRequest request) {
        List files = search(request.getTextQuery(), request.getRichQuery());
        if (files==null)
            return null;

        Response[] response = new Response[files.size()];
        int j = 0;
        for(Iterator iterFiles = files.iterator(); iterFiles.hasNext();  ) {
            FileDesc desc = (FileDesc)iterFiles.next();
            response[j++] = new Response(desc.getIndex(), desc.getSize(),
                                         desc.getName(), desc.getMetadata());
        }
        return response;
    }

    private boolean hasExtension(String filename) {
        int begin = filename.lastIndexOf(".");

        if (begin == -1)
            return false;

        String ext = filename.substring(begin + 1);

        int length = _extensions.length;
        for (int i = 0; i < length; i++) {
            if (ext.equalsIgnoreCase(_extensions[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * @modifies this
     * @effects adds the given file to this, if it exists
     *  and is of the proper extension.  <b>WARNING: this is a
     *  potential security hazard; caller must ensure the file
     *  is in the shared directory.</b>
     * This method should only be called from threads that hold this' monitor
     */
    private void addFile(String path) {
        File myFile = new File(path);

        if (!myFile.exists())
            return;

        String name = myFile.getName();     /* the name of the file */
        if (hasExtension(name)) {
            int n = (int)myFile.length();       /* the list, and increments */
            _size += n;                         /* the appropriate info */

            _files.add(new FileDesc(_files.size(),
                                    myFile,
                                    getReplyCollectionIdentifier(myFile),
                                    _templateRepository,
                                    _replyRepository));
            _numFiles++;
        }
    }

    /**
     * @modifies this
     * @effects adds the given file to this, if it exists
     *  and if it is shared.
     *  <b>WARNING: this is a potential security hazard.</b>
     */
    public synchronized void addFileIfShared(String path) {

        Assert.that(_sharedDirectories != null);

        File f = new File(path);

        String parent = f.getParent();

        File dir = new File(parent);

        if (dir == null)
            return;

        String p;

        try {
            p = dir.getCanonicalPath();
        } catch (IOException e) {
            return;
        }
        if (!_sharedDirectories.contains(p))
            return;

        addFile(path);
    }

    /**
     * @modifies this
     * @effects ensures the given file is not shared.  Returns
     *  true iff the file was previously shared.  In this case,
     *  the file's index will not be assigned to any other files.
     *  Note that the file is not actually removed from disk.
     */
    public synchronized boolean removeFileIfShared(File file) {
        //Look for a file matching <file>...
        for (int i=0; i<_files.size(); i++) {
            FileDesc fd=(FileDesc)_files.get(i);
            if (fd==null)
                continue;
            File candidate=new File(fd.getPath());

            //Aha, it's shared. Unshare it by nulling it out.
            if (file.equals(candidate)) {
                _files.set(i,null);
                _numFiles--;
                _size-=fd.getSize();
                return true;  //No more files in list will match this.
            }
        }
        return false;
    }

    /**
     * Loads the extensions and directories settings and rebuilds the file
     * index.
     *
     * @modifies this
     * @effects ensures that this contains exactly the files recursively given
     *  directories and all their recursive subdirectories.  If dir_names
     *  contains duplicate directories, the duplicates will be ignored.  If
     *  the directories list contains files, they will be ignored. Note that
     *  some files in this before the call will not be in this after the call,
     *  or they may have a different index.
     * <b>WARNING: this is a potential security hazard.</b>
     * WARNING: This call could run for a long time. Only threads that
     * are prepared for that possibility should invoke it.  See the
     * FileManager constructor for an example.
     */
    public synchronized void loadSettings() {
        // Reset the file list info
        _size = 0;
        _numFiles = 0;
        _files=new ArrayList();
        _sharedDirectories = new HashSet();

        // Load the settings info
        String templateDirectoryName =
            SettingsManager.instance().getGMLTemplateDirectory();
        String replyFileName =
            SettingsManager.instance().getGMLReplyFile();
        String extensions = SettingsManager.instance().getExtensions();
        String dir_names = SettingsManager.instance().getDirectories();

        // Reset the GML template repository and load the templates
        _templateRepository.clear();
        File templateDirectory = new File(templateDirectoryName);
        // Set up the DTD File
        File dtdFile = new File(templateDirectory, "gml-template.dtd");
        // Go through each file in the directory, attempting to load it
        // as a GML Template.  On failure, just skip the file.
        String[] templateNames = templateDirectory.list();
        for(int i = 0; i < templateNames.length; i++)
        {
            // Set up the DTD InputSource
            InputSource dtdSource = null;
            try
            {
                dtdSource = new InputSource(new FileInputStream(dtdFile));
            } catch(IOException e) {
                // If we're unable to read the DTD, leave dtdSource as null so
                // that the default resolution happens
            }

            // Load the template into the repository using the DTD InputSource
            try
            {
                _templateRepository.loadTemplate(
                    new InputSource(new FileInputStream(
                        new File(templateDirectory, templateNames[i]))),
                    dtdSource);
            } catch(FileNotFoundException e) {
            } catch(GMLParseException e) {
            }
        }

        // Reset the GML reply repository and load the reply metadata
        _replyRepository.clear();
        try
        {
            _replyRepository.loadReplies(
                new InputSource(new FileInputStream(replyFileName)),
                _templateRepository);
        } catch(GMLParseException e) {
            // TODO: We should probably report the error somehow.  This
            // happens when the reply repository is corrupt.
        } catch(FileNotFoundException e) {
            // TODO: We should probably report the error somehow.  This
            // happens when the reply repository is missing.
        }

        // Tokenize the extensions
        _extensions = HTTPUtil.stringSplit(extensions, ';');

        // Load the directories into the shared directories set
        String[] names = HTTPUtil.stringSplit(dir_names, ';');
        int size = names.length;
        for (int i = 0; i < size; i++) {
            File file = new File(names[i]);

            if (file.isDirectory())
                try {
                    _sharedDirectories.add(file.getCanonicalPath());
                } catch(IOException e) {
                    // Just skip the directory if we can't get a canonical path
                }
        }

        // Do another pass to actually process the directory.  This way,
        // we won't process a directory twice, even if it's specified twice in
        // the setting.
        for(Iterator iter = _sharedDirectories.iterator(); iter.hasNext();  )
            addDirectory((String)iter.next());
    }

    /**
     *  Build the equivalent of the File.listFiles() utility in jdk1.2.2
     */
    private File[] listFiles(File dir) {
        String [] fnames   = dir.list();
        File   [] theFiles = new File[fnames.length];

        for ( int i = 0; i < fnames.length; i++ )
            theFiles[i] = new File(dir, fnames[i]);

        return theFiles;
    }

    /**
     * @modifies this
     * @effects adds the all the files with shared extensions
     *  in the given directory and its recursive children.
     *  If dir_name is actually a file, it will be added if it has
     *  a shared extension.  Entries in this before the call are unmodified.
     */
    public synchronized void addDirectory(String dir_name) {
        File myFile = new File(dir_name);
        if (!myFile.exists())
            return;
        File[] file_list = listFiles(myFile);   /* the files in a specified */
        int n = file_list.length;               /* directory */

        // go through file_list
        // get file name
        // se if it contains extention.
        // if yes, add to new list...


        for (int i=0; i < n; i++) {

            if (file_list[i].isDirectory())     /* the recursive call */
                addDirectory(file_list[i].getAbsolutePath());
            else                                /* add the file with the */
                addFile(file_list[i].getAbsolutePath());  /* addFile method */
        }
    }

    ////////////////////////////////// Queries ///////////////////////////////

    /**
     * Returns a list of FileDesc matching q, or null if there are no matches.
     * Subclasses may override to provide different notions of matching.
     */
    private synchronized List search(String textQuery, String richQuery) {
        //TODO2: ideally this wouldn't be synchronized, a la ConnectionManager.
        //Doing so would allow multiple queries to proceed in parallel.  But
        //then you need to make _files volatile and work on a local reference,
        //i.e., "_files=this._files"

        // Create a GML request document from richQuery, keeping it only
        // if it is the kind of GML Document that we recognize
        GMLDocument gmlDocument = null;
        if(!richQuery.equals(""))
        {
            // Do nothing on exceptions.  Just leave gmlDocument as null.
            // This will happen whenever a query carries non-GML metadata
            try {
                gmlDocument =
                    _templateRepository.parseRequest(richQuery);
            } catch(TemplateNotFoundException e) {
            } catch(GMLParseException e) {
            }
        }

        // Scan the files list looking for matches
        ArrayList response_list=null; // Don't allocate until needed
        for(Iterator iterFiles = _files.iterator(); iterFiles.hasNext();  ) {
            FileDesc desc = (FileDesc)iterFiles.next();
            if (desc==null)
                continue;
            if(desc.isTextQueryMatch(textQuery) ||
               ((gmlDocument != null) &&
                desc.isGMLDocumentMatch(gmlDocument))) {
                if(response_list==null)
                    response_list=new ArrayList();
                response_list.add(desc);
            }
        }

        return response_list;
    }

    /** Unit test--REQUIRES JAVA2 FOR USE OF CREATETEMPFILE */
    /*
    public static void main(String args[]) {
        //Test some of add/remove capability
        File f1=null;
        File f2=null;
        File f3=null;
        try {
            f1=createNewTestFile(1);
            System.out.println("Creating temporary files in "+f1.getParent());
            FileManager fman=FileManager.instance();
            fman.setExtensions("XYZ");
            fman.addDirectory(f1.getParent());
            f2=createNewTestFile(3);
            f3=createNewTestFile(11);

            //One file
            Assert.that(fman.getNumFiles()==1, fman.getNumFiles()+"");
            Assert.that(fman.getSize()==1, fman.getSize()+"");
            Response[] responses=fman.query(new QueryRequest((byte)3,0,"unit"));
            Assert.that(responses.length==1);
            Assert.that(fman.removeFileIfShared(f3)==false);
            responses=fman.query(new QueryRequest((byte)3,0,"unit"));
            Assert.that(responses.length==1);
            Assert.that(fman.getSize()==1);
            Assert.that(fman.getNumFiles()==1);
            fman.get(0);

            //Two files
            fman.addFile(f2.getAbsolutePath());
            Assert.that(fman.getNumFiles()==2, fman.getNumFiles()+"");
            Assert.that(fman.getSize()==4, fman.getSize()+"");
            responses=fman.query(new QueryRequest((byte)3,0,"unit"));
            Assert.that(responses[0].getIndex()!=responses[1].getIndex());
            for (int i=0; i<responses.length; i++) {
                Assert.that(responses[i].getIndex()==0
                               || responses[i].getIndex()==1);
            }

            //Remove file that's shared.  Back to 1 file.
            Assert.that(fman.removeFileIfShared(f2)==true);
            Assert.that(fman.getSize()==1);
            Assert.that(fman.getNumFiles()==1);
            responses=fman.query(new QueryRequest((byte)3,0,"unit"));
            Assert.that(responses.length==1);

            fman.addFile(f3.getAbsolutePath());
            Assert.that(fman.getSize()==12, "size of files: "+fman.getSize());
            Assert.that(fman.getNumFiles()==2, "# files: "+fman.getNumFiles());
            responses=fman.query(new QueryRequest((byte)3,0,"unit"));
            Assert.that(responses.length==2, "response: "+responses.length);
            Assert.that(responses[0].getIndex()!=1);
            Assert.that(responses[1].getIndex()!=1);
            fman.get(0);
            fman.get(2);
            try {
                fman.get(1);
                Assert.that(false);
            } catch (IndexOutOfBoundsException e) { }

            responses=fman.query(new QueryRequest((byte)3,0,"*unit*"));
            Assert.that(responses.length==2, "response: "+responses.length);

        } finally {
            if (f1!=null) f1.delete();
            if (f2!=null) f2.delete();
            if (f3!=null) f3.delete();
        }
    }

    static File createNewTestFile(int size) {
        try {
            File ret=File.createTempFile("FileManager_unit_test",".XYZ");
            OutputStream out=new FileOutputStream(ret);
            out.write(new byte[size]);
            out.flush();
            out.close();
            return ret;
        } catch (Exception e) {
            System.err.println("Couldn't run test");
            e.printStackTrace();
            System.exit(1);
            return null; //never executed
        }
    }
    */
}
