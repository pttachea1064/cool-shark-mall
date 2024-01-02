package cn.tedu.mall.seckill.mapper;

import cn.tedu.mall.pojo.seckill.model.SeckillSpu;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SeckillSpuMapper {
    // 1.查询秒杀商品
    List<SeckillSpu> findSeckillSpus();

    // 2.根据当前时间查询秒杀的商品
    List<SeckillSpu> findSeckillSpusByTime(LocalDateTime time);

    // 3.查询所有秒杀商品的spu的id值(多个),为布隆过滤器做准备,防止redis穿透
    Long[] findAllSeckillSpuIds();

    // 4.根据spu_id查询spu信息
    SeckillSpu findSeckillSpuBySpuId(Long spuId);
}
