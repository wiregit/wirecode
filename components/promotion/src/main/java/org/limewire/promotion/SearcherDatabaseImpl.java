package org.limewire.promotion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import org.hsqldb.jdbcDriver;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SearcherDatabaseImpl implements SearcherDatabase {
    private Connection connection;

    @Inject
    public SearcherDatabaseImpl() {
        new jdbcDriver();
        try {
            connection = DriverManager.getConnection("jdbc:hsqldb:mem:lw", "sa", "");
        } catch (SQLException ex) {
            throw new RuntimeException("Unable to get connection to in-memory db.", ex);
        }
    }
    
    /**
     * Called 
     */
    void createTableStructure() throws SQLException{
        
    }

    public void clear() {
        // TODO Auto-generated method stub

    }

    public void expungeExpired() {
        // TODO Auto-generated method stub

    }

    public void ingest(PromotionBinder binder) {
        // TODO Auto-generated method stub

    }

    public List<QueryResult> query(String query) {
        // TODO Auto-generated method stub
        return null;
    }

}
