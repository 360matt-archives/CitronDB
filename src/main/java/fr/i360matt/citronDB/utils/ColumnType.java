package fr.i360matt.citronDB.utils;

import fr.i360matt.citronDB.api.annotations.Primary;
import fr.i360matt.citronDB.api.annotations.Size;
import fr.i360matt.citronDB.api.annotations.Unique;

import java.lang.reflect.Field;

public class ColumnType {

    public static String getFormat (final Field field) {
        return getFormat(field, true);
    }

    public static String getFormat (final Field field, final Boolean withSpecial) {
        final String special;
        if (field.getAnnotation(Unique.class) != null)
            special = "UNIQUE";
         else if (field.getAnnotation(Primary.class) != null)
            special = "PRIMARY";
         else
            special = null;

        int size = 0;
        Size annotSize;
        if ((annotSize = field.getAnnotation(Size.class)) != null) {
            size = annotSize.size();
        }

        final StringBuilder res = new StringBuilder();

        final Class<?> type = field.getType();
        if (type == int.class) {
            if (size > 255) size = 255;
            res.append("INT");
        } else if (type == long.class) {
            if (size > 255) size = 255;
            res.append("BIGINT");
        } else if (type == float.class) {
            if (size > 255) size = 255;
            res.append("FLOAT");
        } else if (type == double.class) {
            if (size > 255) size = 255;
            res.append("DOUBLE");
        } else if (type == boolean.class) {
            if (size > 0) size = 0;
            res.append("BOOLEAN");
        } else if (type == String.class) {
            res.append( (size > 1) ? "VARCHAR" : "TEXT" );
        } else {
            res.append("BLOB");
        }


        if (size > 0)
            res.append("(").append(size).append(")");
        if (withSpecial && special != null)
            res.append(" ").append(special);

        return field.getName() + " " + res;
    }
}
