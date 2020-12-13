package wtf.listenia.citronDB.api.database;

import com.mysql.cj.jdbc.MysqlDataSource;
import wtf.listenia.citronDB.api.tables.GuestTable;

import java.sql.Connection;
import java.sql.SQLException;

public class Database {
    private String host;
    private int port;

    private String databaseName;
    private String username;
    private String password;

    public Connection connection;
    public int options = 0;


    public Database setHost (String host) { this.host = host; return this; }
    public Database setPort (int port) { this.port = port; return this; }

    public Database setDbName (String databaseName) { this.databaseName = databaseName; return this; }
    public Database setUsername (String username) { this.username = username; return this; }
    public Database setPassword (String password) { this.password = password; return this; }

    public int options (int options) { return this.options = options; }

    public GuestTable getGuestTable (String name) { return new GuestTable (name, this); }


    public void connect () {
        try {
            MysqlDataSource dataSource = new MysqlDataSource();
            dataSource.setUser(this.username);

            dataSource.setServerName(this.host);
            dataSource.setPort(this.port);
            dataSource.setDatabaseName(this.databaseName);
            dataSource.setPassword(this.password);
            dataSource.setServerTimezone("UTC");

            this.connection = dataSource.getConnection();

            // utilisation de la connexion



        } catch (SQLException e) { e.printStackTrace(); }
    }


    public void close () {
        try {
            this.connection.close();
        } catch (SQLException e) { e.printStackTrace(); }
    }

}
