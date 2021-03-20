package wtf.listenia.citronDB.internal;

import wtf.listenia.citronDB.api.TableManager;
import wtf.listenia.citronDB.api.annotations.Primary;
import wtf.listenia.citronDB.api.annotations.Unique;
import wtf.listenia.citronDB.api.builders.RowBuilder;
import wtf.listenia.citronDB.utils.ColumnType;
import wtf.listenia.citronDB.utils.Instancier;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;

public class Requests {

    public static void createTable (final TableManager<?> table) {
        createTable(table, false);
    }

    public static void createTable (final TableManager<?> table, final boolean update) {
        try {
            final StringJoiner sj = new StringJoiner(", ");
            boolean hasPrimary = false;

            final Field[] fields = table.defaultInstance.getFields();
            for (final Field field : fields) {

                final Unique unique = field.getAnnotation(Unique.class);
                if (unique != null) {
                    sj.add(ColumnType.getFormat(field, false));
                } else if (!hasPrimary) {
                    final Primary primary = field.getAnnotation(Primary.class);
                    if (primary != null) {
                        hasPrimary = true;
                        sj.add(ColumnType.getFormat(field, false));
                    }
                } else {
                    sj.add(ColumnType.getFormat(field, false));
                }
            }

            final Statement stmt = table.database.getStatementWithException();

            final String sql = "CREATE TABLE IF NOT EXISTS `" + table.name + "` (" + sj.toString() + ");";
            final int status = stmt.executeUpdate(sql);

            stmt.close();

            if (update && status == 0) {
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
            final List<String> local = new ArrayList<>();
            final List<String> distant = new ArrayList<>();

            final Statement stmt = table.database.getStatementWithException();
            final ResultSet rs = stmt.executeQuery("SELECT * FROM `" + table.name + "` WHERE 1 = 0;"); // select nothing
            final ResultSetMetaData meta = rs.getMetaData();
            for (int i = 0; i < meta.getColumnCount(); i++)
                distant.add(meta.getColumnName(i + 1));
            rs.close();

            final Class<?> instance = table.defaultInstance;
            for (final Field field : table.defaultInstance.getFields())
                local.add(field.getName()); // + " TEXT"


            if (local.size() > 0 || distant.size() > 0) {
                final StringJoiner toDelete = new StringJoiner(",");
                final StringJoiner toModify = new StringJoiner(",");
                final StringJoiner toCreate = new StringJoiner(",");

                for (final String candid : distant) {
                    if (!local.contains(candid)) {
                        toDelete.add("DROP " + candid);
                    } else {
                        final Field field = instance.getField(candid);
                        toModify.add("MODIFY " + ColumnType.getFormat(field, false));
                    }
                }

                local.removeAll(distant); // no-GC
                distant.clear(); // GC-only

                for (final String candid : local) {
                    final Field field = instance.getField(candid);
                    toCreate.add(ColumnType.getFormat(field));
                }

                final StringJoiner actions = new StringJoiner(", ");
                boolean mustSend = false;

                if (toDelete.length() > 0) {
                    actions.add(toDelete.toString());
                    mustSend = true;
                }
                if (toModify.length() > 0) {
                    actions.add(toModify.toString());
                    mustSend = true;
                }
                if (toCreate.length() > 0) {
                    actions.add("ADD(" + toCreate.toString() + ")");
                    mustSend = true;
                }

                if (mustSend) // If request is complete
                    stmt.execute("ALTER TABLE `" + table.name + "` " + actions.toString() + ";");
            }

            stmt.close();
        } catch (final SQLException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public static void removeTable (final TableManager<?> table) {
        try {
            final Statement stmt = table.database.getStatementWithException();
            stmt.execute("DROP TABLE IF EXISTS `" + table.name + "`");
            stmt.close();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }

    public static <D> void insert (final TableManager<?> table, final D struct) {
        try {
            final Field[] fields = struct.getClass().getFields();

            if (fields.length >= 1) {
                final StringJoiner columnsList = new StringJoiner(",");
                final List<Object> valuesList = new ArrayList<>();
                final StringJoiner interro = new StringJoiner(",");

                for (final Field field : fields) {
                    columnsList.add(field.getName());
                    valuesList.add(field.get(struct));
                    interro.add("?");
                }

                final String columns = " (" + columnsList.toString() + ")";
                final PreparedStatement stmt = table.database.getConnection().prepareStatement(
                        "INSERT INTO `" + table.name + "` " + columns + " VALUES (" + interro.toString() + ")"
                );
                for (int i = 0; i < valuesList.size(); i++)
                    stmt.setObject(i + 1, valuesList.get(i));

                stmt.executeUpdate();
                stmt.close();
            }
        } catch (final SQLException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /*
    public static insert (TableManager, Map<Str, Obj>)
    Removed: https://pastebin.com/hq8HFRwU
    */


    public static void insert (final TableManager<?> table, final RowBuilder builder) {
        try {
            if (builder.datas.size() >= 1) {
                StringJoiner columns = new StringJoiner(",");
                StringJoiner preformat = new StringJoiner(",");

                for (final String key : builder.datas.keySet()) {
                    columns.add(key);
                    preformat.add("?");
                }

                final PreparedStatement stmt = table.database.getConnection().prepareStatement(
                        "INSERT INTO `" + table.name + "` (" + columns + ") VALUES (" + preformat.toString() + ")"
                );

                columns = null; // GC
                preformat = null; // GC

                int i = 1;
                for (final Object value : builder.datas.values())
                    stmt.setObject(i++, value);

                stmt.executeUpdate();
                stmt.close();
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }

    public static void remove (TableManager<?> table, final RowBuilder builder) {
        try {
            if (builder.datas.size() > 0) {
                StringJoiner whereComplements = new StringJoiner("AND ");
                for (final String key : builder.datas.keySet())
                    whereComplements.add("`" + key + "`=?");

                final PreparedStatement stmt = table.database.getConnection().prepareStatement(
                        "DELETE FROM `" + table.name + "` WHERE " + whereComplements.toString()
                );

                whereComplements = null; // GC

                int i = 1;
                for (final Object object : builder.datas.values())
                    stmt.setObject(i++, object);

                stmt.executeUpdate();
                stmt.close();
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }


    public static <D> Set<D> getRowLimited (final TableManager<D> table, final RowBuilder pattern, int count) {


        final Set<D> res = new HashSet<>();
        try {
            if (pattern.datas.size() > 0) {
                StringBuilder interro = new StringBuilder();

                final Iterator<String> iter = pattern.datas.keySet().iterator();
                if (iter.hasNext())
                    interro.append(iter.next()).append("=?");
                while (iter.hasNext())
                    interro.append(",AND ").append(iter.next()).append("=?");

                final PreparedStatement stmt = table.database.getConnection().prepareStatement(
                        "SELECT * FROM `" + table.name + "` WHERE " + interro.toString()
                );

                interro = null; // GC

                int i = 1;
                for (final Object object : pattern.datas.values())
                    stmt.setObject(i++, object);

                final ResultSet rs = stmt.executeQuery();
                final ResultSetMetaData data = rs.getMetaData();

                while (count-- > 0 && rs.next()) {
                    final D content = Instancier.createInstance(table.defaultInstance);

                    for (int c = 1; c <= data.getColumnCount(); c++) {
                        final Object obj = rs.getObject(c);
                        if (obj != null)
                            table.defaultInstance.getField(data.getColumnName(c)).set(content, obj);
                    }
                    res.add(content);
                }
                rs.close();
                stmt.close();
            }
        } catch (final SQLException | NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return res;
    }

    public static <D> Set<D> getRow (final TableManager<D> table, final RowBuilder contents) {
        return getRowLimited(table, contents, Integer.MAX_VALUE);
    }

    public static <D> void update (final TableManager<D> table, final RowBuilder pattern, final RowBuilder replacement) {
        try {
            final StringJoiner repComa = new StringJoiner(",");
            final StringJoiner patComa = new StringJoiner(" AND ");

            for (final String key : replacement.datas.keySet())
                repComa.add(key + "=?");
            for (final String key : pattern.datas.keySet())
                patComa.add(key + "=?");

            final PreparedStatement stmt = table.database.getConnection().prepareStatement(
                    "UPDATE `" + table.name + "` SET " + repComa.toString() + " WHERE " + patComa.toString()
            );

            int i = 1;
            for (final Object val : replacement.datas.values())
                stmt.setObject(i++, val);
            for (final Object val : pattern.datas.values())
                stmt.setObject(i++, val);

            stmt.executeUpdate();
            stmt.close();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }

}
