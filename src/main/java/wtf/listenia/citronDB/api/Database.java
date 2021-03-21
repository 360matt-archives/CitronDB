package wtf.listenia.citronDB.api;

import com.mysql.cj.jdbc.MysqlDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;


public class Database {

    private final MysqlDataSource datas = new MysqlDataSource() {{
        try {
            this.setServerTimezone("UTC");
        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }};
    private Connection connection;

    public final Database setHost (final String host) { this.datas.setServerName(host); return this; }
    public final Database setPort (final int port) { this.datas.setPort(port); return this; }

    public final Database setDbName (final String databaseName) { this.datas.setDatabaseName(databaseName); return this; }
    public final Database setUsername (final String username) { this.datas.setUser(username); return this; }
    public final Database setPassword (final String password) { this.datas.setPassword(password); return this; }

    public final Database connect () {
        try {
            this.connection = datas.getConnection();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        return this;
    }

    public final Database connectWithException () throws SQLException {
        this.connection = datas.getConnection();
        return this;
    }

    public final void close () {
        try {
            this.connection.close();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }

    public final void closeWithException () throws SQLException {
        this.connection.close();
    }

    public final Connection getConnection () {
        return connection;
    }

    public final Statement getStatement () {
        try {
            return connection.createStatement();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public final Statement getStatementWithException () throws SQLException {
        return connection.createStatement();
    }

    public final <D> TableManager<D> getTable (final String name, final Class<D> struct) {
        return new TableManager<>(this, name, struct);
    }


}
