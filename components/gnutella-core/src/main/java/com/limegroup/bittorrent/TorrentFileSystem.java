package com.limegroup.bittorrent;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bitzi.util.Base32;
import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.FileUtils;
import com.limegroup.gnutella.util.MultiCollection;
import com.limegroup.gnutella.util.StringUtils;

/**
 * Information about the file hierarchy contained in the torrent.
 */
public class TorrentFileSystem implements Serializable {
	
	private static final long serialVersionUID = 6006838744525690869L;

	/* the name of the torrent */
	private String _name;
	
	/*
	 * the total length of this torrent.
	 */
	private long _totalSize;
	
	/**
	 * a list of <tt>TorrentFile</tt> for every file in this torrent
	 */
	private List<TorrentFile> _files;
	
	/**
	 * any folders that are contained in the torrent
	 */
	private Collection<File> _folders = new HashSet<File>();
	
	/**
	 * a view of the files and folders contained in this torrent
	 */
	private transient Collection<File> _filesAndFolders;
	
	/*
	 * A <tt>File</tt> pointing to the location where the incomplete
	 * torrent is written.
	 */
	private File _incompleteFile;

	/*
	 * A <tt> File </tt> pointing to the file/directory where the completed
	 * torrent will be moved.
	 */
	private File _completeFile;

	
	TorrentFileSystem(Map info, int numHashes, long pieceLength, byte [] infoHash) 
	throws ValueException {
//		 name of the torrent, also specifying the directory under which to
		// save the torrents, as per extension spec, name.urf-8 specifies the
		// utf-8 name of the torrent
		String name = null;
		Object t_name = info.get("name");
		if (!(t_name instanceof byte []))
			throw new ValueException("bad metainfo - bad name");

		// if possible prefer the name.utf-8 key but we are already sure that
		// we have a safe name value to fallback to
		Object t_name_utf8 = info.get("name.utf-8");
		if (t_name_utf8 instanceof byte[]) {
			try {
				name = new String((byte [])t_name_utf8, 
						Constants.UTF_8_ENCODING);
			} catch (UnsupportedEncodingException uee) {
				// fail silently
			}
		}
		
		if (name == null)
			name = StringUtils.getASCIIString((byte [])t_name);

		_name = CommonUtils.convertFileName(name);

		// we need to check the name of the torrent, security risk!
		if (_name.length() == 0)
			throw new ValueException("bad torrent name");

		// read the files belonging to the torrent
		if (info.containsKey("files") == info.containsKey("length"))
			throw new ValueException("single/multiple file mix or neither");

		File incompleteDir = SharingSettings.INCOMPLETE_DIRECTORY.getValue();
		try {
			incompleteDir = incompleteDir.getCanonicalFile();
		} catch (IOException iox){}
		
		_incompleteFile = new File(incompleteDir, 
				Base32.encode(infoHash)+File.separator+_name);
		_completeFile = new File(SharingSettings.getSaveDirectory(), _name);
		
		if (info.containsKey("files")) {
			Object t_files = info.get("files");
			if (!(t_files instanceof List))
				throw new ValueException("bad metainfo - bad files value");
			List<TorrentFile> files = parseFiles((List) t_files, _completeFile
					.getAbsolutePath());
			if (files.size() == 0)
				throw new ValueException("bad metainfo - bad files value " + t_files);
			
			// add the beginning and ending chunks for each file.
			long position = 0;
			for (TorrentFile file : files) {
				file.setBegin((int) (position / pieceLength));
				position += file.length();
				file.setEnd((int) (position / pieceLength));
			}
			
			_files = files;
			_folders.add(_completeFile);
		} else {
			Object t_length = info.get("length");
			if (!(t_length instanceof Long))
				throw new ValueException("bad metainfo - bad file length");
			long length = ((Long) t_length).longValue();
			_files = new ArrayList<TorrentFile>(1);
			try {
				TorrentFile f = new TorrentFile(length, _completeFile
						.getCanonicalPath());
				f.setBegin(0);
				f.setEnd(numHashes);
				_files.add(f);
			} catch (IOException bad) {
				throw new ValueException("bad path");
			}
			
		}
		
		_totalSize = calculateTotalSize(_files);
	}
	
