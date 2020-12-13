package wtf.listenia.citronDB.api.tables;


import java.util.ArrayList;
import java.util.List;

public class TableBuilder {
    final List<String> basics = new ArrayList<>();
    String primary = "";

    public void addColumns (String... columns) {
        for (String column : columns)
            basics.add(column + " TEXT");
    }

    public void addUnique (String column, int size) {
        basics.add(column + " TEXT");
        basics.add("UNIQUE (" + column + "(" + size + "))");
    }

    public void addPrimary (String column, int size) {
        basics.add(column + " TEXT");
        primary = ", PRIMARY KEY (" + column + "(" + size + "))";
    }

    public String getSubSyntax () {
        return (basics.size() != 0)
                ? String.join(", ", basics) + primary
                : "";
    }

}
