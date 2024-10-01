package org.hein.sequence;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.hein.sequence.snowflake.SnowflakeUtil;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Mybatis 雪花算法插件
 */
@Intercepts({@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})})
public class SnowFlakePlugin implements Interceptor {

    private final Map<Class<?>, List<Handler>> handlerMap = new ConcurrentHashMap<>();

    @Override
    public Object plugin(Object target) {
        // 这里 target 是 Executor、ParameterHandler、ResultSetHandler、StatementHandler 中的一个
        // 判断 target 是否是 Executor 类型，是才进行代理，否则直接返回原始对象
        return target instanceof Executor ? Interceptor.super.plugin(target) : target;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement mappedStatement = (MappedStatement) args[0];
        if ("INSERT".equalsIgnoreCase(mappedStatement.getSqlCommandType().name())) {
            for (Object obj : getEntitySet(args[1])) {
                process(obj);
            }
        }
        return invocation.proceed();
    }

    private Set<Object> getEntitySet(Object object) {
        Set<Object> set = new HashSet<>();
        if (object instanceof Map<?, ?>) {
            ((Collection<?>) ((Map<?, ?>) object).get("list")).forEach(each -> {
                if (each instanceof Collection<?>) {
                    set.addAll((Collection<?>) each);
                } else {
                    set.add(each);
                }
            });
        } else {
            set.add(object);
        }
        return set;
    }

    private void process(Object object) throws Throwable {
        Class<?> handlerKey = object.getClass();
        List<Handler> handlerList = handlerMap.get(handlerKey);
        if (handlerList == null) {
            synchronized (this) {
                handlerList = handlerMap.get(handlerKey);
                if (handlerList != null) {
                    for (Handler handler : handlerList) {
                        handler.accept(object);
                    }
                    return;
                }
                handlerMap.put(handlerKey, handlerList = new ArrayList<>());
                Set<Field> allFields = Arrays.stream(object.getClass().getDeclaredFields())
                        .filter(each -> each.isAnnotationPresent(AutoId.class)).collect(Collectors.toSet());
                for (Field field : allFields) {
                    AutoId annotation = field.getAnnotation(AutoId.class);
                    if (annotation.value() == AutoId.IdType.SNOWFLAKE) {
                        if (field.getType().isAssignableFrom(String.class)) {
                            handlerList.add(new SnowFlakeStringHandler(field));
                        } else if (field.getType().isAssignableFrom(Long.class)) {
                            handlerList.add(new SnowFlakeLongHandler(field));
                        }
                    }
                }
            }
        }
        for (Handler handler : handlerList) {
            handler.accept(object);
        }
    }

    private static abstract class Handler {

        Field field;

        Handler(Field field) {
            this.field = field;
        }

        abstract void handle(Field field, Object object) throws Throwable;

        @SuppressWarnings("deprecation")
        private boolean checkField(Object object, Field field) throws IllegalAccessException {
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            return field.get(object) == null;
        }

        public void accept(Object obj) throws Throwable {
            if (checkField(obj, field)) {
                handle(field, obj);
            }
        }
    }

    private static class SnowFlakeLongHandler extends Handler {

        SnowFlakeLongHandler(Field field) {
            super(field);
        }

        @Override
        void handle(Field field, Object object) throws Throwable {
            field.set(object, SnowflakeUtil.nextId());
        }
    }

    private static class SnowFlakeStringHandler extends Handler {

        SnowFlakeStringHandler(Field field) {
            super(field);
        }

        @Override
        void handle(Field field, Object object) throws Throwable {
            field.set(object, SnowflakeUtil.nextIdStr());
        }
    }

    private static class SegmentLongHandler extends Handler {

        SegmentLongHandler(Field field) {
            super(field);
        }

        @Override
        void handle(Field field, Object object) throws Throwable {
            field.set(object, SnowflakeUtil.nextId());
        }
    }

    private static class SegmentStringHandler extends Handler {

        SegmentStringHandler(Field field) {
            super(field);
        }

        @Override
        void handle(Field field, Object object) throws Throwable {
            field.set(object, SnowflakeUtil.nextId());
        }
    }
}
