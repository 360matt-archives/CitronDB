package wtf.listenia.citronDB.api;

import wtf.listenia.citronDB.api.builders.RowBuilder;
import wtf.listenia.citronDB.internal.Requests;

import java.util.Set;

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
        Requests.createTable(this);
    }
    public final void createTable (final boolean update) {
        Requests.createTable(this, update);
    }
    public final void deleteTable () {
        Requests.removeTable(this);
    }
    public void updateStructure () {
        Requests.updateTable(this);
    }

    public final void insert (final D line) {
        Requests.insert(this, line);
    }
    public final void insert (final RowBuilder builder) {
        Requests.insert(this, builder);
    }

    public final void remove (final RowBuilder builder) {
        Requests.remove(this, builder);
    }

    public final Set<D> getLines (final RowBuilder builder) {
        return Requests.getRow(this, builder);
    }

    public final D getLine (final RowBuilder builder) {
        final Set<D> res = Requests.getRowLimited(this, builder, 1);
        return (res.size() > 0) ? (D) res.toArray()[0] : null;
    }

    public final Set<D> getLines (final RowBuilder builder, final int limit) {
        return Requests.getRowLimited(this, builder, limit);
    }

    public final void update (final RowBuilder pattern, final RowBuilder replacement) {
        Requests.update(this, pattern, replacement);
    }

}
