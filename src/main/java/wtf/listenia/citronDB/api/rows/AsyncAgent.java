package wtf.listenia.citronDB.api.rows;

import wtf.listenia.citronDB.api.tables.GuestTable;
import wtf.listenia.citronDB.api.tables.Table;
import wtf.listenia.citronDB.internal.MultipleRequests;
import wtf.listenia.citronDB.internal.Requests;

import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.*;

public class AsyncAgent {
    public final GuestTable table;
    public final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public AsyncAgent(GuestTable table) {
        this.table = table;
    }

    public void add (HashMap<String, Object> value) {
        executorService.submit(() -> {
            Requests.insertMechanist(this.table, value);
            if (executorService.isTerminated())
                executorService.shutdown();
        });
    }

    public void addMultiple (Collection<Table.Row> rows) {
        executorService.submit(() -> {
            MultipleRequests.insertMultipleFromSetRow(this.table, rows);
            if (executorService.isTerminated())
                executorService.shutdown();
        });
    }

    public void remove (Table.Row row) {
        executorService.submit(() -> {
            Requests.removeRow((Table) this.table, row);
            if (executorService.isTerminated())
                executorService.shutdown();
        });
    }
    public void remove (RowBuilder row) {
        executorService.submit(() -> {
            Requests.removeFromBuilder(this.table, row);
            if (executorService.isTerminated())
                executorService.shutdown();
        });
    }
}
