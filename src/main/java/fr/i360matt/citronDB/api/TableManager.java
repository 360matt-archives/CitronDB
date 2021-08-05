package fr.i360matt.citronDB.api;

import com.esotericsoftware.reflectasm.ConstructorAccess;
import com.esotericsoftware.reflectasm.FieldAccess;
import fr.i360matt.citronDB.api.annotations.Primary;
import fr.i360matt.citronDB.api.annotations.Unique;

import fr.i360matt.citronDB.utils.ColumnType;
import fr.i360matt.citronDB.utils.TableCache;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;

public class TableManager <D> {

    public final Database database;
    public final String name;

    private final TableCache tableCache;
    private final FieldAccess fieldAccess;
    private final ConstructorAccess constructorAccess;

    public final Class<D> defaultInstance;
    public final Map<String, Object> defaultAsMap;


    public TableManager (final Database database, final String name, final Class<D> struct) {
        this.database = database;
        this.name = name;

        this.tableCache = new TableCache(name, struct.getFields());
        this.fieldAccess = FieldAccess.get(struct);
        this.constructorAccess = ConstructorAccess.get(struct);


        this.defaultInstance = struct;
        this.defaultAsMap = new HashMap<>();

        final D def = (D) this.constructorAccess.newInstance();

        final Field[] fields = this.fieldAccess.getFields();
        for (int i = 0; i < this.fieldAccess.getFieldCount(); i++) {
            this.defaultAsMap.put(fields[i].getName(), this.fieldAccess.get(def, i));
        }

    }

    public final void createTable () {
        createTable(false);
    }

