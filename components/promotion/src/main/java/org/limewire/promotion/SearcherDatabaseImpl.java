package org.limewire.promotion;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hsqldb.jdbcDriver;
import org.limewire.promotion.containers.PromotionMessageContainer;
import org.limewire.util.CommonUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.messages.BadGGEPBlockException;
import com.limegroup.gnutella.messages.GGEP;

@Singleton
public class SearcherDatabaseImpl implements SearcherDatabase {
    private Connection connection;

    private KeywordUtil keywordUtil;

    private static String getDBLocation() {
        return new File(CommonUtils.getUserSettingsDir(), "promotion/promodb").getAbsolutePath();
    }

    /**
     * Called during shutdown process to ensure the db has been closed.
     */
    private class ShutdownHook extends Thread {
        @Override
        public void run() {
            executeUpdate("SHUTDOWN");
        }
    }

    @Inject
    public SearcherDatabaseImpl(KeywordUtil keywordUtil) {
        this.keywordUtil = keywordUtil;
        new jdbcDriver();
        try {
            connection = DriverManager.getConnection("jdbc:hsqldb:file:" + getDBLocation(), "sa",
                    "");
            Runtime.getRuntime().addShutdownHook(new ShutdownHook());
            createDBIfNeeded();
        } catch (SQLException ex) {
            throw new RuntimeException("Unable to get connection to in-memory db.", ex);
        }
    }