	/**
	 * Parse a list of files from the meta info
	 * 
	 * @param files a <tt>List</tt> containing the file info
	 * @param basePath the <tt>String</tt> identifying the torrent's saving folder
	 * @return List of <tt>TorrentFile</tt>
	 * @throws ValueException if parsing fails.
	 */
	private List<TorrentFile> parseFiles(List<?> files, String basePath)
			throws ValueException {
		ArrayList<TorrentFile> ret = new ArrayList<TorrentFile>();
        for(Object t_file : files) {
			if (!(t_file instanceof Map))
				throw new ValueException("bad metainfo - bad file value");
			ret.add(parseFile((Map) t_file, basePath));
		}
		return ret;
	}

	/**
	 * Utility method to parse a single file from the meta info
	 * 
	 * @param file the <tt>Map</tt> containing the file info
	 * @param basePath the <tt>String</tt> identifying the torrent
	 * @return instance of <tt>TorrentFile</tt>
	 * @throws ValueException if parsing fails.
	 */
	private TorrentFile parseFile(Map<?, ?> file, String basePath)
			throws ValueException {

		Object t_length = file.get("length");

		if (!(t_length instanceof Long))
			throw new ValueException("bad metainfo - bad file length");

		long length = ((Long) t_length).longValue();

		Object t_path = file.get("path");
		if (!(t_path instanceof List))
			throw new ValueException("bad metainfo - bad path");
		
		List<?> path = (List) t_path;
		if (path.isEmpty())
			throw new ValueException("bad metainfo - bad path");

		Object t_path_utf8 = file.get("path.utf-8");
		// the extension specs introduced the utf-8 path.
		if ( ! (t_path_utf8 instanceof List)) {
			// invalid - ignore
			t_path_utf8 = null;
		}

		StringBuffer paths = new StringBuffer(basePath);
		
		Set<File> folders = new HashSet<File>();
		
		// prefer the utf8 path if possible
		if (t_path_utf8 != null) {
			List<?> pathUtf8 = (List) t_path_utf8;
			for (Iterator<?> iter = pathUtf8.iterator(); iter.hasNext();) {
				Object t_next = iter.next();
				if ( ! (t_next instanceof byte [])) {
					// invalid UTF-8 path, fall through to asscii path
					paths = new StringBuffer(basePath);
					folders.clear();
					break; 
				}
				
				String pathElement = StringUtils.getUTF8String((byte []) t_next);
				paths.append(File.separator);
				paths.append(CommonUtils.convertFileName(pathElement));
				if (iter.hasNext())
					folders.add(new File(paths.toString()));
			}
		} 
		
		// parse asccii paths  
		if (paths.length() == basePath.length()) {
			for (int i = 0; i < path.size(); i++) {
				Object next = path.get(i);
				if (! (next instanceof byte []))
					throw new ValueException("bad paths");
				String pathElement = StringUtils.getASCIIString((byte [])next);
				paths.append(File.separator);
				paths.append(CommonUtils.convertFileName(pathElement));
				if (i < path.size() - 1)
					folders.add(new File(paths.toString()));
			}
		}
		
		_folders.addAll(folders);
		return new TorrentFile(length, paths.toString());
	}
	
	/**
	 * Accessor for the total size of this torrent
	 * 
	 * @return long size of the torrent
	 */
	public long getTotalSize() {
		return _totalSize;
	}

	/**
	 * @return the name of this torrent
	 */
	public String getName() {
		return _name;
	}

	/**
	 * @return a list of Files that will be used to write to during download
	 */
	public List<TorrentFile> getIncompleteFiles() {
		List<TorrentFile> ret = new ArrayList<TorrentFile>(_files);
		updateReferences(_incompleteFile,ret);
		return ret;
	}
	
	/**
	 * @return list of files holding the complete torrent.
	 */
	public List<TorrentFile> getFiles() {
		return _files;
	}
	
	/**
	 * @return view of the files and folders that will be created when
	 * the torrent download completes
	 */
	public Collection<File> getFilesAndFolders() {
		if (_filesAndFolders == null)
			_filesAndFolders = new MultiCollection<File>(_files,_folders);
		return _filesAndFolders;
	}
	
	/**
	 * @return true if the given file would conflict with any of the
	 * files that will be used by this torrent when it completes
	 */
	public boolean conflicts(File f) {
		return getFilesAndFolders().contains(f);
	}
	
