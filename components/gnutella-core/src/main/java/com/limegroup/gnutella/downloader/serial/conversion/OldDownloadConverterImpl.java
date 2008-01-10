package com.limegroup.gnutella.downloader.serial.conversion;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.Range;
import org.limewire.io.IOUtils;
import org.limewire.util.CommonUtils;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.DownloaderType;
import com.limegroup.gnutella.downloader.serial.BTDownloadMemento;
import com.limegroup.gnutella.downloader.serial.DownloadMemento;
import com.limegroup.gnutella.downloader.serial.GnutellaDownloadMemento;
import com.limegroup.gnutella.downloader.serial.OldDownloadConverter;
import com.limegroup.gnutella.downloader.serial.RemoteHostMemento;
import com.limegroup.gnutella.settings.SharingSettings;

public class OldDownloadConverterImpl implements OldDownloadConverter {
    
    private static Log LOG = LogFactory.getLog(OldDownloadConverterImpl.class);
    
    public List<DownloadMemento> readAndConvertOldDownloads(File inputFile) throws IOException {
        ObjectInputStream in = null;
        List roots = null;
        SerialIncompleteFileManager sifm = null;
        
        try {
            in = new DownloadConverterObjectInputStream(new BufferedInputStream(new FileInputStream(inputFile)));
            roots = (List)in.readObject();
            sifm = (SerialIncompleteFileManager)in.readObject();
        } catch(ClassNotFoundException cnfe) {
            throw (IOException)new IOException().initCause(cnfe);
        } finally {
            IOUtils.close(in);
        }
        
        if(roots != null && sifm != null)
            return convertSerialRootsToMementos(roots, sifm);
        else
            return Collections.emptyList();
    }

    private List<DownloadMemento> convertSerialRootsToMementos(List roots,
            SerialIncompleteFileManager sifm) {
        List<DownloadMemento> mementos = new ArrayList<DownloadMemento>(roots.size());
        
        for(Object o : roots) {
            if(o instanceof SerialBTDownloader)
                addBTDownloader(mementos, (SerialBTDownloader)o, sifm);
            else if(o instanceof SerialInNetworkDownloader)
                ; // ignore for conversions -- they'll restart on their own
            else if(o instanceof SerialMagnetDownloader)
                addMagnet(mementos, (SerialMagnetDownloader)o, sifm);
            else if(o instanceof SerialResumeDownloader)
                addResume(mementos, (SerialResumeDownloader)o, sifm);
            else if(o instanceof SerialRequeryDownloader)
                ; // ignore!
            else if(o instanceof SerialStoreDownloader)
                addStore(mementos, (SerialStoreDownloader)o, sifm);
            else if(o instanceof SerialManagedDownloader)
                addManaged(mementos, (SerialManagedDownloader)o, sifm);
            else
                LOG.warn("Unable to convert read object: " + o);
        }
        
        return mementos;
    }

    private void addStore(List<DownloadMemento> mementos, SerialStoreDownloader o, SerialIncompleteFileManager sifm) {
        File incompleteFile = getIncompleteFile(o, sifm);
        List<Range> ranges = getRanges(incompleteFile, sifm);
        mementos.add(new GnutellaDownloadMemento(DownloaderType.STORE, o.getProperties(), ranges, incompleteFile, convertToMementos(o.getRemoteFileDescs())));        
    }
    
    private void addManaged(List<DownloadMemento> mementos, SerialManagedDownloader o, SerialIncompleteFileManager sifm) {
        File incompleteFile = getIncompleteFile(o, sifm);
        List<Range> ranges = getRanges(incompleteFile, sifm);
        mementos.add(new GnutellaDownloadMemento(DownloaderType.MANAGED, o.getProperties(), ranges, incompleteFile, convertToMementos(o.getRemoteFileDescs())));        
    }

    private void addResume(List<DownloadMemento> mementos, SerialResumeDownloader o, SerialIncompleteFileManager sifm) {
        File incompleteFile = getIncompleteFile(o, sifm);
        List<Range> ranges = getRanges(incompleteFile, sifm);
        o.getProperties().put("fileSize", o.getSize());
        o.getProperties().put("sha1Urn", o.getUrn());
        o.getProperties().put("defaultFileName", o.getName());
        mementos.add(new GnutellaDownloadMemento(DownloaderType.MANAGED, o.getProperties(), ranges, incompleteFile, convertToMementos(o.getRemoteFileDescs())));
    }

