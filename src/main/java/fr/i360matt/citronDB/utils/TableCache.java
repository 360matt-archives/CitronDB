package fr.i360matt.citronDB.utils;

import java.lang.reflect.Field;

public class TableCache {

    private final String insertSQL;
    public TableCache (final String name, final Field[] fields) {
        final StringBuilder sb_columns = new StringBuilder();
        final StringBuilder sb_interro = new StringBuilder();

        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                sb_columns.append(',');
                sb_interro.append(',');
            }

            sb_columns.append(fields[i].getName());
            sb_interro.append('?');
        }

        this.insertSQL = "INSERT INTO `" + name + "` (" + sb_columns + ") VALUES (" + sb_interro + ')';
    }


    public final String getInsertSQL () {
        return this.insertSQL;
    }

}
