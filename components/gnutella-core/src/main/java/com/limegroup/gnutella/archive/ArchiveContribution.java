package com.limegroup.gnutella.archive;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.io.CopyStreamException;
import org.apache.xerces.dom3.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

import com.limegroup.gnutella.util.CommonUtils;



abstract class ArchiveContribution extends AbstractContribution {
	
	/**
	 * @param media
	 *        One of ArchiveConstants.MEDIA_*
	 *        
	 * @throws IllegalArgumentException
	 *         If media is not valid
	 */
	public ArchiveContribution( String username, String password, 
			String title, String description, int media )
	throws DescriptionTooShortException {
		this( username, password, title, description, media, 
				Archives.defaultCollectionForMedia( media ),
				Archives.defaultTypesForMedia( media ));
	}
	
	public ArchiveContribution( String username, String password, 
			String title, String description, int media, 
			int collection, int type ) 
	throws DescriptionTooShortException {
		setUsername( username );
		setPassword( password );
		setTitle( title );
		setDescription( description );
		setMedia( media );
		setCollection( collection );	
		setType( type );
	}

	
	abstract protected String getFtpServer();
	abstract protected String getFtpPath();
	abstract protected boolean isFtpDirPreMade();
	
	abstract protected void checkin() throws IOException;

	
	/**
	 * 
	 * @throws UnknownHostException
	 *         If the hostname cannot be resolved.
	 *         
	 * @throws SocketException
	 *         If the socket timeout could not be set.
	 *         
	 * @throws FTPConnectionClosedException
	 *         If the connection is closed by the server.
	 *         
	 * @throws LoginFailedException
	 *         If the login fails.
	 *         
	 * @throws DirectoryChangeFailedException
	 *         If changing to the directory provided by the internet
	 *         archive fails.
	 *         
	 * @throws CopyStreamException
	 *         If an I/O error occurs while in the middle of
	 *         transferring a file.
	 *         
	 * @throws IOException
	 *         If an I/O error occurs while sending a command or
	 *         receiving a reply from the server
	 *         
	 * @throws IllegalStateException
	 * 		   If the contribution object is not ready to upload
	 * 		   (no username, password, server, etc. set)
	 * 		   or if java's xml parser is configured badly
	 */
	
