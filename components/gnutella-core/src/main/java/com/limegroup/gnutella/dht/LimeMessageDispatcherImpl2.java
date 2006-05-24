/*
 * Mojito Distributed Hash Tabe (DHT)
 * Copyright (C) 2006 LimeWire LLC
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
 
package com.limegroup.gnutella.dht;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.Connection;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.FileManagerEvent;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.LifecycleEvent;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.chat.Chatter;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.util.ProcessingQueue;
import com.limegroup.gnutella.version.UpdateInformation;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.io.MessageDispatcher;
import com.limegroup.mojito.io.MessageFormatException;
import com.limegroup.mojito.io.Tag;
import com.limegroup.mojito.messages.DHTMessage;
import com.limegroup.mojito.messages.LimeDHTMessage;

/**
 * 
 */
public class LimeMessageDispatcherImpl2 extends MessageDispatcher 
        implements MessageRouter.MessageHandler {

    private static final Log LOG = LogFactory.getLog(LimeMessageDispatcherImpl.class);
    
    private ProcessingQueue processingQueue;
    
    private boolean running = false;
    
    public LimeMessageDispatcherImpl2(Context context) {
        super(context);
        
        processingQueue = new ProcessingQueue(context.getName() + "-LimeMessageDispatcherPQ", true);
        
        RouterService service = new RouterService(new DoNothing());
        RouterService.preGuiInit();
        service.start();
        
        RouterService.getMessageRouter()
            .setUDPMessageHandler(LimeDHTMessage.class, this);
        
        context.scheduleAtFixedRate(new Runnable() {
            public void run() {
                if (isRunning()) {
                    handleClenup();
                }
            }
        }, CLEANUP, CLEANUP);
    }

    protected boolean allow(DHTMessage message) {
        return true;
    }

    public void bind(SocketAddress address) throws IOException {
        running = true;
    }

    public boolean isOpen() {
        return running;
    }
    
    public void stop() {
        running = false;
        processingQueue.clear();
    }
    
    protected boolean enqueueOutput(Tag tag) {
        try {
            InetSocketAddress dst = (InetSocketAddress)tag.getSocketAddres();
            LimeDHTMessage msg = LimeDHTMessage.createMessage(tag.getData().array());
            UDPService.instance().send(msg, dst);
            tag.sent();
            registerInput(tag);
            return true;
        } catch (BadPacketException e) {
            LOG.error("", e);
        } catch (IOException e) {
            LOG.error("", e);
        }
        return false;
    }

    public void handleMessage(Message msg, InetSocketAddress addr, 
            ReplyHandler handler) {
        try {
            DHTMessage message = ((LimeDHTMessage)msg).getDHTMessage(addr);
            received(message);
        } catch (MessageFormatException err) {
            LOG.error("", err);
        } catch (IOException err) {
            LOG.error("", err);
        }
    }

    protected void interestRead(boolean on) {
    }

    protected void interestWrite(boolean on) {
    }

    public boolean isRunning() {
        return running;
    }

    protected void process(Runnable runnable) {
        processingQueue.add(runnable);
    }
    
    public void run() {
        running = true;
    }
    
    private static class DoNothing implements ActivityCallback {

        public void acceptChat(Chatter ctr) {
        }

        public void acceptedIncomingChanged(boolean status) {
        }

        public void addressStateChanged() {
        }

        public void addUpload(Uploader u) {
        }

        public void browseHostFailed(GUID guid) {
        }

        public void chatErrorMessage(Chatter chatter, String str) {
        }

        public void chatUnavailable(Chatter chatter) {
        }

        public void componentLoading(String component) {
        }

        public void connectionClosed(Connection c) {
        }

        public void connectionInitialized(Connection c) {
        }

        public void connectionInitializing(Connection c) {
        }

        public void disconnected() {
        }

        public void fileManagerLoaded() {
        }

        public void fileManagerLoading() {
        }

        public void handleFileEvent(FileManagerEvent evt) {
        }

        public boolean handleMagnets(MagnetOptions[] magnets) {
            return false;
        }

        public void handleQueryResult(RemoteFileDesc rfd, HostData data, Set locs) {
        }

        public void handleQueryString(String query) {
        }

        public void handleSharedFileUpdate(File file) {
        }

        public boolean isQueryAlive(GUID guid) {
            return false;
        }

        public void receiveMessage(Chatter chr) {
        }

        public void removeUpload(Uploader u) {
        }

        public void restoreApplication() {
        }

        public void setAnnotateEnabled(boolean enabled) {
        }

        public void updateAvailable(UpdateInformation info) {
        }

        public void uploadsComplete() {
        }

        public boolean warnAboutSharingSensitiveDirectory(File dir) {
            return false;
        }

        public void addDownload(Downloader d) {
        }

        public void downloadsComplete() {
        }

        public String getHostValue(String key) {
            return null;
        }

        public void promptAboutCorruptDownload(Downloader dloader) {
        }

        public void removeDownload(Downloader d) {
        }

        public void showDownloads() {
        }

        public void handleAddressStateChanged() {
        }

        public void handleLifecycleEvent(LifecycleEvent evt) {
        }
    }
}
