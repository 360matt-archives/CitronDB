package fr.i360matt.citronDB.api.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Size {
    int size () default 1024;
}
