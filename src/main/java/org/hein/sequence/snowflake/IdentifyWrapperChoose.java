package org.hein.sequence.snowflake;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.ArrayList;
import java.util.List;

/**
 * WorkerId & DataCenterId 选择
 */
public class IdentifyWrapperChoose {

    private static final Logger log = LoggerFactory.getLogger(IdentifyWrapperChoose.class);

    private final StringRedisTemplate template;

    public IdentifyWrapperChoose(StringRedisTemplate template) {
        this.template = template;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public IdentifyWrapper chooseIdentify() {
        DefaultRedisScript redisScript = new DefaultRedisScript();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/redis_dataid_workerid.lua")));
        List<Long> resultList = null;
        try {
            redisScript.setResultType(List.class);
            resultList = ((ArrayList) template.execute(redisScript, null));
        } catch (Exception ex) {
            log.error("Redis Lua 脚本获取 WorkId 失败", ex);
        }
        return resultList != null && !resultList.isEmpty() ? new IdentifyWrapper(resultList.get(0), resultList.get(1)) : null;
    }
}
