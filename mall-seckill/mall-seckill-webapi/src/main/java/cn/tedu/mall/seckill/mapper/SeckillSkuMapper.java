package cn.tedu.mall.seckill.mapper;

import cn.tedu.mall.pojo.seckill.model.SeckillSku;
import cn.tedu.mall.pojo.seckill.model.SeckillSpu;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SeckillSkuMapper {
    // 1.根据spuId查询秒杀sku列表
    List<SeckillSku> findSeckillSkusBySpuId(Long spuId);

}
