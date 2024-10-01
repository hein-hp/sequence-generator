package org.hein.sequence.snowflake;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 雪花算法初始化器
 */
@Component
public class SnowflakeInitializer implements InitializingBean {

    private final StringRedisTemplate template;

    public SnowflakeInitializer(StringRedisTemplate template) {
        this.template = template;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        IdentifyWrapperChoose chooser = new IdentifyWrapperChoose(template);
        IdentifyWrapper wrapper = chooser.chooseIdentify();
        Snowflake snowflake;
        if (wrapper == null) {
            snowflake = new Snowflake();
        } else {
            snowflake = new Snowflake(wrapper.getWorkerId(), wrapper.getDataCenterId());
        }
        SnowflakeUtil.init(snowflake);
    }
}