	public void upload() throws UnknownHostException, SocketException, 
	FTPConnectionClosedException, LoginFailedException,
	DirectoryChangeFailedException, CopyStreamException, 
	RefusedConnectionException, IOException {
		
		final int  NUM_XML_FILES = 2;
		final String META_XML_SUFFIX = "_meta.xml";
		final String FILES_XML_SUFFIX = "_files.xml";
		
		
		final String username = getUsername();
		final String password = getPassword();
		
		if ( getFtpServer() == null ) {
			throw new IllegalStateException( "ftp server not set" );
		}
		if ( getFtpPath() == null ) {
			throw new IllegalStateException( "ftp path not set" );
		}
		if ( username == null ) {
			throw new IllegalStateException( "username not set" );			
		}
		if ( password == null ) {
			throw new IllegalStateException( "password not set" );
		}
		
		// calculate total number of files and bytes
		
		
		
		final String metaXmlString = serializeDocument( getMetaDocument() );
		final String filesXmlString = serializeDocument( getFilesDocument() );
		
		final byte[] metaXmlBytes = metaXmlString.getBytes();
		final byte[] filesXmlBytes = filesXmlString.getBytes();
		
		final int metaXmlLength = metaXmlBytes.length;
		final int filesXmlLength = filesXmlBytes.length;
		
		final Collection<ArchiveFile> files = getFiles();
		
		final int totalFiles = NUM_XML_FILES + files.size();
		
		final String[] fileNames = new String[totalFiles];
		final long[] fileSizes = new long[totalFiles];
		
		final String metaXmlName = getIdentifier() + META_XML_SUFFIX; 
		fileNames[0] = metaXmlName;
		fileSizes[0] = metaXmlLength;
		
		final String filesXmlName = getIdentifier() + FILES_XML_SUFFIX;
		fileNames[1] = filesXmlName;
		fileSizes[1] = filesXmlLength;
		
		int j = 2;
        for(ArchiveFile f : files) {
			fileNames[j] = f.getRemoteFileName();
			fileSizes[j] = f.getFileSize();
			j++;
		}

        // init the progress mapping
        for (int i = 0; i < fileSizes.length; i++) { 
            _fileNames2Progress.put(fileNames[i],new UploadFileProgress(fileSizes[i]));
            _totalUploadSize+=fileSizes[i];
        }
		
		
		FTPClient ftp = new FTPClient();
		
		try {
			// first connect
			
			if ( isCancelled() ) { return; }
			ftp.enterLocalPassiveMode();
			
			if ( isCancelled() ) { return; }
			ftp.connect( getFtpServer() );
			
			final int reply = ftp.getReplyCode();
			if ( !FTPReply.isPositiveCompletion(reply) ) {
				throw new RefusedConnectionException( getFtpServer() + "refused FTP connection" );
			}
			// now login
			if ( isCancelled() ) { return; }
			if (!ftp.login( username, password )) {
				throw new LoginFailedException();
			}
			
			try {
				
                // try to change the directory
                if (!ftp.changeWorkingDirectory(getFtpPath())) {
                    // if changing fails, make the directory
                    if ( !isFtpDirPreMade() &&
                            !ftp.makeDirectory( getFtpPath() )) {
                        throw new DirectoryChangeFailedException();
                    }
                    
                    // now change directory, if it fails again bail
                    if ( isCancelled() ) { return; }
                    if (!ftp.changeWorkingDirectory( getFtpPath() )) {
                        throw new DirectoryChangeFailedException();
                    }
                }
				
				if ( isCancelled() ) { return; }
				connected();
				
				
				// upload xml files
				uploadFile( metaXmlName,
						new ByteArrayInputStream( metaXmlBytes ),
						ftp);
				
				uploadFile( filesXmlName,
						new ByteArrayInputStream( filesXmlBytes ),
						ftp);
				
				// now switch to binary mode
				if ( isCancelled() ) { return; }
				ftp.setFileType( FTP.BINARY_FILE_TYPE );
				
				// upload contributed files
                for(ArchiveFile f : files) {
					uploadFile( f.getRemoteFileName(), 
							new FileInputStream( f.getIOFile() ),
							ftp);
				}
			} catch( InterruptedIOException ioe ) {
				// we've been requested to cancel
				return;
			} finally {
				ftp.logout(); // we don't care if logging out fails
			}
		} finally {
			try{
				ftp.disconnect();	
			} catch( IOException e ) {}  // don't care if disconnecting fails
		}		

		// now tell the Internet Archive that we're done
		if ( isCancelled() ) { return; }
		checkinStarted();

		if ( isCancelled() ) { return; }		
		checkin();
		
		if ( isCancelled() ) { return; }		
		checkinCompleted();
	}
	

	
	/**
	 * 
	 * @param fileName
	 * @param input
	 * 		  The input stream (not necessarily buffered).
	 *        This stream will be closed by this method
	 */
	private void uploadFile( String remoteFileName, 
			InputStream input, FTPClient ftp)
	throws InterruptedIOException, IOException {
		fileStarted( remoteFileName );
		final InputStream fileStream = new BufferedInputStream(
				new UploadMonitorInputStream( input, this));
		
		try {
			if ( isCancelled() ) {
				throw new InterruptedIOException();
			}
			
			ftp.storeFile( remoteFileName, fileStream );
		} finally {
			fileStream.close();
		}

		if ( isCancelled() ) {
			throw new InterruptedIOException();
		}
		fileCompleted();
	}

	
	