    public final void createTable (final boolean update) {
        final Field[] fields = this.defaultInstance.getFields();
        if (fields.length == 0)
            return;

        boolean hasPrimary = false;

        final StringBuilder resSQL = new StringBuilder();
        resSQL.append("CREATE TABLE IF NOT EXISTS `");
        resSQL.append(this.name);
        resSQL.append("` (");


        for (int i = 0; i < fields.length; i++) {
            final Field field = fields[i];
            if (field.getAnnotation(Unique.class) != null) {
                if (i > 0) resSQL.append(',');
                resSQL.append(ColumnType.getFormat(field, false));
            } else if (!hasPrimary) {
                if (field.getAnnotation(Primary.class) != null) {
                    hasPrimary = true;
                    if (i > 0) resSQL.append(',');
                    resSQL.append(ColumnType.getFormat(field, false));
                }
            } else {
                resSQL.append(',');
                resSQL.append(ColumnType.getFormat(field, false));
            }
        }

        resSQL.append(')');




        try (final Statement stmt = this.database.getStatementWithException()) {
            final int status = stmt.executeUpdate(resSQL.toString());

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
        final String sql = "SELECT * FROM `" + this.name + "` WHERE 1 = 0;";

        try (
                final Statement stmt = this.database.getStatementWithException();
                final ResultSet rs = stmt.executeQuery(sql);
         ) {
            final ResultSetMetaData meta = rs.getMetaData();

            final List<String> distant = new ArrayList<>();
            for (int i = 0; i < meta.getColumnCount(); i++)
                distant.add(meta.getColumnName(i + 1));
            rs.close();

            Field[] local = this.defaultInstance.getFields();

            if (local.length == 0 || distant.size() == 0) {
                stmt.close();
                return;
            }

            final StringBuilder resSQL = new StringBuilder();
            resSQL.append("ALTER TABLE `");
            resSQL.append(this.name);
            resSQL.append("` ");


            boolean firstWasAdded = false;
            for (final Field field : local) {
                if (!distant.contains(field.getName())) {
                    if (!firstWasAdded) {
                        resSQL.append("ADD(");
                        firstWasAdded = true;
                    } else {
                        resSQL.append(',');
                    }
                    resSQL.append(ColumnType.getFormat(field, false));
                }
            }

            if (firstWasAdded) {
                // if there are minimum one column
                resSQL.append(')');
                stmt.execute(resSQL.toString());
            }

        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }


    public final void insert (final D line) {
        final Field[] fields = defaultInstance.getFields();
        if (fields.length >= 1) {
            try (final PreparedStatement stmt = this.database.getConnection().prepareStatement(this.tableCache.getInsertSQL())) {
                for (int i = 0; i < fields.length; i++)
                    stmt.setObject(i + 1, fieldAccess.get(line, i));
                stmt.executeUpdate();
            } catch (final SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public final void insert (final Map<String, Object> datas) {
        if (datas.size() >= 1) {
            final StringBuilder resSQL = new StringBuilder();
            resSQL.append("INSERT INTO `");
            resSQL.append(this.name);
            resSQL.append("` (");


            final StringBuilder preformat = new StringBuilder();

            final Object[] values = new Object[datas.size()];

            int ind = 0;
            for (final Map.Entry<String, Object> entry : defaultAsMap.entrySet()) {
                Object value = datas.get(entry.getKey());
                if (value != null)
                    value = defaultAsMap.get(entry.getKey());
                values[ind] = value;

                if (ind > 0) {
                    resSQL.append(',');
                    preformat.append(',');
                }

                resSQL.append(value);
                preformat.append('?');

                ind++;
            }

            resSQL.append(") VALUES (");
            resSQL.append(preformat);
            resSQL.append(')');


            try (final PreparedStatement stmt = this.database.getConnection().prepareStatement(resSQL.toString())) {
                for (int i = 0; i < values.length; i++)
                    stmt.setObject(i + 1, values[i]);
                stmt.executeUpdate();
            } catch (final SQLException e) {
                e.printStackTrace();
            }
        }
    }


    public final void remove (final Map<String, Object> datas) {
        if (datas.size() > 0) {
            final StringBuilder resSQL = new StringBuilder();
            resSQL.append("DELETE FROM `");
            resSQL.append(this.name);
            resSQL.append("` WHERE ");

            final Object[] values = new Object[datas.size()];

            int ind = 0;
            for (final String key : datas.keySet()) {
                values[ind] = key;
                if (ind > 0)
                    resSQL.append(',');
                resSQL.append('`');
                resSQL.append(key);
                resSQL.append("`=?");
                ind++;
            }

            try (final PreparedStatement stmt = this.database.getConnection().prepareStatement(resSQL.toString())) {
                for (int i = 0; i < values.length; i++)
                    stmt.setObject(i+1, values[i]);
                stmt.executeUpdate();
            } catch (final SQLException e) {
                e.printStackTrace();
            }
         }
    }


    public final boolean exist (final Map<String, Object> pattern) {
        try (
                final PreparedStatement statement = this.whereStatement(pattern);
                final ResultSet rs = statement.executeQuery();
        ) {
            return rs.next();
        } catch (final SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    public final Set<D> getLines (final Map<String, Object> pattern) {
        return getLines(pattern, Integer.MAX_VALUE);
    }

    public final D getLine (final Map<String, Object> pattern) {
        if (pattern.size() > 0) {
            try (
                    final PreparedStatement statement = this.whereStatement(pattern);
                    final ResultSet rs = statement.executeQuery();
            ) {
                final ResultSetMetaData data = rs.getMetaData();
                if (rs.next()) {
                    final D content = (D) this.constructorAccess.newInstance();
                    for (int c = 1; c <= data.getColumnCount(); c++) {
                        final Object obj = rs.getObject(c);
                        if (obj != null) {
                            this.fieldAccess.set(content, data.getColumnName(c), obj);
                        }
                    }
                    return content;
                }
            } catch (final SQLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }


    public final Set<D> getLines (final Map<String, Object> pattern, int limit) {
        final Set<D> res = new HashSet<>();
        if (pattern.size() > 0) {
            final StringBuilder resSQL = new StringBuilder();
            resSQL.append("SELECT ");
            resSQL.append(limit);
            resSQL.append(" FROM `");
            resSQL.append(this.name);
            resSQL.append("` WHERE ");

            final Object[] values = new Object[pattern.size()];

            int ind = 0;
            for (final Map.Entry<String, Object> entry : pattern.entrySet()) {
                values[ind] = entry.getValue();
                if (ind > 0)
                    resSQL.append(",AND ");
                resSQL.append(entry.getKey());
                resSQL.append("=?");
                ind++;
            }

            try (final PreparedStatement stmt = this.database.getConnection().prepareStatement(resSQL.toString())) {
                for (int i = 0; i < values.length; i++)
                    stmt.setObject(i+1, values[i]);

                try (final ResultSet rs = stmt.executeQuery()) {
                    final ResultSetMetaData data = rs.getMetaData();

                    while (limit-- > 0 && rs.next()) {
                        final D content = (D) this.constructorAccess.newInstance();

                        for (int c = 1; c <= data.getColumnCount(); c++) {
                            final Object obj = rs.getObject(c);
                            if (obj != null)
                                this.fieldAccess.set(content, data.getColumnName(c), obj);
                        }
                        res.add(content);
                    }
                }
            } catch (final SQLException e) {
                e.printStackTrace();
            }
        }
        return res;
    }


    public final void update (final Map<String, Object> pattern, final Map<String, Object> replacement) {
        if (replacement.size() > 0 && pattern.size() > 0) {

            final StringBuilder resSQL = new StringBuilder();
            resSQL.append("UPDATE `");
            resSQL.append(this.name);
            resSQL.append("` SET ");


            final Object[] replaceValues = new Object[replacement.size()];
            final Object[] patternValues = new Object[pattern.size()];

            int ind = 0;
            for (final Map.Entry<String, Object> entry : replacement.entrySet()) {
                replaceValues[ind] = entry.getValue();
                if (ind > 0)
                    resSQL.append(',');
                resSQL.append(entry.getKey());
                resSQL.append("=?");
                ind++;
            }

            resSQL.append(" WHERE ");

            ind = 0;
            for (final Map.Entry<String, Object> entry : pattern.entrySet()) {
                patternValues[ind] = entry.getValue();
                if (ind > 0)
                    resSQL.append(",AND ");
                resSQL.append(entry.getKey());
                resSQL.append("=?");
                ind++;
            }

            try (final PreparedStatement stmt = this.database.getConnection().prepareStatement(resSQL.toString())) {
                int i = 1;
                for (final Object val : replaceValues)
                    stmt.setObject(i++, val);
                for (final Object val : patternValues)
                    stmt.setObject(i++, val);
                stmt.executeUpdate();
            } catch (final SQLException e) {
                e.printStackTrace();
            }
        }
    }




    // ______________________________________________________________________________________ //

    private PreparedStatement whereStatement (final Map<String, Object> pattern) throws SQLException {
        final StringBuilder resSQL = new StringBuilder();
        resSQL.append("SELECT * FROM `");
        resSQL.append(this.name);
        resSQL.append("` WHERE ");

        final Object[] values = new Object[pattern.size()];

        int ind = 0;
        for (final Map.Entry<String, Object> entry : pattern.entrySet()) {
            resSQL.append(entry.getKey());
            values[ind] = entry.getValue();
            if (ind > 0)
                resSQL.append(" AND ");
            resSQL.append("=?");
            ind++;
        }

        final PreparedStatement stmt = this.database.getConnection().prepareStatement(resSQL.toString());
        for (int i = 0; i < values.length; i++)
            stmt.setObject(i+1, values[i]);
        return stmt;
    }

}
