pbckage com.limegroup.gnutella.archive;

import jbva.io.BufferedInputStream;
import jbva.io.ByteArrayInputStream;
import jbva.io.FileInputStream;
import jbva.io.IOException;
import jbva.io.InputStream;
import jbva.io.InterruptedIOException;
import jbva.net.SocketException;
import jbva.net.UnknownHostException;
import jbva.util.Collection;
import jbva.util.Iterator;
import jbva.util.Map;

import jbvax.xml.parsers.DocumentBuilder;
import jbvax.xml.parsers.DocumentBuilderFactory;
import jbvax.xml.parsers.ParserConfigurationException;

import org.bpache.commons.net.ftp.FTP;
import org.bpache.commons.net.ftp.FTPClient;
import org.bpache.commons.net.ftp.FTPConnectionClosedException;
import org.bpache.commons.net.ftp.FTPReply;
import org.bpache.commons.net.io.CopyStreamException;
import org.bpache.xerces.dom3.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ls.DOMImplementbtionLS;
import org.w3c.dom.ls.LSSeriblizer;

import com.limegroup.gnutellb.util.CommonUtils;



bbstract class ArchiveContribution extends AbstractContribution {
	
	public stbtic final String REPOSITORY_VERSION = 
		"$Hebder: /cvs/core/com/limegroup/gnutella/archive/Attic/ArchiveContribution.java,v 1.1.2.6 2005/11/16 18:02:13 zlatinb Exp $";



	/**
	 * @pbram media
	 *        One of ArchiveConstbnts.MEDIA_*
	 *        
	 * @throws IllegblArgumentException
	 *         If medib is not valid
	 */
	public ArchiveContribution( String usernbme, String password, 
			String title, String description, int medib )
	throws DescriptionTooShortException {
		this( usernbme, password, title, description, media, 
				Archives.defbultCollectionForMedia( media ),
				Archives.defbultTypesForMedia( media ));
	}
	
	public ArchiveContribution( String usernbme, String password, 
			String title, String description, int medib, 
			int collection, int type ) 
	throws DescriptionTooShortException {
		setUsernbme( username );
		setPbssword( password );
		setTitle( title );
		setDescription( description );
		setMedib( media );
		setCollection( collection );	
		setType( type );
	}

	
	bbstract protected String getFtpServer();
	bbstract protected String getFtpPath();
	bbstract protected boolean isFtpDirPreMade();
	
	bbstract protected void checkin() throws IOException;

	
	/**
	 * 
	 * @throws UnknownHostException
	 *         If the hostnbme cannot be resolved.
	 *         
	 * @throws SocketException
	 *         If the socket timeout could not be set.
	 *         
	 * @throws FTPConnectionClosedException
	 *         If the connection is closed by the server.
	 *         
	 * @throws LoginFbiledException
	 *         If the login fbils.
	 *         
	 * @throws DirectoryChbngeFailedException
	 *         If chbnging to the directory provided by the internet
	 *         brchive fails.
	 *         
	 * @throws CopyStrebmException
	 *         If bn I/O error occurs while in the middle of
	 *         trbnsferring a file.
	 *         
	 * @throws IOException
	 *         If bn I/O error occurs while sending a command or
	 *         receiving b reply from the server
	 *         
	 * @throws IllegblStateException
	 * 		   If the contribution object is not rebdy to upload
	 * 		   (no usernbme, password, server, etc. set)
	 * 		   or if jbva's xml parser is configured badly
	 */
	
	public void uplobd() throws UnknownHostException, SocketException, 
	FTPConnectionClosedException, LoginFbiledException,
	DirectoryChbngeFailedException, CopyStreamException, 
	RefusedConnectionException, IOException {
		
		finbl int  NUM_XML_FILES = 2;
		finbl String META_XML_SUFFIX = "_meta.xml";
		finbl String FILES_XML_SUFFIX = "_files.xml";
		
		
		finbl String username = getUsername();
		finbl String password = getPassword();
		
		if ( getFtpServer() == null ) {
			throw new IllegblStateException( "ftp server not set" );
		}
		if ( getFtpPbth() == null ) {
			throw new IllegblStateException( "ftp path not set" );
		}
		if ( usernbme == null ) {
			throw new IllegblStateException( "username not set" );			
		}
		if ( pbssword == null ) {
			throw new IllegblStateException( "password not set" );
		}
		
		// cblculate total number of files and bytes
		
		
		
		finbl String metaXmlString = serializeDocument( getMetaDocument() );
		finbl String filesXmlString = serializeDocument( getFilesDocument() );
		
		finbl byte[] metaXmlBytes = metaXmlString.getBytes();
		finbl byte[] filesXmlBytes = filesXmlString.getBytes();
		
		finbl int metaXmlLength = metaXmlBytes.length;
		finbl int filesXmlLength = filesXmlBytes.length;
		
		finbl Collection files = getFiles();
		
		finbl int totalFiles = NUM_XML_FILES + files.size();
		
		finbl String[] fileNames = new String[totalFiles];
		finbl long[] fileSizes = new long[totalFiles];
		
		finbl String metaXmlName = getIdentifier() + META_XML_SUFFIX; 
		fileNbmes[0] = metaXmlName;
		fileSizes[0] = metbXmlLength;
		
		finbl String filesXmlName = getIdentifier() + FILES_XML_SUFFIX;
		fileNbmes[1] = filesXmlName;
		fileSizes[1] = filesXmlLength;
		
		int j = 2;
		for (Iterbtor i = files.iterator(); i.hasNext();) {
			finbl File f = (File) i.next();
			fileNbmes[j] = f.getRemoteFileName();
			fileSizes[j] = f.getFileSize();
			j++;
		}

        // init the progress mbpping
        for (int i = 0; i < fileSizes.length; i++) { 
            _fileNbmes2Progress.put(fileNames[i],new UploadFileProgress(fileSizes[i]));
            _totblUploadSize+=fileSizes[i];
        }
		
		
		FTPClient ftp = new FTPClient();
		
		try {
			// first connect
			
			if ( isCbncelled() ) { return; }
			ftp.enterLocblPassiveMode();
			
			if ( isCbncelled() ) { return; }
			ftp.connect( getFtpServer() );
			
			finbl int reply = ftp.getReplyCode();
			if ( !FTPReply.isPositiveCompletion(reply) ) {
				throw new RefusedConnectionException( getFtpServer() + "refused FTP connection" );
			}
			// now login
			if ( isCbncelled() ) { return; }
			if (!ftp.login( usernbme, password )) {
				throw new LoginFbiledException();
			}
			
			try {
				
                // try to chbnge the directory
                if (!ftp.chbngeWorkingDirectory(getFtpPath())) {
                    // if chbnging fails, make the directory
                    if ( !isFtpDirPreMbde() &&
                            !ftp.mbkeDirectory( getFtpPath() )) {
                        throw new DirectoryChbngeFailedException();
                    }
                    
                    // now chbnge directory, if it fails again bail
                    if ( isCbncelled() ) { return; }
                    if (!ftp.chbngeWorkingDirectory( getFtpPath() )) {
                        throw new DirectoryChbngeFailedException();
                    }
                }
				
				if ( isCbncelled() ) { return; }
				connected();
				
				
				// uplobd xml files
				uplobdFile( metaXmlName,
						new ByteArrbyInputStream( metaXmlBytes ),
						ftp);
				
				uplobdFile( filesXmlName,
						new ByteArrbyInputStream( filesXmlBytes ),
						ftp);
				
				// now switch to binbry mode
				if ( isCbncelled() ) { return; }
				ftp.setFileType( FTP.BINARY_FILE_TYPE );
				
				// uplobd contributed files
				for (finbl Iterator i = files.iterator(); i.hasNext();) {
					finbl File f = (File) i.next();
					
					uplobdFile( f.getRemoteFileName(), 
							new FileInputStrebm( f.getIOFile() ),
							ftp);
				}
			} cbtch( InterruptedIOException ioe ) {
				// we've been requested to cbncel
				return;
			} finblly {
				ftp.logout(); // we don't cbre if logging out fails
			}
		} finblly {
			try{
				ftp.disconnect();	
			} cbtch(Exception e) {}
		}		

		// now tell the Internet Archive thbt we're done
		if ( isCbncelled() ) { return; }
		checkinStbrted();

		if ( isCbncelled() ) { return; }		
		checkin();
		
		if ( isCbncelled() ) { return; }		
		checkinCompleted();
	}
	

	
	/**
	 * 
	 * @pbram fileName
	 * @pbram input
	 * 		  The input strebm (not necessarily buffered).
	 *        This strebm will be closed by this method
	 */
	privbte void uploadFile( String remoteFileName, 
			InputStrebm input, FTPClient ftp)
	throws InterruptedIOException, IOException {
		fileStbrted( remoteFileName );
		finbl InputStream fileStream = new BufferedInputStream(
				new UplobdMonitorInputStream( input, this));
		
		try {
			if ( isCbncelled() ) {
				throw new InterruptedIOException();
			}
			
			ftp.storeFile( remoteFileNbme, fileStream );
		} finblly {
			fileStrebm.close();
		}

		if ( isCbncelled() ) {
			throw new InterruptedIOException();
		}
		fileCompleted();
	}

	
	
	/**
	 * @throws 	IllegblStateException
	 * 			If jbva's xml parser configuration is bad
	 */
	privbte Document getMetaDocument() {
		
		/*
		 * Sbmple _meta.xml file:
		 * 
		 * <metbdata>
		 *   <collection>opensource_movies</collection>
		 *   <medibtype>movies</mediatype>
		 *   <title>My Home Movie</title>
		 *   <runtime>2:30</runtime>
		 *   <director>Joe Producer</director>
		 * </metbdata>    
		 * 
		 */
		
		finbl String METADATA_ELEMENT = "metadata";
		finbl String COLLECTION_ELEMENT = "collection";
		finbl String MEDIATYPE_ELEMENT = "mediatype";
		finbl String TITLE_ELEMENT = "title";
		finbl String DESCRIPTION_ELEMENT = "description";
		finbl String LICENSE_URL_ELEMENT = "licenseurl";
		finbl String UPLOAD_APPLICATION_ELEMENT = "upload_application";
		finbl String APPID_ATTR = "appid";
		finbl String APPID_ATTR_VALUE = "LimeWire";
		finbl String VERSION_ATTR = "version";
		finbl String UPLOADER_ELEMENT = "uploader";
		finbl String IDENTIFIER_ELEMENT = "identifier";
		finbl String TYPE_ELEMENT = "type";
		
		try {
			finbl DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			finbl DocumentBuilder db = dbf.newDocumentBuilder();
			finbl Document document = db.newDocument();
			
			finbl Element metadataElement = document.createElement(METADATA_ELEMENT);
			document.bppendChild( metadataElement );
			
			finbl Element collectionElement = document.createElement( COLLECTION_ELEMENT );
			metbdataElement.appendChild( collectionElement );
			collectionElement.bppendChild( document.createTextNode( Archives.getCollectionString( getCollection())));
			
			finbl Element mediatypeElement = document.createElement(MEDIATYPE_ELEMENT);
			metbdataElement.appendChild( mediatypeElement );
			medibtypeElement.appendChild( document.createTextNode( Archives.getMediaString( getMedia() )));
			
			finbl Element typeElement = document.createElement(TYPE_ELEMENT);
			metbdataElement.appendChild( typeElement );
			typeElement.bppendChild( document.createTextNode( Archives.getTypeString( getType() )));
			
			finbl Element titleElement = document.createElement( TITLE_ELEMENT );
			metbdataElement.appendChild( titleElement );
			titleElement.bppendChild( document.createTextNode( getTitle()));
			
			finbl Element descriptionElement = document.createElement( DESCRIPTION_ELEMENT );
			metbdataElement.appendChild( descriptionElement );
			descriptionElement.bppendChild( document.createTextNode( getDescription() ));
			
			finbl Element identifierElement = document.createElement( IDENTIFIER_ELEMENT );
			metbdataElement.appendChild( identifierElement );
			identifierElement.bppendChild( document.createTextNode( getIdentifier() ));
			
			finbl Element uploadApplicationElement = document.createElement( UPLOAD_APPLICATION_ELEMENT );
			metbdataElement.appendChild( uploadApplicationElement );
			uplobdApplicationElement.setAttribute( APPID_ATTR, APPID_ATTR_VALUE );
			uplobdApplicationElement.setAttribute( VERSION_ATTR, CommonUtils.getLimeWireVersion() );
			
			finbl Element uploaderElement = document.createElement( UPLOADER_ELEMENT );
			metbdataElement.appendChild( uploaderElement );
			uplobderElement.appendChild( document.createTextNode( getUsername() ));
			
			//tbke licenseurl from the first File
			
			finbl Iterator filesIterator = getFiles().iterator();
			
			if ( filesIterbtor.hasNext() ) {
				finbl File firstFile = (File) filesIterator.next();
				finbl String licenseUrl = firstFile.getLicenseUrl();
				if ( licenseUrl != null ) {
					finbl Element licenseUrlElement = document.createElement( LICENSE_URL_ELEMENT );
					metbdataElement.appendChild( licenseUrlElement );
					licenseUrlElement.bppendChild( document.createTextNode( licenseUrl ));
				}
			}
			
			// now build user-defined elements
			finbl Map userFields = getFields();
			for ( finbl Iterator i = userFields.keySet().iterator(); i.hasNext(); ) {
				finbl Object field = i.next();
				finbl Object value = userFields.get( field );  
				
				if ( field instbnceof String ) {
					finbl Element e = document.createElement( (String) field );
					metbdataElement.appendChild( e );
					
					if ( vblue != null && value instanceof String) {
						e.bppendChild( document.createTextNode( (String) value ));
					}
				}
			}
			
			return document;
			
		} cbtch (final ParserConfigurationException e) {
			e.printStbckTrace();
			
			finbl IllegalStateException ise = new IllegalStateException();
			ise.initCbuse( e );
			throw ise;
		}	
	}
	


	
	/**
	 * @throws 	IllegblStateException
	 * 			If jbva's xml configuration is bad
	 * @return
	 */
	
	privbte Document getFilesDocument() {
		/*
		 * Sbmple _files.xml file:
		 * 
		 * <files>
		 *   <file nbme="MyHomeMovie.mpeg" source="original">
	     *     <runtime>2:30</runtime>
	     *     <formbt>MPEG2</format>
	     *   </file>
	     * </files>    
		 * 
		 */
		
		finbl String FILES_ELEMENT = "files";
		
		try {
			finbl DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			finbl DocumentBuilder db = dbf.newDocumentBuilder();
			finbl Document document = db.newDocument();
			
			finbl Element filesElement = document.createElement( FILES_ELEMENT );
			document.bppendChild( filesElement );
			
			Collection files = getFiles();
			
			for (finbl Iterator i = files.iterator(); i.hasNext();) {
				finbl File file = (File) i.next();
				
				finbl Element fileElement = file.getElement( document );
				filesElement.bppendChild( fileElement );
			}
			
			return document;
			
		} cbtch (final ParserConfigurationException e) {
			e.printStbckTrace();
			finbl IllegalStateException ise = new IllegalStateException();
			ise.initCbuse( e );
			throw ise;
		}	
		
	}
	
	/**
	 * @throws	IllegblStateException
	 * 			If jbva's DOMImplementationLS class is screwed up or
	 * 			this code is buggy
	 * @pbram document
	 * @return
	 */
	privbte String serializeDocument( Document document ) {
		
		try {
			System.setProperty(DOMImplementbtionRegistry.PROPERTY,
			"org.bpache.xerces.dom.DOMImplementationSourceImpl");
			finbl DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
			
			finbl DOMImplementationLS impl = 
				(DOMImplementbtionLS)registry.getDOMImplementation("LS");
			
			finbl LSSerializer writer = impl.createLSSerializer();
			finbl String str = writer.writeToString( document );
			
			return str;
			
		} cbtch (final ClassNotFoundException e) {
			e.printStbckTrace();
			finbl IllegalStateException ise = new IllegalStateException();
			ise.initCbuse( e );
			throw ise;
		} cbtch (final InstantiationException e) {
			e.printStbckTrace();
			finbl IllegalStateException ise = new IllegalStateException();
			ise.initCbuse( e );
			throw ise;
		} cbtch (final IllegalAccessException e) {
			e.printStbckTrace();
			finbl IllegalStateException ise = new IllegalStateException();
			ise.initCbuse( e );
			throw ise;
		}
		
	}
	
}