	/**
	 * @throws 	IllegalStateException
	 * 			If java's xml parser configuration is bad
	 */
	private Document getMetaDocument() {
		
		/*
		 * Sample _meta.xml file:
		 * 
		 * <metadata>
		 *   <collection>opensource_movies</collection>
		 *   <mediatype>movies</mediatype>
		 *   <title>My Home Movie</title>
		 *   <runtime>2:30</runtime>
		 *   <director>Joe Producer</director>
		 * </metadata>    
		 * 
		 */
		
		final String METADATA_ELEMENT = "metadata";
		final String COLLECTION_ELEMENT = "collection";
		final String MEDIATYPE_ELEMENT = "mediatype";
		final String TITLE_ELEMENT = "title";
		final String DESCRIPTION_ELEMENT = "description";
		final String LICENSE_URL_ELEMENT = "licenseurl";
		final String UPLOAD_APPLICATION_ELEMENT = "upload_application";
		final String APPID_ATTR = "appid";
		final String APPID_ATTR_VALUE = "LimeWire";
		final String VERSION_ATTR = "version";
		final String UPLOADER_ELEMENT = "uploader";
		final String IDENTIFIER_ELEMENT = "identifier";
		final String TYPE_ELEMENT = "type";
		
		try {
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			final DocumentBuilder db = dbf.newDocumentBuilder();
			final Document document = db.newDocument();
			
			final Element metadataElement = document.createElement(METADATA_ELEMENT);
			document.appendChild( metadataElement );
			
			final Element collectionElement = document.createElement( COLLECTION_ELEMENT );
			metadataElement.appendChild( collectionElement );
			collectionElement.appendChild( document.createTextNode( Archives.getCollectionString( getCollection())));
			
			final Element mediatypeElement = document.createElement(MEDIATYPE_ELEMENT);
			metadataElement.appendChild( mediatypeElement );
			mediatypeElement.appendChild( document.createTextNode( Archives.getMediaString( getMedia() )));
			
			final Element typeElement = document.createElement(TYPE_ELEMENT);
			metadataElement.appendChild( typeElement );
			typeElement.appendChild( document.createTextNode( Archives.getTypeString( getType() )));
			
			final Element titleElement = document.createElement( TITLE_ELEMENT );
			metadataElement.appendChild( titleElement );
			titleElement.appendChild( document.createTextNode( getTitle()));
			
			final Element descriptionElement = document.createElement( DESCRIPTION_ELEMENT );
			metadataElement.appendChild( descriptionElement );
			descriptionElement.appendChild( document.createTextNode( getDescription() ));
			
			final Element identifierElement = document.createElement( IDENTIFIER_ELEMENT );
			metadataElement.appendChild( identifierElement );
			identifierElement.appendChild( document.createTextNode( getIdentifier() ));
			
			final Element uploadApplicationElement = document.createElement( UPLOAD_APPLICATION_ELEMENT );
			metadataElement.appendChild( uploadApplicationElement );
			uploadApplicationElement.setAttribute( APPID_ATTR, APPID_ATTR_VALUE );
			uploadApplicationElement.setAttribute( VERSION_ATTR, CommonUtils.getLimeWireVersion() );
			
			final Element uploaderElement = document.createElement( UPLOADER_ELEMENT );
			metadataElement.appendChild( uploaderElement );
			uploaderElement.appendChild( document.createTextNode( getUsername() ));
			
			//take licenseurl from the first File
			
			for(ArchiveFile firstFile : getFiles()) {
				final String licenseUrl = firstFile.getLicenseUrl();
				if ( licenseUrl != null ) {
					final Element licenseUrlElement = document.createElement( LICENSE_URL_ELEMENT );
					metadataElement.appendChild( licenseUrlElement );
					licenseUrlElement.appendChild( document.createTextNode( licenseUrl ));
				}
			}
			
			// now build user-defined elements
            for (Map.Entry<String, String> entry : getFields().entrySet()) {
                String field = entry.getKey();
                final String value = entry.getValue();
                final Element e = document.createElement(field);
                metadataElement.appendChild(e);

                if (value != null)
                    e.appendChild(document.createTextNode(value));
            }
			
			return document;
			
		} catch (final ParserConfigurationException e) {
			final IllegalStateException ise = new IllegalStateException();
			ise.initCause( e );
			throw ise;
		}	
	}
	


	
	/**
	 * @throws 	IllegalStateException
	 * 			If java's xml configuration is bad
	 * @return
	 */
	
	private Document getFilesDocument() {
		/*
		 * Sample _files.xml file:
		 * 
		 * <files>
		 *   <file name="MyHomeMovie.mpeg" source="original">
	     *     <runtime>2:30</runtime>
	     *     <format>MPEG2</format>
	     *   </file>
	     * </files>    
		 * 
		 */
		
		final String FILES_ELEMENT = "files";
		
		try {
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			final DocumentBuilder db = dbf.newDocumentBuilder();
			final Document document = db.newDocument();
			
			final Element filesElement = document.createElement( FILES_ELEMENT );
			document.appendChild( filesElement );
			
			Collection<ArchiveFile> files = getFiles();
			
            for(ArchiveFile file : files) {
				final Element fileElement = file.getElement( document );
				filesElement.appendChild( fileElement );
			}
			
			return document;
			
		} catch (final ParserConfigurationException e) {
			final IllegalStateException ise = new IllegalStateException();
			ise.initCause( e );
			throw ise;
		}	
		
	}
	
	/**
	 * @throws	IllegalStateException
	 * 			If java's DOMImplementationLS class is screwed up or
	 * 			this code is buggy
	 * @param document
	 * @return
	 */
	private String serializeDocument( Document document ) {
		
		try {
			System.setProperty(DOMImplementationRegistry.PROPERTY,
			"org.apache.xerces.dom.DOMImplementationSourceImpl");
			final DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
			
			final DOMImplementationLS impl = 
				(DOMImplementationLS)registry.getDOMImplementation("LS");
			
			final LSSerializer writer = impl.createLSSerializer();
			final String str = writer.writeToString( document );
			
			return str;
			
		} catch (final ClassNotFoundException e) {
			final IllegalStateException ise = new IllegalStateException();
			ise.initCause( e );
			throw ise;
		} catch (final InstantiationException e) {
			final IllegalStateException ise = new IllegalStateException();
			ise.initCause( e );
			throw ise;
		} catch (final IllegalAccessException e) {
			final IllegalStateException ise = new IllegalStateException();
			ise.initCause( e );
			throw ise;
		}
		
	}
	
}
