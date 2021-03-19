package wtf.listenia.citronDB.api;

import com.mysql.cj.jdbc.MysqlDataSource;

import java.sql.Connection;
import java.sql.SQLException;


public class Database {

    private final MysqlDataSource datas = new MysqlDataSource() {{
        try {
            this.setServerTimezone("UTC");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }};
    public Connection connection;

    public Database setHost (final String host) { this.datas.setServerName(host); return this; }
    public Database setPort (final int port) { this.datas.setPort(port); return this; }

    public Database setDbName (final String databaseName) { this.datas.setDatabaseName(databaseName); return this; }
    public Database setUsername (final String username) { this.datas.setUser(username); return this; }
    public Database setPassword (final String password) { this.datas.setPassword(password); return this; }

    public Database connect () {
        try {
            this.connection = datas.getConnection();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return this;
    }

    public Database connectWithException () throws SQLException {
        this.connection = datas.getConnection();
        return this;
    }

    public void close () {
        try {
            this.connection.close();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void closeWithException () throws SQLException {
        this.connection.close();
    }

    public <D> TableManager<D> getTable (final String name, final Class<D> struct) {
        return new TableManager<>(this, name, struct);
    }


}
