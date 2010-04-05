package org.limewire.core.impl.search;

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

import org.limewire.inject.EagerSingleton;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.CommonUtils;
import org.limewire.util.URIUtils;

import com.google.inject.Inject;

@EagerSingleton
public class SqlTorrentUriStore implements TorrentUriStore, Service {
    
    private static final Log LOG = LogFactory.getLog(SqlTorrentUriStore.class);

    private final Connection connection;

    private PreparedStatement selectNotTorrentUris;

    private PreparedStatement selectTorrentUris;

    private PreparedStatement selectTorrentUrisByHost;

    private PreparedStatement insertNotTorrentUri;

    private PreparedStatement insertTorrentUri;

    private PreparedStatement insertTorrentUriByHost;
    
    @Inject
    public SqlTorrentUriStore() {
        try {
            Class.forName("org.hsqldb.jdbcDriver");
        } catch (ClassNotFoundException e1) {
            throw new RuntimeException(e1);
        }
        try {
            File dbFile = new File(CommonUtils.getUserSettingsDir(), "torrenturis");
            String connectionUrl = "jdbc:hsqldb:file:" + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(connectionUrl, "sa", "");
            Statement statement = connection.createStatement();
            try {
                statement.execute("create cached table not_torrent_uris (hash int, uri varchar(2048))");
                statement.execute("create index not_torrent_uris_index on not_torrent_uris(hash)");
                statement.execute("create cached table torrent_uris (hash int, uri varchar(2048))");
                statement.execute("create index torrent_uris_index on torrent_uris(hash)");
                statement.execute("create cached table torrent_uris_by_host(host varchar_ignorecase(255), uri varchar(2048))");
                statement.execute("create index torrentindex on torrent_uris_by_host(host)");
            } catch (SQLException se) {
                se.printStackTrace();
            }
            selectNotTorrentUris = connection.prepareStatement("select uri from not_torrent_uris where hash = ?");
            selectTorrentUris = connection.prepareStatement("select uri from torrent_uris where hash = ?");
            selectTorrentUrisByHost = connection.prepareStatement("select uri from torrent_uris_by_host where host = ?");
            insertNotTorrentUri = connection.prepareStatement("insert into not_torrent_uris values (?, ?)");
            insertTorrentUri = connection.prepareStatement("insert into torrent_uris values (?, ?)");
            insertTorrentUriByHost = connection.prepareStatement("insert into torrent_uris_by_host values (?, ?)");
        } catch (SQLException se) {
            throw new RuntimeException(se);
        }
        
    }
        
    public static void main(String[] args) {
        new SqlTorrentUriStore();
    }
    
    @Override
    public String getServiceName() {
        return "frienddbcache";
    }

    @Override
    public void initialize() {
    }
    
    @Override
    public void start() {
    }
    
    @Override
    public void stop() {
        try {
            Statement statement = connection.createStatement();
            statement.execute("SHUTDOWN");
            System.out.println("shutdown");
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    @Inject
    void register(ServiceRegistry serviceRegistry) {
        serviceRegistry.register(this);
    }
    
    @Override
    public Set<URI> getTorrentUrisForHost(String host) {
        Set<URI> uris = new HashSet<URI>();
        try {
            selectTorrentUrisByHost.setString(1, host);
            ResultSet resultSet = selectTorrentUris.executeQuery();
            while (resultSet.next()) {
                try {
                    String uriString = resultSet.getString(1);
                    uris.add(URIUtils.toURI(uriString));
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        } catch (SQLException se) {
            se.printStackTrace();
        }
        return uris;
    }
    
    @Override
    public boolean isNotTorrentUri(URI uri) {
        try {
            selectNotTorrentUris.setInt(1, uri.hashCode());
            ResultSet resultSet = selectNotTorrentUris.executeQuery();
            while (resultSet.next()) {
                String uriString = resultSet.getString(1);
                try {
                    URI otherUri = URIUtils.toURI(uriString);
                    if (uri.equals(otherUri)) {
                        return true;
                    }
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        } catch (SQLException se) {
            se.printStackTrace();
        }
        return false;
    }
    
    @Override
    public boolean isTorrentUri(URI uri) {
        try {
            selectTorrentUris.setInt(1, uri.hashCode());
            ResultSet resultSet = selectTorrentUris.executeQuery();
            while (resultSet.next()) {
                String uriString = resultSet.getString(1);
                try {
                    URI otherUri = URIUtils.toURI(uriString);
                    if (uri.equals(otherUri)) {
                        return true;
                    }
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        } catch (SQLException se) {
            se.printStackTrace();
        }
        return false;
    }

    @Override
    public void setIsTorrentUri(URI uri, boolean isTorrentUri) {
        try {
            if (isTorrentUri) {
                insertTorrentUri.setInt(1, uri.hashCode());
                insertTorrentUri.setString(2, uri.toASCIIString());
                insertTorrentUri.execute();
            } else {
                insertNotTorrentUri.setInt(1, uri.hashCode());
                insertNotTorrentUri.setString(2, uri.toASCIIString());
                insertNotTorrentUri.execute();
            } 
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addCanonicalTorrentUris(String host, URI uri) {
        try {
            insertTorrentUriByHost.setString(1, host);
            insertTorrentUriByHost.setString(2, uri.toASCIIString());
            insertTorrentUriByHost.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
