package wtf.listenia.citronDB.api.rows;


import java.util.HashMap;

public class RowBuilder {
    public final HashMap<String, Object> content = new HashMap<>();

    public void set (String column, Object value) {
        content.put(column, value);
    }
    public Object get (String column) { return content.get(column); }

}
