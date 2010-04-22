package com.limegroup.gnutella;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.limewire.core.settings.SecuritySettings;
import org.limewire.inject.EagerSingleton;
import org.limewire.io.GUID;
import org.limewire.lifecycle.Asynchronous;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.lifecycle.ServiceStage;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.security.id.SecureIdStore;
import org.limewire.util.Base32;
import org.limewire.util.Clock;
import org.limewire.util.CommonUtils;

import com.google.inject.Inject;
import com.google.inject.name.Named;

@EagerSingleton
public class SecureIdDatabaseStore implements SecureIdStore, Service {
    
    private static final Log LOG = LogFactory.getLog(SecureIdDatabaseStore.class);
    
    private volatile DbStore store;
    
    @Inject
    void register(ServiceRegistry serviceRegistry) {
        serviceRegistry.register(this).in(ServiceStage.VERY_LATE);
    }
    
    @Inject
    void register(@Named("backgroundExecutor") ScheduledExecutorService scheduledExecutorService,
            final Clock clock) {
        scheduledExecutorService.schedule(new Runnable() {
            @Override
            public void run() {
                long aYearAgo = clock.now() - TimeUnit.DAYS.toMillis(365);
                store.deleteOlderThan(aYearAgo);
            }
        }, 5, TimeUnit.MINUTES);
    }
    
    @Override
    public byte[] getLocalData() {
        String value = SecuritySettings.SECURE_IDENTITY.get();
        if (value.isEmpty()) {
            return null;
        }
        return Base32.decode(value);
    }
    @Override
    public void setLocalData(byte[] value) {
        SecuritySettings.SECURE_IDENTITY.set(Base32.encode(value));
    }
    
    @Override
    public String getServiceName() {
        return "id db store";
    }
    @Override
    public void initialize() {
    }
    
    @Override
    @Asynchronous
    public void start() {
        try {
            store = new DbStore(false);
        } catch (SQLException e) {
            LOG.debug("error initializing store", e);
            try {
                store = new DbStore(true);
            } catch (SQLException e1) {
                LOG.debug("error reinitializing store", e);
            }
        }
    }
    
    @Override
    public void stop() {
        store.stop();
    }

    @Override
    public byte[] get(GUID key) {
        return store.get(key);
    }

    @Override
    public void put(GUID key, byte[] value) {
        store.put(key, value);
    }
    
    class DbStore {
        
        private final Connection connection;

        private final PreparedStatement getStatement;
        
        private final PreparedStatement putStatement;

        private PreparedStatement deleteStatement;
        
        // TODO implement drop
        public DbStore(boolean dropDb) throws SQLException {
            try {
                Class.forName("org.hsqldb.jdbcDriver");
            } catch (ClassNotFoundException e1) {
                throw new RuntimeException(e1);
            }
            File dbFile = new File(CommonUtils.getUserSettingsDir(), "secure-ids");
            String connectionUrl = "jdbc:hsqldb:file:" + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(connectionUrl, "sa", "");
            Statement statement = connection.createStatement();
            try {
                statement.execute("create cached table ids (guid binary(16) primary key, timestamp bigint, data varbinary(200))");
            } catch (SQLException se) {
                LOG.debug("table already exists", se);
            }
            getStatement = connection.prepareStatement("select data from ids where guid = ?");
            putStatement = connection.prepareStatement("insert into ids values (?, ?)");
            deleteStatement = connection.prepareStatement("delete from ids where timestamp < ?");
        }
        
        public synchronized byte[] get(GUID key) {
            try {
                getStatement.setBytes(1, key.bytes());
                ResultSet resultSet = getStatement.executeQuery();
                while (resultSet.next()) {
                    return resultSet.getBytes(1);
                }
            } catch (SQLException e) {
                LOG.debug("error getting value", e);
            }
            return null;
        }

        public synchronized void put(GUID key, byte[] value) {
            try {
                putStatement.setBytes(1, key.bytes());
                putStatement.setLong(2, System.currentTimeMillis());
                putStatement.setBytes(3, value);
                putStatement.execute();
            } catch (SQLException e) {
                LOG.debug("error putting value", e);
            }
        }
        
        public synchronized void stop() {
            try {
                Statement statement = connection.createStatement();
                statement.execute("SHUTDOWN");
            } catch (SQLException e) {
                LOG.debug("error shutting down", e);
            }
        }
        
        public synchronized void deleteOlderThan(long timestamp) {
            try {
                deleteStatement.setLong(1, timestamp);
                deleteStatement.execute();
            } catch (SQLException e) {
                LOG.debug("error deleting old entries", e);
            }
        }

    }
}
