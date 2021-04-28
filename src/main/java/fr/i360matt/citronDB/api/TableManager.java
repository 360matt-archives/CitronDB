package fr.i360matt.citronDB.api;

import fr.i360matt.citronDB.api.annotations.Primary;
import fr.i360matt.citronDB.api.annotations.Unique;
import fr.i360matt.citronDB.api.builders.RowBuilder;
import fr.i360matt.citronDB.utils.ColumnType;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class TableManager <D> {

    public final Database database;
    public final String name;

    public final Class<D> defaultInstance;
    public final Map<String, Object> defaultAsMap;


    public TableManager (final Database database, final String name, final Class<D> struct) {
        this.database = database;
        this.name = name;
        this.defaultInstance = struct;
        this.defaultAsMap = new HashMap<>();

        try {
            final D def = struct.newInstance();
            for (final Field field : defaultInstance.getFields())
                this.defaultAsMap.put(field.getName(), field.get(def));
        } catch (final IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }
    }

    public final void createTable () {
        createTable(false);
    }

    public final void createTable (final boolean update) {
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

        try (final Statement stmt = this.database.getStatementWithException()) {
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
        try (final Statement stmt = this.database.getStatementWithException();) {
            stmt.execute("DROP TABLE IF EXISTS `" + this.name + "`");
        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }


    public void updateStructure () {
        final Map<String, Field> local = new HashMap<>();
        final List<String> distant = new ArrayList<>();

        final String sql = "SELECT * FROM `" + this.name + "` WHERE 1 = 0;";

        try (
                final Statement stmt = this.database.getStatementWithException();
                final ResultSet rs = stmt.executeQuery(sql);
         ) {
            final ResultSetMetaData meta = rs.getMetaData();
            for (int i = 0; i < meta.getColumnCount(); i++)
                distant.add(meta.getColumnName(i + 1));
            rs.close();

            for (final Field field : this.defaultInstance.getFields())
                local.put(field.getName(), field); // + " TEXT"

            if (local.size() == 0 || distant.size() == 0) {
                stmt.close();
                return;
            }


            final List<String> toCreate = new ArrayList<>(local.keySet());
            toCreate.removeAll(distant);
            final List<String> toDelete = new ArrayList<>(distant);
            toDelete.removeAll(local.keySet());
            final List<String> toModify = new ArrayList<>(local.keySet());
            toModify.removeAll(toCreate);

            final StringJoiner action = new StringJoiner(",");
            boolean mustSend = false;

            if (toCreate.size() > 0) {
                action.add("ADD(" + toCreate.stream()
                        .map(x -> ColumnType.getFormat(local.get(x), false))
                        .collect(Collectors.joining(",")) +
                        ")");
                mustSend = true;
                }
            if (toDelete.size() > 0) {
                action.add("DROP " + String.join(",DROP ", toDelete));
                mustSend = true;
            }
            if (toModify.size() > 0) {
                action.add("MODIFY " + toModify.stream()
                        .map(x -> ColumnType.getFormat(local.get(x), false))
                        .collect(Collectors.joining(",MODIFY "))
                );
                mustSend = true;
            }

            if (mustSend) // If request is complete
                // deepcode ignore Sqli: < Tkt bro >
                stmt.execute("ALTER TABLE `" + this.name + "` " + action.toString() + ";");

        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }


    public final void insert (final D line) {
        final Field[] fields = defaultInstance.getFields();
        if (fields.length >= 1) {
            final StringJoiner columnsList = new StringJoiner(",");
            final List<Object> valuesList = new ArrayList<>();
            final StringJoiner interro = new StringJoiner(",");

            try {
                for (final Field field : fields) {
                    columnsList.add(field.getName());
                    valuesList.add(field.get(line));
                    interro.add("?");
                }

                final String columns = " (" + columnsList.toString() + ")";
                final String sql = "INSERT INTO `" + this.name + "` " + columns + " VALUES (" + interro.toString() + ")";

                try (final PreparedStatement stmt = this.database.getConnection().prepareStatement(sql);) {
                    for (int i = 0; i < valuesList.size(); i++)
                        stmt.setObject(i + 1, valuesList.get(i));

                    stmt.executeUpdate();
                } catch (final SQLException e) {
                    e.printStackTrace();
                }
            } catch (final IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public final void insert (final RowBuilder builder) {
        if (builder.datas.size() >= 1) {
            for (final Map.Entry<String, Object> entry : defaultAsMap.entrySet())
                builder.datas.putIfAbsent(entry.getKey(), entry.getValue());

            final StringJoiner columns = new StringJoiner(",");
            final StringJoiner preformat = new StringJoiner(",");

            for (final String key : builder.datas.keySet()) {
                columns.add(key);
                preformat.add("?");
            }

            final String sql = "INSERT INTO `" + this.name + "` (" + columns + ") VALUES (" + preformat.toString() + ")";
            try (final PreparedStatement stmt = this.database.getConnection().prepareStatement(sql);) {
                int i = 1;
                for (final Object value : builder.datas.values())
                    stmt.setObject(i++, value);
                stmt.executeUpdate();
            } catch (final SQLException e) {
                e.printStackTrace();
            }
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
        try (
                final PreparedStatement statement = getStatementForOneLine(pattern);
                final ResultSet rs = statement.executeQuery();
        ) {
            final boolean state = rs.next();
            rs.getStatement().close();
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

        if (pattern.datas.size() > 0) {
            try (
                    final PreparedStatement statement = getStatementForOneLine(pattern);
                    final ResultSet rs = statement.executeQuery();
            ) {
                final ResultSetMetaData data = rs.getMetaData();
                if (rs.next()) {
                    content = this.defaultInstance.newInstance();
                    for (int c = 1; c <= data.getColumnCount(); c++) {
                        final Object obj = rs.getObject(c);
                        if (obj != null) {

                            this.defaultInstance.getField(data.getColumnName(c)).set(content, obj);
                        }
                    }
                }
            } catch (final SQLException | NoSuchFieldException | IllegalAccessException | InstantiationException e) {
                e.printStackTrace();
            }
        }
        return content;
    }


    public final Set<D> getLines (final RowBuilder pattern, int limit) {
        final Set<D> res = new HashSet<>();
        if (pattern.datas.size() > 0) {
            final StringBuilder interro = new StringBuilder();

            final Iterator<String> iter = pattern.datas.keySet().iterator();
            if (iter.hasNext())
                interro.append(iter.next()).append("=?");
            while (iter.hasNext())
                interro.append(",AND ").append(iter.next()).append("=?");

            final String sql = "SELECT " + limit + " FROM `" + this.name + "` WHERE " + interro.toString();
            try (final PreparedStatement stmt = this.database.getConnection().prepareStatement(sql)) {
                int i = 1;
                for (final Object object : pattern.datas.values())
                    stmt.setObject(i++, object);


                try (final ResultSet rs = stmt.executeQuery()) {
                    final ResultSetMetaData data = rs.getMetaData();

                    while (limit-- > 0 && rs.next()) {
                        final D content = this.defaultInstance.newInstance();

                        for (int c = 1; c <= data.getColumnCount(); c++) {
                            final Object obj = rs.getObject(c);
                            if (obj != null)
                                this.defaultInstance.getField(data.getColumnName(c)).set(content, obj);
                        }
                        res.add(content);
                    }
                }
            } catch (final SQLException | NoSuchFieldException | IllegalAccessException | InstantiationException e) {
                e.printStackTrace();
            }
        }
        return res;
    }


    public final void update (final RowBuilder pattern, final RowBuilder replacement) {
        if (replacement.datas.size() > 0) {

            /*
            for (final Map.Entry<String, Object> entry : defaultAsMap.entrySet())
                replacement.datas.putIfAbsent(entry.getKey(), entry.getValue());
             */

            final StringJoiner repComa = new StringJoiner(",");
            final StringJoiner patComa = new StringJoiner(" AND ");

            for (final String key : replacement.datas.keySet())
                repComa.add(key + "=?");
            for (final String key : pattern.datas.keySet())
                patComa.add(key + "=?");

            final String sql = "UPDATE `" + this.name + "` SET " + repComa.toString() + " WHERE " + patComa.toString();
            try (final PreparedStatement stmt = this.database.getConnection().prepareStatement(sql)) {
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




    // ______________________________________________________________________________________ //

    private PreparedStatement getStatementForOneLine (final RowBuilder pattern) throws SQLException {
        final StringBuilder interro = new StringBuilder();

        final Iterator<String> iter = pattern.datas.keySet().iterator();
        if (iter.hasNext())
            interro.append(iter.next()).append("=?");
        while (iter.hasNext())
            interro.append(" AND ").append(iter.next()).append("=?");

        final String sql = "SELECT * FROM `" + this.name + "` WHERE " + interro.toString();

        final PreparedStatement stmt = this.database.getConnection().prepareStatement(sql);
        int i = 1;
        for (final Object object : pattern.datas.values())
            stmt.setObject(i++, object);
        return stmt;
    }

}
