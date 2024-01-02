package cn.tedu.mall.seckill.timer;

import cn.tedu.mall.seckill.mapper.SeckillSpuMapper;
import cn.tedu.mall.seckill.utils.RedisBloomUtils;
import cn.tedu.mall.seckill.utils.SeckillCacheUtils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class SeckillBloomInitialJob implements Job {
    @Autowired
    private RedisBloomUtils redisBloomUtils;
    @Autowired
    private SeckillSpuMapper seckillSpuMapper;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        // 1.首先确定保存到布隆过滤器的秒杀批次的key值
        // 我们设计的添加两个秒杀批次的布隆过滤器
        // 避免两个批次之间的瞬间空档期,而且也允许用户看到下一批次的商品数据
        // spu:bloom:filter:1111
        String bloomTodayKey = SeckillCacheUtils.getBloomFilterKey(LocalDate.now());
        // spu:bloom:filter:1112
        String bloomTomorrowKey = SeckillCacheUtils.getBloomFilterKey(LocalDate.now().plusDays(1));
        // 2.获取秒杀的spu_id值
        Long[] spuIds = seckillSpuMapper.findAllSeckillSpuIds();
        // 3.将Long[]转换为String[]
        String[] strings = new String[spuIds.length];
        for (int i = 0; i < spuIds.length; i++) {
            strings[i] = spuIds[i] + "";
        }
        // 4.将字符串数组存储到布隆过滤器中
        redisBloomUtils.bfmadd(bloomTodayKey, strings);
        redisBloomUtils.bfmadd(bloomTomorrowKey, strings);
        System.out.println("布隆过滤器加载完毕!");
    }
}
