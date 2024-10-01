package org.hein.sequence;

import java.lang.annotation.*;

/**
 * 主键自增策略
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface AutoId {

    IdType value() default IdType.SNOWFLAKE;

    enum IdType {

        /**
         * 雪花算法
         */
        SNOWFLAKE
    }
}
