package com.oceanverse.common.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperateLog {

    String module();

    OperateType type() default OperateType.OTHER;

    enum OperateType {
        CREATE, UPDATE, DELETE, OTHER
    }
}
