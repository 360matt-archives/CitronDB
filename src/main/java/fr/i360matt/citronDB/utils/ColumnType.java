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
        int size;
        final String special;

        Unique annotUnique;
        Primary annotPrimary;
        Size annotSize;

        if ((annotUnique = field.getAnnotation(Unique.class)) != null) {
            size = annotUnique.size();
            annotUnique = null; // GC
            special = "UNIQUE";
        } else if ((annotPrimary = field.getAnnotation(Primary.class)) != null) {
            size = annotPrimary.size();
            annotPrimary = null; // GC
            special = "PRIMARY";
        } else if ((annotSize = field.getAnnotation(Size.class)) != null) {
            size = annotSize.size();
            annotSize = null; // GC
            special = null;
        } else {
            size = 0;
            special = null;
        }

        final StringBuilder res = new StringBuilder();

        final Class<?> type = field.getType();
        if (Integer.TYPE.isAssignableFrom(type)) {
            if (size > 255) size = 255;
            res.append("INT");
        } else if (Long.TYPE.isAssignableFrom(type)) {
            if (size > 255) size = 255;
            res.append("BIGINT");
        } else if (Float.TYPE.isAssignableFrom(type)) {
            if (size > 255) size = 255;
            res.append("FLOAT");
        } else if (Double.TYPE.isAssignableFrom(type)) {
            if (size > 255) size = 255;
            res.append("DOUBLE");
        } else if (Boolean.TYPE.isAssignableFrom(type)) {
            if (size > 0) size = 0;
            res.append("BOOLEAN");
        } else
            res.append( (size > 1) ? "VARCHAR" : "TEXT" );

        if (size > 0)
            res.append("(").append(size).append(")");
        if (withSpecial && special != null)
            res.append(" ").append(special);

        return field.getName() + " " + res.toString();
    }
}
