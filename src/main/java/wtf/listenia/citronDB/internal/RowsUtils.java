package wtf.listenia.citronDB.internal;

import wtf.listenia.citronDB.api.tables.Table;

import java.lang.reflect.Field;
import java.util.HashMap;

public class RowsUtils {
    public static HashMap<String, Object> rowToHashMap (Table.Row row) {
        final HashMap<String, Object> res = new HashMap<>();
        try {
            for (Field field : row.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                res.put(field.getName(), field.get(row));
            }
        } catch (Exception exception) { exception.printStackTrace(); }
        return res;
    }

}
