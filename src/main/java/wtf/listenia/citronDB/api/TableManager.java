package wtf.listenia.citronDB.api;

import wtf.listenia.citronDB.api.annotations.Primary;
import wtf.listenia.citronDB.api.annotations.Unique;
import wtf.listenia.citronDB.api.builders.RowBuilder;
import wtf.listenia.citronDB.utils.ColumnType;
import wtf.listenia.citronDB.utils.Instancier;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;

public class TableManager <D> {

    public final Database database;
    public final String name;

    public final Class<D> defaultInstance;


    public TableManager (final Database database, final String name, final Class<D> struct) {
        this.database = database;
        this.name = name;
        this.defaultInstance = struct;
    }

    public final void createTable () {
        createTable(false);
    }

    public final void createTable (final boolean update) {
        try {
            final StringJoiner sj = new StringJoiner(", ");
            boolean hasPrimary = false;

            final Field[] fields = this.defaultInstance.getFields();
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

            final Statement stmt = this.database.getStatementWithException();

            final String sql = "CREATE TABLE IF NOT EXISTS `" + this.name + "` (" + sj.toString() + ");";
            final int status = stmt.executeUpdate(sql);

            stmt.close();

            if (update && status == 0) {
                // if the table has not been created above,
                // we must apply the new structure by an ALTER.
                updateStructure();
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }


    public final void deleteTable () {
        try {
            final Statement stmt = this.database.getStatementWithException();
            stmt.execute("DROP TABLE IF EXISTS `" + this.name + "`");
            stmt.close();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }


    public void updateStructure () {
        try {
            final List<String> local = new ArrayList<>();
            final List<String> distant = new ArrayList<>();

            final Statement stmt = this.database.getStatementWithException();
            final ResultSet rs = stmt.executeQuery("SELECT * FROM `" + this.name + "` WHERE 1 = 0;"); // select nothing
            final ResultSetMetaData meta = rs.getMetaData();
            for (int i = 0; i < meta.getColumnCount(); i++)
                distant.add(meta.getColumnName(i + 1));
            rs.close();

            final Class<?> instance = this.defaultInstance;
            for (final Field field : this.defaultInstance.getFields())
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
                    stmt.execute("ALTER TABLE `" + this.name + "` " + actions.toString() + ";");
            }

            stmt.close();
        } catch (final SQLException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }


    public final void insert (final D line) {
        try {
            final Field[] fields = defaultInstance.getFields();

            if (fields.length >= 1) {
                final StringJoiner columnsList = new StringJoiner(",");
                final List<Object> valuesList = new ArrayList<>();
                final StringJoiner interro = new StringJoiner(",");

                for (final Field field : fields) {
                    columnsList.add(field.getName());
                    valuesList.add(field.get(line));
                    interro.add("?");
                }

                final String columns = " (" + columnsList.toString() + ")";
                final PreparedStatement stmt = this.database.getConnection().prepareStatement(
                        "INSERT INTO `" + this.name + "` " + columns + " VALUES (" + interro.toString() + ")"
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

    public final void insert (final RowBuilder builder) {
        try {
            if (builder.datas.size() >= 1) {
                StringJoiner columns = new StringJoiner(",");
                StringJoiner preformat = new StringJoiner(",");

                for (final String key : builder.datas.keySet()) {
                    columns.add(key);
                    preformat.add("?");
                }

                final PreparedStatement stmt = this.database.getConnection().prepareStatement(
                        "INSERT INTO `" + this.name + "` (" + columns + ") VALUES (" + preformat.toString() + ")"
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


    public final void remove (final RowBuilder builder) {
        try {
            if (builder.datas.size() > 0) {
                StringJoiner whereComplements = new StringJoiner("AND ");
                for (final String key : builder.datas.keySet())
                    whereComplements.add("`" + key + "`=?");

                final PreparedStatement stmt = this.database.getConnection().prepareStatement(
                        "DELETE FROM `" + this.name + "` WHERE " + whereComplements.toString()
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


    public final boolean exist (final RowBuilder pattern) {
        try {
            final StringBuilder interro = new StringBuilder();

            final Iterator<String> iter = pattern.datas.keySet().iterator();
            if (iter.hasNext())
                interro.append(iter.next()).append("=?");
            while (iter.hasNext())
                interro.append(",AND ").append(iter.next()).append("=?");

            final PreparedStatement stmt = this.database.getConnection().prepareStatement(
                    "SELECT 1 FROM `" + this.name + "` WHERE " + interro.toString()
            );

            int i = 1;
            for (final Object object : pattern.datas.values())
                stmt.setObject(i++, object);

            final ResultSet rs = stmt.executeQuery();
            final boolean state = rs.next();

            rs.close();
            stmt.close();

            return state;
        } catch (final SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    public final Set<D> getLines (final RowBuilder builder) {
        return getLines(builder, Integer.MAX_VALUE);
    }

    public final D getLine (final RowBuilder pattern) {
        D content = null;
        try {
            if (pattern.datas.size() > 0) {
                final StringBuilder interro = new StringBuilder();

                final Iterator<String> iter = pattern.datas.keySet().iterator();
                if (iter.hasNext())
                    interro.append(iter.next()).append("=?");
                while (iter.hasNext())
                    interro.append(",AND ").append(iter.next()).append("=?");

                final PreparedStatement stmt = this.database.getConnection().prepareStatement(
                        "SELECT 1 FROM `" + this.name + "` WHERE " + interro.toString()
                );

                int i = 1;
                for (final Object object : pattern.datas.values())
                    stmt.setObject(i++, object);

                final ResultSet rs = stmt.executeQuery();
                final ResultSetMetaData data = rs.getMetaData();


                if (rs.next()) {
                    content = Instancier.createInstance(this.defaultInstance);
                    for (int c = 1; c <= data.getColumnCount(); c++) {
                        final Object obj = rs.getObject(c);
                        if (obj != null)
                            this.defaultInstance.getField(data.getColumnName(c)).set(content, obj);
                    }
                }

                rs.close();
                stmt.close();
            }
        } catch (final SQLException | NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return content;
    }


    public final Set<D> getLines (final RowBuilder pattern, int limit) {
        final Set<D> res = new HashSet<>();
        try {
            if (pattern.datas.size() > 0) {
                final StringBuilder interro = new StringBuilder();

                final Iterator<String> iter = pattern.datas.keySet().iterator();
                if (iter.hasNext())
                    interro.append(iter.next()).append("=?");
                while (iter.hasNext())
                    interro.append(",AND ").append(iter.next()).append("=?");

                final PreparedStatement stmt = this.database.getConnection().prepareStatement(
                        "SELECT " + limit + " FROM `" + this.name + "` WHERE " + interro.toString()
                );

                int i = 1;
                for (final Object object : pattern.datas.values())
                    stmt.setObject(i++, object);

                final ResultSet rs = stmt.executeQuery();
                final ResultSetMetaData data = rs.getMetaData();

                while (limit-- > 0 && rs.next()) {
                    final D content = Instancier.createInstance(this.defaultInstance);

                    for (int c = 1; c <= data.getColumnCount(); c++) {
                        final Object obj = rs.getObject(c);
                        if (obj != null)
                            this.defaultInstance.getField(data.getColumnName(c)).set(content, obj);
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


    public final void update (final RowBuilder pattern, final RowBuilder replacement) {
        try {
            final StringJoiner repComa = new StringJoiner(",");
            final StringJoiner patComa = new StringJoiner(" AND ");

            for (final String key : replacement.datas.keySet())
                repComa.add(key + "=?");
            for (final String key : pattern.datas.keySet())
                patComa.add(key + "=?");

            final PreparedStatement stmt = this.database.getConnection().prepareStatement(
                    "UPDATE `" + this.name + "` SET " + repComa.toString() + " WHERE " + patComa.toString()
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
