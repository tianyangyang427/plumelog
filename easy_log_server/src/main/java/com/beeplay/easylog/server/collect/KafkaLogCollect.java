package com.beeplay.easylog.server.collect;

import com.beeplay.easylog.core.kafka.KafkaConsumerClient;
import com.beeplay.easylog.server.InitConfig;
import com.beeplay.easylog.server.es.ElasticSearchClient;
import com.beeplay.easylog.server.util.DateUtil;
import com.beeplay.easylog.server.util.GfJsonUtil;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.LoggerFactory;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
/**
* @Author Frank.chen
* @Description //TODO
* @Date 14:15 2020/5/12
* @Param 
* @return 
**/
public class KafkaLogCollect {

    private  org.slf4j.Logger logger= LoggerFactory.getLogger(KafkaLogCollect.class);

    private  List<Map<String,Object>> list=new CopyOnWriteArrayList();

    private String kafkaHosts;
    private String esHosts;

    public KafkaLogCollect(String kafkaHosts,String esHosts){
        this.kafkaHosts=kafkaHosts;
        this.esHosts=esHosts;
    }
    public  void kafkaStart(){
        KafkaConsumer<String, String> consumer = KafkaConsumerClient.getInstance(kafkaHosts).getKafkaConsumer();
        consumer.subscribe(Arrays.asList(InitConfig.LOG_KEY));
        logger.info("getting log ready!");
        ElasticSearchClient ec=ElasticSearchClient.getInstance(esHosts);
        logger.info("sending log ready!");
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
            if(records.count()>0) {
                logger.info("get log " + " " + records.count() + " counts!");
                records.forEach(record->{
                    logger.info("get log:" + record.value());
                    Map<String, Object> map = GfJsonUtil.parseObject(record.value(), Map.class);
                    list.add(map);
                    if (list.size() >= InitConfig.MAX_SEND_SIZE) {
                        sendLog(ec);
                    }
                });
                if(list.size()>0) {
                    sendLog(ec);
                }
            }
        }
    }
    private  void sendLog(ElasticSearchClient ec){
        try {
            ec.insertList(list,InitConfig.ES_INDEX+ DateUtil.parseDateToStr(new Date(),DateUtil.DATE_FORMAT_YYYYMMDD),InitConfig.ES_TYPE);
            logger.info("log insert es success! count:"+list.size());
            list.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