    private void addMagnet(List<DownloadMemento> mementos, SerialMagnetDownloader o, SerialIncompleteFileManager sifm) {
        File incompleteFile = getIncompleteFile(o, sifm);
        List<Range> ranges = getRanges(incompleteFile, sifm);
        if(o.getUrn() != null)
            o.getProperties().put("sha1Urn", o.getUrn());      
        mementos.add(new GnutellaDownloadMemento(DownloaderType.MAGNET, o.getProperties(), ranges, incompleteFile, convertToMementos(o.getRemoteFileDescs())));
    }

    private void addBTDownloader(List<DownloadMemento> mementos, SerialBTDownloader o, SerialIncompleteFileManager sifm) {
        mementos.add(new BTDownloadMemento(o.getProperties()));
    }
    
    private List<Range> getRanges(File incompleteFile, SerialIncompleteFileManager ifm) {
        List<Range> ranges = ifm.getBlocks().get(incompleteFile);
        if(ranges != null) {
            List<Range> fixedRanges = new ArrayList<Range>(ranges.size());
            for(Range range : ranges) {
                fixedRanges.add(Range.createRange(range.getLow(), range.getHigh() -1));
            }
            return fixedRanges;
        }
        
        return Collections.emptyList();
    }
    
    private File getIncompleteFile(SerialManagedDownloader download, SerialIncompleteFileManager sifm) {
        URN sha1 = getSha1(download);        
        File incompleteFile = null;
        
        if(download instanceof SerialResumeDownloader)
            incompleteFile = ((SerialResumeDownloader)download).getIncompleteFile();
        
        if(sha1 != null)
            incompleteFile = sifm.getHashes().get(sha1);
                
        if(incompleteFile == null) {
            File saveFile = (File)download.getProperties().get("saveFile");
            if(saveFile != null) {
                String defaultName = (String)download.getProperties().get("defaultFileName");
                if(defaultName != null) {
                    saveFile = new File(SharingSettings.getSaveDirectory(defaultName), defaultName);
                }
            }

            Number size = (Number)download.getProperties().get("fileSize");
            if(download instanceof SerialInNetworkDownloader)
                size = ((SerialInNetworkDownloader)download).getSize();
            else if(download instanceof SerialResumeDownloader)
                size = ((SerialResumeDownloader)download).getSize();
            
            if (saveFile != null && size != null) {
                String name = CommonUtils.convertFileName(saveFile.getName());
                incompleteFile = new File(SharingSettings.INCOMPLETE_DIRECTORY.getValue(), "T-"
                        + size.longValue() + "-" + name);
            }
        }
        
        return incompleteFile;
    }
    
    private URN getSha1(SerialManagedDownloader download) {
        URN sha1 = null;
        
        if(download instanceof SerialInNetworkDownloader)
            sha1 = ((SerialInNetworkDownloader)download).getUrn();
        else if(download instanceof SerialMagnetDownloader)
            sha1 = ((SerialMagnetDownloader)download).getUrn();
        else if(download instanceof SerialResumeDownloader)
            sha1 = ((SerialResumeDownloader)download).getUrn();
        
        if(sha1 != null && !sha1.isSHA1())
            sha1 = null;
        
        for(SerialRemoteFileDesc rfd : download.getRemoteFileDescs()) {
            if(sha1 != null)
                break;
            
            for(URN urn : rfd.getUrns()) {
               if(urn.isSHA1()) {
                   sha1 = urn;
                   break;
               }
            }
        }
        
      return sha1;
    }
    
    private Set<RemoteHostMemento> convertToMementos(Set<SerialRemoteFileDesc> rfds) {
        Set<RemoteHostMemento> mementos = new HashSet<RemoteHostMemento>(rfds.size());
        for(SerialRemoteFileDesc rfd : rfds) {
            mementos.add(new RemoteHostMemento(rfd.getHost(), rfd.getPort(), rfd.getFilename(), rfd
                    .getIndex(), rfd.getClientGUID(), rfd.getSpeed(), rfd.getSize(), rfd
                    .isChatEnabled(), rfd.getQuality(), rfd.isReplyToMulticast(), rfd
                    .getXml(), rfd.getUrns(), rfd.isBrowseHostEnabled(), rfd.isFirewalled(),
                    rfd.getVendor(), rfd.isHttp11(), rfd.isTlsCapable(), rfd.getHttpPushAddr()));
        }
        return mementos;
    }
}
