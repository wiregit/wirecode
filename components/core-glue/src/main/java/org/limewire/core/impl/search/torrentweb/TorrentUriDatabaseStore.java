package org.limewire.core.impl.search.torrentweb;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.lifecycle.ServiceStage;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.Clock;
import org.limewire.util.CommonUtils;
import org.limewire.util.URIUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

// can be lazily singleton, uses Service only to be stopped properly, but if
// it's never started no need for service registration
@Singleton
public class TorrentUriDatabaseStore implements TorrentUriStore, Service {
    
    private static final Log LOG = LogFactory.getLog(TorrentUriDatabaseStore.class);
    
    /**
     * Invariant: once initialized <code>dbStore</code> will not become null.
     */
    private volatile DbStore dbStore = null;
    
    /**
     * Lock for construction of <code>dbStore</code>. 
     */
    private final Object lock = new Object();

    private final Clock clock;
    
    @Inject
    public TorrentUriDatabaseStore(Clock clock) {
        this.clock = clock;
    }
    
    @Override
    public String getServiceName() {
        return "torrent uri store";
    }

    @Override
    public void initialize() {
    }
    
    @Override
    public void start() {
    }
    
    @Override
    public void stop() {
        if (dbStore != null) {
            dbStore.stop();
        }
    }
    
    @Inject
    void register(ServiceRegistry serviceRegistry) {
        serviceRegistry.register(this).in(ServiceStage.VERY_LATE);
    }
    
    private DbStore getStore() {
        if (dbStore != null) {
            return dbStore;
        }
        synchronized (lock) {
            if (dbStore == null) {
                dbStore = new DbStore();
            }
            return dbStore;
        }
    }

    @Override
    public void addCanonicalTorrentUris(String host, URI uri) {
        getStore().addCanonicalTorrentUris(host, uri);
    }
    
    @Override
    public Set<URI> getTorrentUrisForHost(String host) {
        return getStore().getTorrentUrisForHost(host);
    }
    
    @Override
    public boolean isNotTorrentUri(URI uri) {
        return getStore().isNotTorrentUri(uri);
    }
    
    @Override
    public boolean isTorrentUri(URI uri) {
        return getStore().isTorrentUri(uri);
    }
    
    @Override
    public void setIsTorrentUri(URI uri, boolean isTorrent) {
        getStore().setIsTorrentUri(uri, isTorrent);
    }
        
    private class DbStore implements TorrentUriStore {
    
        private final Connection connection;

        private final PreparedStatement selectTorrentUris;

        private final PreparedStatement selectTorrentUrisByHost;

        private final PreparedStatement insertTorrentUri;

        private final PreparedStatement insertTorrentUriByHost;

        private PreparedStatement updateTorrentUri;
        
        
        public DbStore() {
            try {
                Class.forName("org.hsqldb.jdbcDriver");
            } catch (ClassNotFoundException e1) {
                throw new RuntimeException(e1);
            }
            try {
                // TODO maybe move into subfolder
                File dbFile = new File(CommonUtils.getUserSettingsDir(), "torrent-uris");
                String connectionUrl = "jdbc:hsqldb:file:" + dbFile.getAbsolutePath();
                connection = DriverManager.getConnection(connectionUrl, "sa", "");
                Statement statement = connection.createStatement();
                // set properties to make memory footprint small
                statement.execute("set property \"hsqldb.cache_scale\" 8");
                statement.execute("set property \"hsqldb.cache_size_scale\" 6");
                try {
                    statement.execute("create cached table torrent_uris (hash int, uri varchar(2048), is_torrent boolean, timestamp bigint, constraint unique_hash_uri unique(hash, uri))");
                    statement.execute("create index torrent_uris_index on torrent_uris(hash)");
                    statement.execute("create cached table torrent_uris_by_host(host varchar_ignorecase(255), uri varchar(2048), timestamp bigint, constraint unique_host_uri unique (host, uri))");
                    statement.execute("create index torrentindex on torrent_uris_by_host(host)");
                } catch (SQLException se) {
                    LOG.debug("sql exception while creating", se);
                }
                selectTorrentUris = connection.prepareStatement("select uri, is_torrent from torrent_uris where hash = ?");
                selectTorrentUrisByHost = connection.prepareStatement("select uri from torrent_uris_by_host where host = ?");
                insertTorrentUri = connection.prepareStatement("insert into torrent_uris values (?, ?, ?, ?)");
                updateTorrentUri = connection.prepareStatement("update torrent_uris set is_torrent = ?, timestamp = ? where hash = ? and uri = ?");
                insertTorrentUriByHost = connection.prepareStatement("insert into torrent_uris_by_host values (?, ?, ?)");
            } catch (SQLException se) {
                throw new RuntimeException(se);
            }
        }
        
