package ljl.bilibili.notice.consumer.mysql_es;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static ljl.bilibili.notice.constant.Constant.*;
/**
 *mysql与es数据同步消费者
 */
@Service
@RocketMQMessageListener(
        topic = "mysqlToEs",
        consumerGroup = "mysql-es-group",
        consumeMode = ConsumeMode.CONCURRENTLY
)
public class MysqlToEsConsumer implements RocketMQListener<MessageExt> {
    @Resource
    ObjectMapper objectMapper;
    @Resource
    RedisTemplate objectRedisTemplate;
    @Override
    public void onMessage(MessageExt messageExt){
        String json = new String(messageExt.getBody(), StandardCharsets.UTF_8);
        HashMap<String,Object> map ;
        try {
            map = objectMapper.readValue(json, HashMap.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        if(map.get(OPERATION_TYPE).equals(OPERATION_TYPE_ADD)){
            map.remove(OPERATION_TYPE);
            if(map.get(TABLE_NAME).equals(VIDEO_TABLE_NAME)){
                map.remove(TABLE_NAME);
                objectRedisTemplate.opsForList().rightPush(VIDEO_ADD_KEY,map);
            }else {
                map.remove(TABLE_NAME);
                objectRedisTemplate.opsForList().rightPush(USER_ADD_KEY,map);
            }

        }

        else if(map.get(OPERATION_TYPE).equals(OPERATION_TYPE_DELETE)){
            map.remove(OPERATION_TYPE);
                map.remove(TABLE_NAME);
                objectRedisTemplate.opsForList().rightPush(VIDEO_DELETE_KEY,map);

        }

        else{
            map.remove(OPERATION_TYPE);
            if(map.get(TABLE_NAME).equals(VIDEO_TABLE_NAME)){
                map.remove(OPERATION_TYPE);
                objectRedisTemplate.opsForList().rightPush(VIDEO_UPDATE_KEY,map);
            }
            else{
                map.remove(TABLE_NAME);
                objectRedisTemplate.opsForList().rightPush(USER_UPDATE_KEY,map);
            }
        }

    }
}