	/**
	 * @return true if the given file conflicts with any of the files
	 * used by this torrent during download
	 */
	public boolean conflictsIncomplete(File f) {
		return getBaseFile().getParentFile().equals(f);
	}

	/**
	 * @return <tt>File</tt> the parent file for all files in this torrent.
	 */
	public File getBaseFile() {
		return _incompleteFile;
	}

	/**
	 * @return <tt>File</tt> the parent file for all files in this torrent or
	 *         the only file in this torrent
	 */
	public File getCompleteFile() {
		return _completeFile;
	}
	
	/**
	 * changes the location where this torrent will be saved.
	 */
	public void setCompleteFile(File f) {
		updateReferences(f, _files);
		updateFolderReferences(f);
		_completeFile = f;
	}
	
	/**
	 * Updates the files in the specified collection to start at the 
	 * new base path.
	 * The files must already be absolute and be children of _completeFile.
	 * 
	 * @param completeBase the new base path.
	 *
	 */
	private void updateReferences(File completeBase, List<TorrentFile> l) {
		int offset = _completeFile.getAbsolutePath().length();
		String newPath = completeBase.getAbsolutePath();
		for (int i = 0; i < l.size(); i++) {
			TorrentFile current = l.get(i);
			TorrentFile updated = new TorrentFile(current.length(), newPath
					+ current.getPath().substring(offset));
			updated.setBegin(current.getBegin());
			updated.setEnd(current.getEnd());
			l.set(i,updated);
		}
	}
	
	/**
	 * Updates the folders in the torrent to start at the 
	 * new base path.
	 */
	private void updateFolderReferences(File completeBase) {
		int offset = _completeFile.getAbsolutePath().length();
		String newPath = completeBase.getAbsolutePath();
		Collection<File> newFolders = new HashSet<File>(_folders.size());
		for (File f: _folders) 
			newFolders.add(new File(newPath + f.getPath().substring(offset)));
		_folders.clear();
		_folders.addAll(newFolders); 
	}
	
	void moveToCompleteFolder() throws IOException {
		saveFile(_incompleteFile, _completeFile);
		
		FileUtils.deleteRecursive(_incompleteFile.getParentFile());
	}
	
	/**
	 * private helper, copies file, updates FileManager and _files
	 * 
	 * @param incFile
	 *            the <tt>File</tt> from incomplete directory
	 * @param completeDest
	 *            the <tt>File</tt> the file in the save destination
	 * @throws IOException on failure
	 */
	private void saveFile(File incFile, File completeDest) throws IOException {
		completeDest = completeDest.getCanonicalFile();

		FileUtils.setWriteable(completeDest.getParentFile());
		if (incFile.isDirectory()) {
			saveDirectory(incFile, completeDest);
			return;
		}

		// overwrite complete file and make it writeable
		completeDest.delete();
		FileUtils.setWriteable(completeDest);
		if (!FileUtils.forceRename(incFile, completeDest)) 
			throw new IOException("could not rename file " + incFile);
	}

	/**
	 * Saves a directory
	 * 
	 * @param incFile
	 *            the directory to move
	 * @param completeDir
	 *            the destination to move the directory to
	 * @throws IOException if failed.
	 */
	private void saveDirectory(File incFile, File completeDir) 
	throws IOException {

		// we will delete completeDir if it exists and if it is not a directory
		if (completeDir.exists()) {
			// completeDir is not a directory
			if (!completeDir.isDirectory()) {
				if (!(completeDir.delete() && completeDir.mkdirs())) 
					throw new IOException("could not create complete dir " + completeDir);
			}
		} else if (!completeDir.mkdirs()) {
			// completeDir does not exist...
			throw new IOException("could not create complete dir " + completeDir);
		}

		FileUtils.setWriteable(completeDir);

		for (File f : incFile.listFiles()) 
			saveFile(f, new File(completeDir, f.getName())); 

		// remove the empty directory from the incomplete folder.
		// all its contents have been moved to the shared folder
		FileUtils.deleteRecursive(incFile);
	}
	
	void addToLibrary() {
		for (File f : _folders)
			RouterService.getFileManager().addSharedFolder(f);
		for (File f : _files)
			RouterService.getFileManager().addFileIfShared(f);
	}
	
	private static long calculateTotalSize(List<TorrentFile> files) {
		long ret = 0;
		for (File f : files)
			ret += f.length();
		return ret;
	}
}