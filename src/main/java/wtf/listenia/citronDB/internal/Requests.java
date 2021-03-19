package wtf.listenia.citronDB.internal;

import wtf.listenia.citronDB.api.annotations.Primary;
import wtf.listenia.citronDB.api.annotations.Unique;
import wtf.listenia.citronDB.api.builders.RowBuilder;
import wtf.listenia.citronDB.api.TableManager;
import wtf.listenia.citronDB.utils.ColumnType;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;

public class Requests {

    public static void createTable (final TableManager<?> table) {
        try {
            final StringJoiner sj = new StringJoiner(", ");
            boolean hasPrimary = false;

            final Field[] fields = table.defaultInstance.getClass().getFields();
            for (final Field field : fields) {

                boolean currentUnique = false;
                boolean currentPrimary = false;

                final Unique unique = field.getAnnotation(Unique.class);
                if (unique != null) {
                    currentUnique = true;
                    sj.add(field.getName() + " VARCHAR(" + unique.size() + ")");
                    sj.add("UNIQUE(" + field.getName() + ")");
                }
                if (!hasPrimary) {
                    final Primary primary = field.getAnnotation(Primary.class);
                    if (primary != null) {
                        currentPrimary = true;
                        hasPrimary = true;
                        sj.add(field.getName() + " VARCHAR(" + primary.size() + ")");
                        sj.add("PRIMARY KEY(`" + field.getName() + "`)");
                    }
                }

                if (!currentUnique && !currentPrimary)
                    sj.add(field.getName() + " TEXT");
            }

            final Statement stmt = table.database.connection.createStatement();

            final String sql = "CREATE TABLE IF NOT EXISTS `" + table.name + "` (" + sj.toString() + ");";
            final int status = stmt.executeUpdate(sql);

            stmt.close();

            if (status != 0) {
                // if the table has not been created above,
                // we must apply the new structure by an ALTER.

                updateTable(table);
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateTable (final TableManager<?> table) {
        try {

            final Statement stmt = table.database.connection.createStatement();
            final List<String> local = new ArrayList<>();
            final List<String> distant = new ArrayList<>();

            final Class<?> instance = table.defaultInstance.getClass();

            for (final Field field : table.defaultInstance.getClass().getFields())
                local.add(field.getName()); // + " TEXT"


            final ResultSet rs = stmt.executeQuery("SELECT * FROM `" + table.name + "` WHERE 1 = 0;"); // select nothing
            final ResultSetMetaData meta = rs.getMetaData();
            for (int i=0; i<meta.getColumnCount(); i++)
                distant.add(meta.getColumnName(i + 1));
            rs.close();

            final StringJoiner toCreate = new StringJoiner(",");
            final StringJoiner toDelete = new StringJoiner(",");
            final StringJoiner toModify = new StringJoiner(",");

            if (local.size() > 0 || distant.size() > 0) {
                for (final String candid : distant) {
                    if (!local.contains(candid)) {
                        toDelete.add(candid);
                    } else {
                        final Field field = instance.getField(candid);
                        toModify.add("MODIFY " + ColumnType.getFormat(field, false));
                    }
                    local.remove(candid);
                }
                for (final String candid : local) {
                    final Field field = instance.getField(candid);
                    toCreate.add(ColumnType.getFormat(field));
                    distant.remove(candid);
                }
            }

            if (toCreate.length() > 0)
                stmt.execute("ALTER TABLE `" + table.name + "` ADD COLUMN " + toCreate.toString() + ";");
            if (toDelete.length() > 0)
                stmt.execute("ALTER TABLE `" + table.name + "` DROP " + toDelete.toString() + ";");
            if (toModify.length() > 0)
                stmt.execute("ALTER TABLE `" + table.name + "` " + toModify.toString() + ";");

        } catch (SQLException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public static void removeTable (final TableManager<?> table) {
        try {
            final Statement stmt = table.database.connection.createStatement();
            stmt.execute("DROP TABLE IF EXISTS `" + table.name + "`");
            stmt.close();
        } catch (final SQLException exception) {
            exception.printStackTrace();
        }
    }

    public static <D> void insert (final TableManager<?> table, final D struct) {
        try {
            final Field[] fields = struct.getClass().getFields();

            if (fields.length >= 1) {
                final StringJoiner columnsList = new StringJoiner(",");
                final List<String> valuesList = new ArrayList<>();
                final StringJoiner interro = new StringJoiner(",");

                for (final Field field : fields) {
                    columnsList.add(field.getName());
                    valuesList.add(field.get(struct).toString());
                    interro.add("?");
                }

                final String columns = " (" + columnsList.toString() + ")";
                final String sql = "INSERT INTO `" + table.name + "` " + columns + " VALUES (" + interro.toString() + ")";

                final PreparedStatement stmt = table.database.connection.prepareStatement(sql);
                for (int i = 0; i < valuesList.size(); i++)
                    stmt.setObject(i+1, valuesList.get(i));
                stmt.executeUpdate();

                stmt.close();
            }
        } catch (final SQLException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /*
    public static insert (TableManager, Map<Str, Obj>)
    Removed: https://pastebin.com/GRrCskFp
    */

    public static void insert (final TableManager<?> table, final Map<String, Object> contents) {
        try {
            if (contents.size() >= 1) {
                final StringJoiner columnsList = new StringJoiner(",");
                final List<String> valuesList = new ArrayList<>();
                final StringJoiner interro = new StringJoiner(",");

                for (final Map.Entry<String, Object> entry : contents.entrySet()) {
                    columnsList.add(entry.getKey());
                    valuesList.add(entry.getValue().toString());
                    interro.add("?");
                }

                final String columns = " (" + columnsList.toString() + ")";
                final String sql = "INSERT INTO `" + table.name + "` " + columns + " VALUES " + interro.toString();

                final PreparedStatement stmt = table.database.connection.prepareStatement(sql);
                for (int i = 0; i < valuesList.size(); i++)
                    stmt.setObject(i+1, valuesList.get(i));
                stmt.executeUpdate();

                stmt.close();
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }

    public static void insert (final TableManager<?> table, final RowBuilder builder) {
        try {
            if (builder.datas.size() >= 1) {
                final PreparedStatement stmt = table.database.connection.prepareStatement("");

                final StringJoiner columns = new StringJoiner(",");
                final StringJoiner preformat = new StringJoiner(",");

                int i = 1;
                for (final Map.Entry<String, Object> entry : builder.datas.entrySet()) {
                    columns.add(entry.getKey());
                    stmt.setObject(i++, entry.getValue());
                    preformat.add("?");
                }

                final String sql = "INSERT INTO `" + table.name + "` " + columns + " VALUES (" + preformat.toString() + ")";
                stmt.executeUpdate(sql);


                stmt.close();
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }

    public static void remove (TableManager<?> table, final RowBuilder builder) {
        try {


            if (builder.datas.size() > 0) {

                final StringJoiner whereComplements = new StringJoiner("AND ");

                final PreparedStatement stmt = table.database.connection.prepareStatement("");

                int i = 1;
                for (final Map.Entry<String, Object> entry : builder.datas.entrySet()) {
                    whereComplements.add("`" + entry.getKey() + "` = ?");
                    stmt.setObject(i++, entry.getValue());
                }

                final String sql = "DELETE FROM `" + table.name + "` WHERE " + whereComplements.toString();
                stmt.executeUpdate(sql);

                stmt.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static <D> Set<D> getRowLimited (final TableManager<D> table, final RowBuilder contents, int count) {
        final Set<D> res = new HashSet<>();
        try {
            if (contents.datas.size() > 0) {
                final StringJoiner sj = new StringJoiner("AND ");

                final PreparedStatement stmt = table.database.connection.prepareStatement("");

                int i = 1;
                for (final Map.Entry<String, Object> entry : contents.datas.entrySet()) {
                    sj.add(entry.getKey() + "= ?");
                    stmt.setObject(i++, entry.getValue());
                }

                final String sql = "SELECT * FROM `" + table.name + "` WHERE " + sj.toString();
                final ResultSet rs = stmt.executeQuery(sql);

                final ResultSetMetaData data = rs.getMetaData();

                while (count-- > 0 && rs.next()) {
                    final D content = table.defaultInstance;
                    for (int c=0; i<data.getColumnCount(); c++)
                        content.getClass().getField(data.getColumnName(c + 1)).set(content, rs.getObject(c + 1));
                    res.add(content);
                }
                rs.close();

                stmt.close();
            }
        } catch (SQLException | NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return res;
    }

    public static <D> Set<D> getRow (final TableManager<D> table, final RowBuilder contents) {
        return getRowLimited(table, contents, Integer.MAX_VALUE);
    }

    public static <D> void update (final TableManager<D> table, final RowBuilder pattern, final RowBuilder replacement) {
        try {
            final StringBuilder sql = new StringBuilder("UPDATE `" + table.name + "` SET ");

            final StringJoiner repComa = new StringJoiner(",");
            for (final String key : replacement.datas.keySet())
                repComa.add(key + "=?");
            sql.append(repComa.toString());

            sql.append(" WHERE ");

            final StringJoiner patComa = new StringJoiner(" AND ");
            for (final String key : pattern.datas.keySet())
                patComa.add(key + "=?");
            sql.append(patComa.toString());

            final PreparedStatement stmt = table.database.connection.prepareStatement(sql.toString());

            int i = 1;
            for (final Object val : replacement.datas.values())
                stmt.setObject(i++, val);
            for (final Object val : pattern.datas.values())
                stmt.setObject(i++, val);

            stmt.executeUpdate();
        } catch (final SQLException e) {
           e.printStackTrace();
        }
    }

}
