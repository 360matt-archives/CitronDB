package wtf.listenia.citronDB.api;

import wtf.listenia.citronDB.api.builders.RowBuilder;
import wtf.listenia.citronDB.internal.Requests;
import wtf.listenia.citronDB.utils.Instancier;

import java.util.Set;

public class TableManager <D> {

    public final Database database;
    public final String name;

    public final D defaultInstance;
    public D temporary;


    public TableManager (final Database database, final String name, final Class<D> struct) {
        this.database = database;
        this.name = name;
        this.temporary = this.defaultInstance = Instancier.getEmptyRaw(struct);
    }

    public final void createTable () {
        Requests.createTable(this);
    }
    public final void delete () {
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

    public final Set<D> getLines (final RowBuilder builder, final Class<D> struct) {
        return Requests.getRow(this, builder);
    }

    public final Set<D> getLines (final RowBuilder builder, final Class<D> struct, final int limit) {
        return Requests.getRowLimited(this, builder, limit);
    }

    public final void update (final RowBuilder pattern, final RowBuilder replacement) {
        Requests.update(this, pattern, replacement);
    }

}
