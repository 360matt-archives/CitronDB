package wtf.listenia.citronDB.api.tables;

import wtf.listenia.citronDB.api.database.DatabaseOption;
import wtf.listenia.citronDB.api.database.Database;
import wtf.listenia.citronDB.internal.Requests;
import wtf.listenia.citronDB.internal.RowsUtils;

import java.util.Collection;

public abstract class Table extends GuestTable {
    public static abstract class Row {};

    public Row modelRow() {
        return null;
    }


    public Table (String tableName, Database database) {
        super(tableName, database);

        if ((database.options & DatabaseOption.AutoUpdate) == DatabaseOption.AutoUpdate)
            updateStructure();
    }



    public final void insertRow (Row row) { this.asyncInsert.add(RowsUtils.rowToHashMap(row)); }
    public final void insertMultipleRow (Collection<Row> rows) { this.asyncInsert.addMultiple(rows); }


    public final void removeRow (Row row) {
        this.asyncInsert.remove(row);
    }


    /*
    public final Set<Row> getRow (RowBuilder builder, int count) {
        return Requests.getRowFromBuilder(this, builder, modelRow(), count);
    }
    public final Row getRow (RowBuilder builder) {
        return Requests.getRowFromBuilder(this, builder, modelRow(), 1).iterator().next();
    }
     */



    public final void updateStructure () {
        Requests.createTableFromRow(this);
    }

}
