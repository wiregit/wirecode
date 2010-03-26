package org.limewire.core.impl.library;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.limewire.core.api.URN;
import org.limewire.core.api.search.SearchResult;
import org.limewire.friend.api.FriendPresence;
import org.limewire.inject.EagerSingleton;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.CommonUtils;

import com.google.inject.Inject;

@EagerSingleton
public class FriendFileDbCache implements Service {
    
    private static final Log LOG = LogFactory.getLog(FriendFileDbCache.class);

    private final Connection connection;
    private final PreparedStatement selectStatement;

    private final PreparedStatement updateStatement;

    private final PreparedStatement insertStatement;

    @Inject
    public FriendFileDbCache() {
        try {
            Class.forName("org.hsqldb.jdbcDriver");
        } catch (ClassNotFoundException e1) {
            throw new RuntimeException(e1);
        }
        try {
            File dbFile = new File(CommonUtils.getUserSettingsDir(), "frienddbcache");
            String connectionUrl = "jdbc:hsqldb:file:" + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(connectionUrl, "sa", "");
            Statement statement = connection.createStatement();
            try {
                statement.execute("create cached table results (presence varchar_ignorecase(20), urn varchar_ignorecase(42), seen INT)");
                statement.execute("create index lookup on results(presence, urn)");
            } catch (SQLException se) {
            }
            selectStatement = connection.prepareStatement("select seen from results where presence = ? and urn = ?");
            insertStatement = connection.prepareStatement("insert into results values (?, ?, ?)");
            updateStatement = connection.prepareStatement("update results set seen = ? where presence = ? and urn = ?");
        } catch (SQLException se) {
            throw new RuntimeException(se);
        }
        
    }
    
    /**
     * @return if result is relatively new from this friend
     */
    public boolean addResult(FriendPresence presence, SearchResult result) {
        String id = presence.getPresenceId();
        URN urn = result.getUrn();
        try {
            synchronized (connection) {
                selectStatement.setString(1, id);
                selectStatement.setString(2, urn.toString());
                LOG.debugf("querying: {0}", selectStatement);
                ResultSet resultSet = selectStatement.executeQuery();
                int seen = 0;
                if (resultSet.next()) {
                    seen = resultSet.getInt(1);
                } else {
                    LOG.debugf("seen: {0}", seen);
                    insertStatement.setString(1, id);
                    insertStatement.setString(2, urn.toString());
                    insertStatement.setInt(3, 0);
                    insertStatement.execute();
                    return true;
                }
                LOG.debugf("seen: {0}", seen);
                updateStatement.setInt(1, ++seen);
                updateStatement.setString(2, id);
                updateStatement.setString(3, urn.toString());
                updateStatement.execute();
                return seen < 3;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
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
}
