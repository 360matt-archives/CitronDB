package wtf.listenia.citronDB.api.tables;

import wtf.listenia.citronDB.api.rows.AsyncAgent;
import wtf.listenia.citronDB.api.rows.RowBuilder;
import wtf.listenia.citronDB.api.database.Database;
import wtf.listenia.citronDB.internal.Requests;

import java.util.Set;

public class GuestTable {
    public final String tableName;
    public final Database database;
    public final GuestTable table;
    public final AsyncAgent asyncInsert;





    public GuestTable (String tableName, Database database) {
        this.tableName = tableName;
        this.database = database;
        this.table = this;
        this.asyncInsert = new AsyncAgent(this);
    }


    public final void insertRow (RowBuilder row) {
        asyncInsert.add(row.content);
    }
    public final void removeRow (RowBuilder row) {
        this.asyncInsert.remove(row);
    }

    public final Set<RowBuilder> getRowFromBuilder (RowBuilder row, int count) {
        return Requests.getRowBuilderFromBuilder(this, row, count);
    }
    public final RowBuilder getRowFromBuilder (RowBuilder row) {
        return Requests.getRowBuilderFromBuilder(this, row, 1).iterator().next();
    }







    public final void setStructure (TableBuilder tableBuilder) {
        Requests.createTableFromBuilder(this.table, tableBuilder);
    }

    public final void removeTable () {
        Requests.removeTable(this.table);
    }

}
