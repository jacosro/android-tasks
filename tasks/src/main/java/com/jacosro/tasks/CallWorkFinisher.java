package com.jacosro.tasks;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.CLASS;

@Inherited
@Retention(CLASS)
@Target({ElementType.METHOD})
public @interface CallWorkFinisher {
}
