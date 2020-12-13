package wtf.listenia.citronDB.internal;

import wtf.listenia.citronDB.api.rows.RowBuilder;
import wtf.listenia.citronDB.api.tables.GuestTable;
import wtf.listenia.citronDB.api.tables.Table;
import wtf.listenia.citronDB.api.tables.TableBuilder;


import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class Requests {

    public static String[] getColumns (GuestTable table) {
        try {
            Statement stmt = table.database.connection.createStatement();
            final ResultSet rs = stmt.executeQuery("SHOW COLUMNS FROM `" + table.tableName + "`;");
            final List<String> res = new ArrayList<>();
            while (rs.next())
                res.add(rs.getString(1));
            return res.toArray(new String[]{});
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return new String[]{};
    }


    public static void insertFromRow (Table table, Table.Row row) {
        insertMechanist(table, RowsUtils.rowToHashMap(row));
    }

    public static void insertFromBuilder (GuestTable table, RowBuilder row) {
        insertMechanist(table, row.content);
    }

    public static void insertMechanist (GuestTable table, HashMap<String, Object> contents) {
            try {
                final List<String> columnsList = new ArrayList<>();
                final List<String> valuesList = new ArrayList<>();

                for (Map.Entry<String, Object> entry : contents.entrySet()) {
                    columnsList.add(entry.getKey());
                    valuesList.add("\"" + entry.getValue().toString() + "\"");
                }

                if (columnsList.size() > 0) {
                    final String columns = " (" + String.join(", ", columnsList) + ")";
                    final String values = " (" + String.join(", ", valuesList) + ")";

                    columnsList.clear();
                    valuesList.clear();

                    Statement stmt = table.database.connection.createStatement();
                    stmt.execute("INSERT INTO `" + table.tableName + "` " + columns + " VALUES " + values);
                    stmt.close();

                }

            } catch (Exception e) {
                e.printStackTrace();
            }
    }


    public static void removeRow (Table table, Table.Row row) {
        removeMechanist(table, RowsUtils.rowToHashMap(row));
    }
    public static void removeFromBuilder (GuestTable table, RowBuilder row) {
        removeMechanist(table, row.content);
    }

    private static void removeMechanist (GuestTable table, HashMap<String, Object> contents) {
        try {
            Set<String> whereComplements = new HashSet<>();

            for (Map.Entry<String, Object> entry : contents.entrySet())
                whereComplements.add("`" + entry.getKey() + "`=\"" + entry.getValue().toString() + "\"");

            if (whereComplements.size() > 0) {
                final Statement stmt = table.database.connection.createStatement();
                stmt.execute("DELETE FROM `" + table.tableName + "` WHERE " + String.join("AND ", whereComplements));
                stmt.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void createTableFromBuilder (GuestTable table, TableBuilder builder) {
        createTableMechanist(table, builder.getSubSyntax());
    }

    public static void createTableFromRow (Table table) {
        StringBuilder columns = new StringBuilder();
        for (Field field : table.modelRow().getClass().getDeclaredFields())
            columns.append(field.getName()).append(" TEXT, ");
        columns = new StringBuilder(columns.substring(0, columns.length() - 2));
        createTableMechanist(table, columns.toString());
    }



    private static void createTableMechanist (GuestTable table, String columns) {
        try {
            final Statement stmt = table.database.connection.createStatement();

            stmt.execute("CREATE TABLE IF NOT EXISTS `" + table.tableName + "` (" + columns + ");");

            /*
             *  ID: A
             *  Description: all theses columns must be removed AFTER creations of new columns
             * */
            final ResultSet rs = stmt.executeQuery("SHOW COLUMNS FROM `" + table.tableName + "`;");
            final Set<String> toDelete = new HashSet<>();
            while (rs.next()) {
                String col = rs.getString(1);
                if (!columns.contains(col))
                    toDelete.add("DROP COLUMN " + col);
            }
            rs.close();


            stmt.execute("ALTER TABLE `" + table.tableName + "` ADD IF NOT EXISTS (" + columns + ");");

            /* delete columns from id:A */
            if (toDelete.size() > 0)
                stmt.execute("ALTER TABLE `" + table.tableName + "` " + String.join(", ", toDelete));

            stmt.close();

        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }


    public static void removeTable (GuestTable table) {
        try {
            final Statement stmt = table.database.connection.createStatement();
            stmt.execute("DROP TABLE IF EXISTS `" + table.tableName + "`");
            stmt.close();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }



    public static <R> Set<R> getRowFromBuilder (GuestTable table, RowBuilder builder, R model, int count) {
        final Set<R> res = new HashSet<>();

        try {
            for (RowBuilder rowBuilder: getRowFromHashMap(table, builder.content, count)) {
                R row = (R) model.getClass().newInstance();
                for (Field field : row.getClass().getFields())
                    field.set(row, ConvertTypes.get(rowBuilder.get(field.getName()), field.getGenericType().getTypeName()));
                res.add(row);
            }
        } catch (Exception exception) { exception.printStackTrace(); }
        return res;
    }

    public static Set<RowBuilder> getRowBuilderFromBuilder (GuestTable table, RowBuilder builder, int count) {
        return getRowFromHashMap(table, builder.content, count);
    }

    private static Set<RowBuilder> getRowFromHashMap (GuestTable table, HashMap<String, Object> map, int count) {
        final Set<RowBuilder> res = new HashSet<>();
        for (HashMap<String, Object> object : getRowMechanist(table, map, count)) {
            final RowBuilder rowB = new RowBuilder();
            rowB.content.putAll(object);
            res.add(rowB);
        }
        return res;
    }

    private static Set<HashMap<String, Object>> getRowMechanist (GuestTable table, HashMap<String, Object> contents, int count) {
        final Set<HashMap<String, Object>> res = new HashSet<>();
        try {
            if (contents.size() > 0) {
                Set<String> whereComplements = new HashSet<>();

                for (Map.Entry<String, Object> entry : contents.entrySet())
                    if (entry.getValue() != null)
                        whereComplements.add("" + entry.getKey() + "=\"" + entry.getValue().toString() + "\"");

                final Statement stmt = table.database.connection.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM `" + table.tableName + "` WHERE " + String.join("AND ", whereComplements));

                final String[] columns =  getColumns(table);

                while (count-- > 0 && rs.next()) {
                    HashMap<String, Object> content = new HashMap<>();
                    for (int i=0; i<columns.length; i++)
                        content.put(columns[i], rs.getObject(i+1));
                    res.add(content);
                }
                rs.close();
                stmt.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res;
    }


}
