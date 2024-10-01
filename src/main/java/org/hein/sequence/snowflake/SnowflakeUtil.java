package org.hein.sequence.snowflake;

/**
 * 雪花算法工具类
 */
public final class SnowflakeUtil {

    /**
     * 雪花算法
     */
    private static Snowflake SNOWFLAKE;

    public static void init(Snowflake snowflake) {
        SNOWFLAKE = snowflake;
    }

    public static Long nextId() {
        return SNOWFLAKE.nextId();
    }

    public static String nextIdStr() {
        return Long.toString(SNOWFLAKE.nextId());
    }
}