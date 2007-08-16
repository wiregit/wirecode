package com.limegroup.gnutella.archive;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.limewire.util.CommonUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.licenses.License;
import com.limegroup.gnutella.licenses.LicenseFactory;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLNames;
import com.limegroup.gnutella.xml.LimeXMLUtils;

class ArchiveFile {

	/*
	 * From http://www.archive.org/help/contrib-advanced.php:
	 * 
	 * The format name is very important to choose accurately. The list of
	 * acceptable format names is: WAVE, 64Kbps MP3, 128Kbps MP3, 256Kbps MP3,
	 * VBR MP3, 96Kbps MP3, 160Kbps MP3, 192Kbps MP3, Ogg Vorbis, Shorten, Flac,
	 * 24bit Flac, 64Kbps M3U, VBR M3U, Single Page Original JPEG Tar, Single
	 * Page Processed JPEG Tar, Single Page Original JPEG ZIP, Single Page
	 * Processed JPEG ZIP, Single Page Original TIFF ZIP, Single Page Processed
	 * TIFF ZIP, Checksums, MPEG2, MPEG1, 64Kb MPEG4, 256Kb MPEG4, MPEG4, 56Kb
	 * QuickTime, 64Kb QuickTime, 256Kb QuickTime, QuickTime, Motion JPEG, DivX,
	 * IV50, Windows Media, Cinepack, Flash, Real Media, Item Image, Collection
	 * Header, Animated GIF, Animated GIF Images, Thumbnail, JPEG, Single Page
	 * Original JPEG, Single Page Processed JPEG, Single Page Original TIFF,
	 * Single Page Processed TIFF, Multi Page Original TIFF, Multi Page
	 * Processed TIFF, PDF, DjVuTXT, DjVuXML, Flippy Index, DjVu, Single Page
	 * Processed JPEG Tar, Single Page Original JPEG Tar, Flippy ZIP, Text,
	 * Single Book Page Text, TGZiped Text Files, Book Cover, DAT, ARC,
	 * Metadata, Files Metadata, Item Metadata, Book Metadata. This list is
	 * dynamically generated so check back later for newly acceptable formats.
	 */

	private static final Map<Integer, String> _mp3Formats = new HashMap<Integer, String>();

	static {

		/*
		 * Weird. looks like if you can only submit constant bit rate MP3s with
		 * these bitrates
		 */
		final int[] bitRates = { 64, 96, 128, 160, 192, 256 };

		for (int i = 0; i < bitRates.length; i++) {
			final Integer bitRate = new Integer(bitRates[i]);
			_mp3Formats.put(bitRate, bitRate.toString() + "Kbps MP3");
		}
	}

	private static final String MP3_VBR = "VBR MP3";

	private static final String OGG_VORBIS = "Ogg Vorbis";

	
	private final FileDesc _fd;

	private String _format;
	private String _runtime;
	private String _licenseUrl;
	private String _licenseDeclaration;
	
	private Element _element;
	
	private final String _remoteFileName;  // normalized

	/*
	 * @throws UnsupportedFormatException
	 */
	ArchiveFile(FileDesc fd) {
		_fd = fd;
		
		final LimeXMLDocument xmlDoc = _fd.getXMLDocument();

		final String fileName = _fd.getFileName();
		
		_remoteFileName = Archives.normalizeName( fileName );

		// set the format
		if (LimeXMLUtils.isMP3File(fileName)) {
			// check the bitrate
			
			try {
				final String bitRateStr = xmlDoc.getValue(LimeXMLNames.AUDIO_BITRATE);
				
				if ( bitRateStr != null ) {
					final Integer bitRate = Integer.valueOf( bitRateStr );
					
					if (_mp3Formats.get( bitRate ) != null ) {
						_format = _mp3Formats.get( bitRate );
					} 
				}
			} catch (NumberFormatException e) {
			}
			
			if (_format == null ) {
				// I guess we'll just assume it's a VBR then
				_format = MP3_VBR;
			}
			
			
			
		} else if (LimeXMLUtils.isOGGFile(fileName)) {
			_format = OGG_VORBIS;
		} else {
			// Houston, we have a problem
			throw new UnsupportedFormatException();
		}
		
		// set the runtime 
		
		try {
			final String secondsStr = xmlDoc.getValue(LimeXMLNames.AUDIO_SECONDS);
			
			if ( secondsStr != null ) {
				final int seconds = Integer.parseInt(secondsStr);
				_runtime = CommonUtils.seconds2time( seconds );
			}
			
		} catch (NumberFormatException e) {
		}
		
		// set the licenseUrl and licenseDeclaration
		
		if ( xmlDoc.isLicenseAvailable() ) {
			final License license = xmlDoc.getLicense();
			
			if ( license.getLicenseName() == LicenseFactory.CC_NAME ) {
				_licenseUrl = license.getLicenseDeed(null).toString(); 
				_licenseDeclaration = xmlDoc.getLicenseString();
			}
		}
	}
	
	String getLicenseUrl() {
		return _licenseUrl;
	}
	
	String getLicenseDeclaration() {
		return _licenseDeclaration;
	}
	
	String getLocalFileName() {
		return _fd.getFileName();
	}
	
	String getRemoteFileName() {
		return _remoteFileName;
	}
	
	long getFileSize() {
		return _fd.getFileSize();
	}
	
	File getIOFile() {
		return _fd.getFile();
	}
	

	

	/**
	 * 
	 * @param document
	 * 	      root document for generating the element
	 * @return
	 */
	
	Element getElement( Document document ) {
		/*
		 * Sample XML representation:
		 * 
		 *   <file name="MyHomeMovie.mpeg" source="original">
		 *     <runtime>2:30</runtime>
		 *     <format>MPEG2</format>
		 *   </file>
		 */
		
		final String FILE_ELEMENT = "file";
		final String NAME_ATTR = "name";
		final String SOURCE_ATTR = "source";
		final String SOURCE_ATTR_DEFAULT_VALUE = "original";
		final String RUNTIME_ELEMENT = "runtime";
		final String FORMAT_ELEMENT = "format";
		final String LICENSE_ELEMENT = "license";

		
		if ( _element == null ) {
		
				
				final Element fileElement = document.createElement(FILE_ELEMENT);
				
				
				fileElement.setAttribute( NAME_ATTR, getRemoteFileName());
				fileElement.setAttribute( SOURCE_ATTR, SOURCE_ATTR_DEFAULT_VALUE );
				
				if ( _runtime != null ) {
					final Element runtimeElement = document.createElement(RUNTIME_ELEMENT);
					runtimeElement.appendChild( document.createTextNode( _runtime ));
					fileElement.appendChild( runtimeElement );
				}
				
				// _format should not be null (otherwise we would have thrown
				// an UnsupportedFormatException upon construction)
				final Element formatElement = document.createElement(FORMAT_ELEMENT);
				formatElement.appendChild( document.createTextNode( _format ));
				fileElement.appendChild( formatElement );
				
				// defacto standard due to ccPublisher.  each <file> has
				// a child <license> element with its text set to be
				// the license declaration
				
				final String licenseDeclaration = getLicenseDeclaration(); 
				
				if ( licenseDeclaration != null ) {
					final Element licenseElement = document.createElement( LICENSE_ELEMENT );
					licenseElement.appendChild( document.createTextNode( licenseDeclaration ));
					fileElement.appendChild( licenseElement );					
				}
				
				// we all good now
				_element = fileElement;
		}
		
		return _element;
		
	}

}
