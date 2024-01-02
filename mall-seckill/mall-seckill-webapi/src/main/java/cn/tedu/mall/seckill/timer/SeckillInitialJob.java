package cn.tedu.mall.seckill.timer;

import cn.tedu.mall.common.exception.CoolSharkServiceException;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.pojo.seckill.model.SeckillSku;
import cn.tedu.mall.pojo.seckill.model.SeckillSpu;
import cn.tedu.mall.seckill.mapper.SeckillSkuMapper;
import cn.tedu.mall.seckill.mapper.SeckillSpuMapper;
import cn.tedu.mall.seckill.utils.SeckillCacheUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.math.RandomUtils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.parameters.P;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SeckillInitialJob implements Job {
    //由于随机码生成后,不需要改变,所以将随机码以二进制的形式保存到redis中
    @Autowired
    private RedisTemplate redisTemplate;
    //由于库存数会在redis中直接进行加减,所以将库存数以字符串的形式保存到redis中
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private SeckillSpuMapper seckillSpuMapper;
    @Autowired
    private SeckillSkuMapper seckillSkuMapper;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        // 1.五分钟前开始预热,所以我们需要五分钟后开始检查秒杀是否开始
        LocalDateTime time = LocalDateTime.now().plusMinutes(5);
        List<SeckillSpu> spusByTime = seckillSpuMapper.findSeckillSpusByTime(time);
        if (spusByTime == null) {
            throw new CoolSharkServiceException(ResponseCode.BAD_REQUEST, "不在秒杀时间范围,请稍后预热");
        }
        // 2.开始逐个商品进行预热
        // 2.1遍历spusByTime集合,根据其中的每个商品的spuId获取sku的列表,进行预热
        for (SeckillSpu seckillSpu : spusByTime) {
            // 2.1.1获取每个商品的spuId
            Long spuId = seckillSpu.getSpuId();
            // 2.1.2根据spuId,获取sku列表
            List<SeckillSku> seckillSkus = seckillSkuMapper.findSeckillSkusBySpuId(spuId);
            // 2.1.3遍历seckillSkus集合,然后获取每个商品的库存
            for (SeckillSku seckillSku : seckillSkus) {
                // 2.1.3.1获取商品sku的id
                Long skuId = seckillSku.getSkuId();
                log.info("开始将skuId为{}的商品的库存预热到redis中", skuId);
                // 2.1.3.2根据skuId获取对应的redis缓冲中的key 的名称
                //skuId 12345 → getStockKey(skuId) → mall:seckill:sku:stock:12345(是redis的key)
                String stockKey = SeckillCacheUtils.getStockKey(skuId);
                // 2.1.3.3检查redis中是否包含该key,如果包含了,就不需要缓存
                if (redisTemplate.hasKey(stockKey)) {
                    log.info("skuId为{}的商品的库存已预热到redis中", skuId);
                } else {
                    // 2.1.3.4如果不包含,说明需要预热,所以将库存数转换为字符串进行缓存
                    String stock = seckillSku.getSeckillStock() + ""; //库存转换为字符串
                    // 2.1.3.5将stockKey作为key,库存作为value存储到redis中,并且指定生效时间为24小时,为了防止雪崩,可以添加随机秒值
                    stringRedisTemplate.boundValueOps(stockKey).set(
                            stock,
                            24 * 60 * 60 + RandomUtils.nextInt(100),
                            TimeUnit.SECONDS);
                    log.info("成功将skuId为{}的商品的库存预热到redis中", skuId);
                }
            }
            // 2.1.4缓存随机码,获取随机码的key值
            String randCodeKey = SeckillCacheUtils.getRandCodeKey(spuId);
            // 2.1.5判断随机码是否生成
            if (redisTemplate.hasKey(randCodeKey)) {
                // 此处没必要获取,只是为了打印
                int randCode = (int) redisTemplate.boundValueOps(randCodeKey).get();
                log.info("spuId为{}的商品的随机码为{}", spuId, randCode);
            } else {
                // 2.1.6生成随机码,进行缓存
                int randCode = RandomUtils.nextInt(900000) + 100000;
                redisTemplate.boundValueOps(randCodeKey).set(
                        randCode,
                        24 * 60 * 60 + RandomUtils.nextInt(100),
                        TimeUnit.SECONDS
                );
                log.info("成功将spuId为{}的商品产生随机码{},并且预热到redis中", spuId, randCode);
            }
        }
    }
}
