package cn.tedu.mall.seckill.service.impl;

import cn.tedu.mall.pojo.product.vo.SkuStandardVO;
import cn.tedu.mall.pojo.seckill.model.SeckillSku;
import cn.tedu.mall.pojo.seckill.vo.SeckillSkuVO;
import cn.tedu.mall.product.service.seckill.IForSeckillSkuService;
import cn.tedu.mall.seckill.mapper.SeckillSkuMapper;
import cn.tedu.mall.seckill.service.ISeckillSkuService;
import cn.tedu.mall.seckill.utils.SeckillCacheUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class SeckillSkuServiceImpl implements ISeckillSkuService {

    @Autowired
    private SeckillSkuMapper seckillSkuMapper;

    @DubboReference
    private IForSeckillSkuService forSeckillSkuService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public List<SeckillSkuVO> listSeckillSkus(Long spuId) {
        // 1.根据spu_id查询seckill_sku表中的信息
        List<SeckillSku> seckillSkus = seckillSkuMapper.findSeckillSkusBySpuId(spuId);
        // 2.提前准备好封装返回结果的容器
        List<SeckillSkuVO> seckillSkuVOS = new ArrayList<>();
        // 3.遍历查询的seckillSkus,基于此,来查询对应pms_sku表中内容
        for (SeckillSku seckillSku : seckillSkus) {
            // 4.获取sku_id,根据sku_id获取对应的值
            Long skuId = seckillSku.getSkuId();
            // 5.根据sku_id获取对应的缓存的key
            String seckillSkuVOKey = SeckillCacheUtils.getSeckillSkuVOKey(skuId);
            // 6.声明SeckillSkuVO对象,接受信息
            SeckillSkuVO seckillSkuVO = null;
            // 7.根据key判断缓冲中是否存在
            if (redisTemplate.hasKey(seckillSkuVOKey)) {
                // 8.从缓冲直接取出对应key的值
                seckillSkuVO = (SeckillSkuVO) redisTemplate.boundValueOps(seckillSkuVOKey).get();
            } else {
                // 9.缓存中没有,则直接去数据库中查询数据
                SkuStandardVO skuStandardVO = forSeckillSkuService.getById(skuId);
                // 10.将skuStandardVO的内容,封装到SeckillSkuVO中
                seckillSkuVO = new SeckillSkuVO();
                BeanUtils.copyProperties(skuStandardVO, seckillSkuVO);
                // 11.补充其余缺失信息
                seckillSkuVO.setSeckillLimit(seckillSku.getSeckillLimit()); //秒杀的限购
                seckillSkuVO.setSeckillPrice(seckillSku.getSeckillPrice()); //秒杀价格
                seckillSkuVO.setStock(seckillSku.getSeckillStock()); //秒杀的库存
                // 12.将seckillSkuVO添加到缓存中
                redisTemplate.boundValueOps(seckillSkuVOKey).set(
                        seckillSkuVO,
                        24 * 60 * 60 + RandomUtils.nextInt(100),
                        TimeUnit.SECONDS
                );
            }
            // 13.将封装好的对象,添加到集合中
            seckillSkuVOS.add(seckillSkuVO);
        }
        return seckillSkuVOS;
    }
}