    /**
     * Creates a statement and runs the given SQL, then returns the resulting
     * affected row count.
     * 
     * @see {@link java.sql.Statement#executeUpdate(String)}
     */
    private int executeUpdate(String sql, Object... values) {
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            try {
                for (int i = 0; i < values.length; i++) {
                    statement.setObject(i + 1, values[i]);
                }
                return statement.executeUpdate();
            } finally {
                statement.close();
            }
        } catch (SQLException ex) {
            throw new RuntimeException("SQLException during update", ex);
        }
    }

    /**
     * Creates a statement and runs the given SQL, then returns the results of a
     * call to "CALL IDENTITY()"
     */
    private long executeInsert(String sql, Object... values) {
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            try {
                for (int i = 0; i < values.length; i++) {
                    if (values[i] instanceof Date)
                        statement.setDate(i + 1, new java.sql.Date(((Date) values[i]).getTime()));
                    else
                        statement.setObject(i + 1, values[i]);
                }
                statement.executeUpdate();
            } finally {
                statement.close();
            }
            statement = connection.prepareStatement("CALL IDENTITY()");
            try {
                ResultSet rs = statement.executeQuery();
                try {
                    rs.next();
                    return rs.getLong(1);
                } finally {
                    rs.close();
                }
            } finally {
                statement.close();
            }
        } catch (SQLException ex) {
            throw new RuntimeException("SQLException during update", ex);
        }
    }

    private void createDBIfNeeded() {
        try {
            query("foo");
        } catch (RuntimeException ignored) {
            // Looks like the db needs some work, try clearing it.
            clear();
        }
    }

    public void clear() {
        executeUpdate("DROP TABLE keywords IF EXISTS");
        executeUpdate("DROP TABLE entries IF EXISTS");

        executeUpdate("CREATE CACHED TABLE entries (" + "entry_id IDENTITY, "
                + "unique_id BIGINT, " + "probability_num FLOAT, " + "type_byte TINYINT, "
                + "valid_start_dt DATETIME, " + "valid_end_dt DATETIME, entry_ggep BINARY )");
        executeUpdate("CREATE CACHED TABLE keywords ("
                + "keyword_id IDENTITY, "
                + "binder_unique_id BIGINT,"
                + "phrase VARCHAR,"
                + "entry_id INTEGER, FOREIGN KEY (entry_id) REFERENCES entries (entry_id) ON DELETE CASCADE )");
    }

    public void expungeExpired() {
        executeUpdate("DELETE FROM entries WHERE valid_end_dt < CURRENT_TIMESTAMP");
    }

    /**
     * Does the actual ingestion of the given promo entry, inserting it into the
     * db. Package-visible for testing.
     */
    void ingest(PromotionMessageContainer promo, long binderID) {
        executeUpdate("DELETE FROM entries WHERE unique_id = ?", promo.getUniqueID());
        long entryID = executeInsert(
                "INSERT INTO entries "
                        + "(unique_id, probability_num, type_byte, valid_start_dt, valid_end_dt, entry_ggep) "
                        + "values (?,?,?,?,?,?)", promo.getUniqueID(), promo.getProbability(),
                promo.getMediaType().getValue(), promo.getValidStart(), promo.getValidEnd(), promo
                        .getEncoded());
        for (String keywords : keywordUtil.splitKeywords(promo.getKeywords()))
            executeInsert(
                    "INSERT INTO keywords (phrase, binder_unique_id, entry_id) values (?,?,?)",
                    keywords, binderID, entryID);
    }

    public void ingest(PromotionBinder binder) {
        for (PromotionMessageContainer promo : binder.getPromoMessageList())
            ingest(promo, promo.getUniqueID());
    }

    public List<QueryResult> query(String query) {
        List<QueryResult> results = new ArrayList<QueryResult>();

        String normalizedQuery = "%" + keywordUtil.normalizeQuery(query) + "%";

        PreparedStatement statement = null;
        try {
            statement = connection
                    .prepareStatement("SELECT DISTINCT e.entry_id, k.binder_unique_id, e.probability_num FROM "
                            + "keywords k JOIN entries e ON e.entry_id = k.entry_id WHERE "
                            + "e.valid_start_dt <= CURRENT_TIMESTAMP AND e.valid_end_dt >= CURRENT_TIMESTAMP "
                            + "AND k.phrase LIKE ? ORDER BY e.probability_num DESC");
            statement.setString(1, normalizedQuery);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                results.add(new QueryResultImpl(rs.getLong("binder_unique_id"),
                        getPromotionMessageContainer(rs.getLong("entry_id")), query));
            }
            rs.close();
        } catch (SQLException ex) {
            throw new RuntimeException("SQLException during query.", ex);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException ignored) {

                }
            }
        }

        return results;
    }

    /**
     * Loads the given entry id from the database, ignoring all the fields
     * except the entry_ggep, reparsing the ggep into a promotion entry.
     * 
     * @param entryID
     * @return
     */
    PromotionMessageContainer getPromotionMessageContainer(long entryID) {
        PreparedStatement statement = null;
        try {
            statement = connection
                    .prepareStatement("select entry_ggep from entries where entry_id = ?");
            statement.setLong(1, entryID);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                byte[] ggep = rs.getBytes(1);
                PromotionMessageContainer promo = new PromotionMessageContainer();
                promo.parse(new GGEP(ggep, 0));
                return promo;
            }
            rs.close();
            // Couldn't find the given entry id.
            return null;
        } catch (SQLException ex) {
            throw new RuntimeException("SQLException during query.", ex);
        } catch (BadGGEPBlockException ex) {
            throw new RuntimeException("GGEPException during query.", ex);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    private class QueryResultImpl implements QueryResult {
        private final long binderUniqueId;

        private final Date creationDate = new Date();

        private final PromotionMessageContainer promotionMessageContainer;

        private final String query;

        QueryResultImpl(long binderUniqueId, PromotionMessageContainer promo, String query) {
            this.binderUniqueId = binderUniqueId;
            this.promotionMessageContainer = promo;
            this.query = query;
        }

        public long getBinderUniqueId() {
            return binderUniqueId;
        }

        public Date getDate() {
            return creationDate;
        }

        public PromotionMessageContainer getPromotionMessageContainer() {
            return promotionMessageContainer;
        }

        public String getQuery() {
            return query;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof QueryResultImpl) {
                QueryResultImpl other = (QueryResultImpl) obj;
                return this.promotionMessageContainer.equals(other.promotionMessageContainer);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return promotionMessageContainer.hashCode();
        }
    }
}
