package wtf.listenia.citronDB.internal;

import wtf.listenia.citronDB.api.tables.GuestTable;
import wtf.listenia.citronDB.api.tables.Table;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.util.*;

public class MultipleRequests {

    public static void insertMultipleFromSetRow (GuestTable table, Collection<Table.Row> rows) {
        try {
            if (rows.size() > 0) {
                final List<String> column = new ArrayList<>();

                final Table.Row model = rows.iterator().next();
                for (Field field : model.getClass().getFields())
                    column.add(field.getName());

                final String columnsFormat = String.join(",", column);
                final String valuesChained = ("(" + String.join(",", "?".repeat(column.size()).split("")) + "),").repeat(rows.size());
                final String allValues = valuesChained.substring(0, valuesChained.length() - 1);

                column.clear();

                final PreparedStatement statement = table.database.connection.prepareStatement(
                        "INSERT INTO " + table.tableName + " (" + columnsFormat + ") VALUES " + allValues
                );

                int increment = 1;
                for (Table.Row row : rows) {
                    for (Field field : row.getClass().getFields()) {
                        field.setAccessible(true);
                        statement.setObject(increment++, field.get(row));
                    }
                }

                statement.execute();
                statement.close();
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

}
