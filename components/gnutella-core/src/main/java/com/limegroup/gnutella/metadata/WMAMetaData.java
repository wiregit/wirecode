package com.limegroup.gnutella.metadata;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.esau.ptarmigan.Generator;
import org.esau.ptarmigan.GeneratorFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class WMAMetaData extends AudioMetaData {

    private static final String ARTIST_TAG = "artist";
    private static final String TITLE_TAG = "title";
    private static final String ALBUM_TAG = "album";
    private static final String DATE_YEAR_TAG = "date-year";
    private static final String DATE_TAG = "date";
    private static final String TRACK_TAG = "tracknumber";
    private static final String RATE_TAG = "bit-rate";
    
    public WMAMetaData(File f) throws IOException {
        super(f);
    }

    protected void parseFile(File f) throws IOException {
        try {
            Generator generator = GeneratorFactory.newInstance();
            generator.setContentHandler(new WMAHandler());
            generator.parse(new InputSource(new BufferedInputStream(new FileInputStream(f))));
        }catch (InstantiationException bad) {
            throw new IOException();
        }catch (SAXException bad) {
            throw new IOException();
        }catch (IllegalAccessException bad) {
            throw new IOException();
        }
    }
    
    private class WMAHandler extends DefaultHandler {
        
        String currentElement;

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (localName.equals(ARTIST_TAG) ||
                    localName.equals(TITLE_TAG) ||
                    localName.equals(ALBUM_TAG) ||
                    localName.equals(DATE_YEAR_TAG) ||
                    localName.equals(DATE_TAG) ||
                    localName.equals(TRACK_TAG) ||
                    localName.equals(RATE_TAG) ){
                currentElement = localName;
            }
            else currentElement = null;
        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
            currentElement = null;
        }

        public void characters(char[] ch, int start, int length) throws SAXException {
            if (currentElement == null)
                return;
            
            StringBuffer buf = new StringBuffer();
            buf.append(ch,start,length);
            
            if (currentElement.equals(ARTIST_TAG))
                setArtist(buf.toString());
            else if (currentElement.equals(TITLE_TAG))
                setTitle(buf.toString());
            else if (currentElement.equals(ALBUM_KEY))
                setAlbum(buf.toString());
            else if (currentElement.equals(DATE_YEAR_TAG))
                setYear(buf.toString());
            else if (currentElement.equals(DATE_TAG))
                setYear(buf.toString());
            else if (currentElement.equals(RATE_TAG)) {
                try {
                    setBitrate(Integer.parseInt(buf.toString()));
                }catch(NumberFormatException bad) {
                    throw new SAXException(bad);
                }
            } 
            else if (currentElement.equals(TRACK_TAG)) {
                try {
                    setTrack(Short.parseShort(buf.toString()));
                } catch(NumberFormatException bad) {
                    throw new SAXException(bad);
                } 
            }
        }
        
    }

}
