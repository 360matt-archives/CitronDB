package wtf.listenia.citronDB.utils;

import wtf.listenia.citronDB.api.annotations.Primary;
import wtf.listenia.citronDB.api.annotations.Size;
import wtf.listenia.citronDB.api.annotations.Unique;

import java.lang.reflect.Field;

public class ColumnType {

    public static String getFormat (final Field field) {
        return getFormat(field, true);
    }

    public static String getFormat (final Field field, final Boolean withSpecial) {

        int size = 0;
        String special = null;

        final Unique annotUnique = field.getAnnotation(Unique.class);
        if (annotUnique != null) {
            size = annotUnique.size();
            special = "UNIQUE";
        } else {
            final Primary annotPrimary = field.getAnnotation(Primary.class);
            if (annotPrimary != null) {
                size = annotPrimary.size();
                special = "PRIMARY";
            } else {
                final Size annotSize = field.getAnnotation(Size.class);
                if (annotSize != null) {
                    size = annotSize.size();
                }
            }
        }

        final Size annotSize;
        if ((annotSize = field.getAnnotation(Size.class)) != null)
            size = annotSize.size();

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
