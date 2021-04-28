package fr.i360matt.citronDB.api.builders;

import java.util.HashMap;
import java.util.Map;

public class RowBuilder {

    public final Map<String, Object> datas = new HashMap<>();

    public final RowBuilder define (final String column, final Object value) {
        datas.put(column, value);
        return this;
    }

}