        @Override
        public synchronized Set<URI> getTorrentUrisForHost(String host) {
            Set<URI> uris = new HashSet<URI>();
            try {
                selectTorrentUrisByHost.setString(1, host);
                ResultSet resultSet = selectTorrentUrisByHost.executeQuery();
                while (resultSet.next()) {
                    try {
                        String uriString = resultSet.getString(1);
                        boolean added = uris.add(URIUtils.toURI(uriString));
                        assert added;
                    } catch (URISyntaxException e) {
                        LOG.debug("", e);
                    }
                }
            } catch (SQLException se) {
                throw new RuntimeException(se);
            }
            return uris;
        }
        
        @Override
        public boolean isNotTorrentUri(URI uri) {
            Boolean value = getTorrentUriValue(uri);
            return value == null ? false : !value.booleanValue();
        }
        
        private synchronized Boolean getTorrentUriValue(URI uri) {
            try {
                selectTorrentUris.setInt(1, uri.hashCode());
                ResultSet resultSet = selectTorrentUris.executeQuery();
                while (resultSet.next()) {
                    String uriString = resultSet.getString(1);
                    try {
                        URI otherUri = URIUtils.toURI(uriString);
                        if (uri.equals(otherUri)) {
                            return resultSet.getBoolean(2);
                        }
                    } catch (URISyntaxException e) {
                        LOG.debugf(e, "uri: {0}", uriString);
                    }
                }
            } catch (SQLException se) {
                throw new RuntimeException(se);
            }
            return null;
        }
        
        
        @Override
        public boolean isTorrentUri(URI uri) {
            Boolean value = getTorrentUriValue(uri);
            return value == null ? false : value.booleanValue();
        }
        
        @Override
        public synchronized void setIsTorrentUri(URI uri, boolean isTorrentUri) {
            try {
                Boolean value = getTorrentUriValue(uri);
                if (value != null) {
                    if (value.booleanValue() != isTorrentUri) {
                        updateTorrentUri.setBoolean(1, isTorrentUri);
                        updateTorrentUri.setLong(2, clock.now());
                        updateTorrentUri.setInt(3, uri.hashCode());
                        updateTorrentUri.setString(4, uri.toASCIIString());
                        updateTorrentUri.executeUpdate();
                    }
                } else {
                    insertTorrentUri.setInt(1, uri.hashCode());
                    insertTorrentUri.setString(2, uri.toASCIIString());
                    insertTorrentUri.setBoolean(3, isTorrentUri);
                    insertTorrentUri.setLong(4, clock.now());
                    insertTorrentUri.execute();
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        
        @Override
        public synchronized void addCanonicalTorrentUris(String host, URI uri) {
            try {
                insertTorrentUriByHost.setString(1, host);
                insertTorrentUriByHost.setString(2, uri.toASCIIString());
                insertTorrentUriByHost.setLong(3, clock.now());
                insertTorrentUriByHost.execute();
            } catch (SQLException e) {
                LOG.debugf(e, "host {0}, uri {1}", host, uri);
            }
        }
        
        public void stop() {
            LOG.debug("shutting db down");
            try {
                Statement statement = connection.createStatement();
                statement.execute("SHUTDOWN");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

}