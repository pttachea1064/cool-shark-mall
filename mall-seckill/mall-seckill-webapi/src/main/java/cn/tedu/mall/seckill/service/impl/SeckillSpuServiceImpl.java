package cn.tedu.mall.seckill.service.impl;

import cn.tedu.mall.common.exception.CoolSharkServiceException;
import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.pojo.product.vo.SpuDetailStandardVO;
import cn.tedu.mall.seckill.mapper.SeckillSpuMapper;
import cn.tedu.mall.pojo.product.vo.SpuStandardVO;
import cn.tedu.mall.pojo.seckill.model.SeckillSpu;
import cn.tedu.mall.pojo.seckill.vo.SeckillSpuDetailSimpleVO;
import cn.tedu.mall.pojo.seckill.vo.SeckillSpuVO;
import cn.tedu.mall.product.service.seckill.IForSeckillSpuService;
import cn.tedu.mall.seckill.service.ISeckillSpuService;
import cn.tedu.mall.seckill.utils.RedisBloomUtils;
import cn.tedu.mall.seckill.utils.SeckillCacheUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class SeckillSpuServiceImpl implements ISeckillSpuService {
    // 查询seckill_spu表
    @Autowired
    private SeckillSpuMapper seckillSpuMapper;
    // 查询关联的pms_spu表内容
    @DubboReference
    private IForSeckillSpuService forSeckillSpuService;
    @Autowired
    private RedisTemplate redisTemplate;
    public static final String SECKILL_SPU_DETAIL_VO_PREFEX = "seckill:spu:detail:vo:";
    @Autowired
    private RedisBloomUtils redisBloomUtils;

    @Override
    public JsonPage<SeckillSpuVO> listSeckillSpus(Integer page, Integer pageSize) {
        // 1.设置分页信息
        PageHelper.startPage(page, pageSize);
        // 2.执行查询的方法
        List<SeckillSpu> seckillSpus = seckillSpuMapper.findSeckillSpus();
        // 3.准备一个集合,用于装载seckill_spu的内容以及与之关联的pms_spu的内容
        ArrayList<SeckillSpuVO> seckillSpuVOS = new ArrayList<>();
        // 4.遍历seckillSpus对象,将其中的属性装进SeckillSpuVO实例中
        for (SeckillSpu seckillSpu : seckillSpus) {
            // 4.1将seckill_spu的内容封装到SeckillSpuVO中
            SeckillSpuVO seckillSpuVO = new SeckillSpuVO();
            seckillSpuVO.setSeckillListPrice(seckillSpu.getListPrice()); //秒杀价格
            seckillSpuVO.setStartTime(seckillSpu.getStartTime()); //秒杀开始时间
            seckillSpuVO.setEndTime(seckillSpu.getEndTime()); //秒杀结束时间
            // 4.2将关联的pms_spu的内容封装到SeckillSpuVO中
            // 4.2.1获取当前遍历的秒杀对象的spu_id值
            Long spuId = seckillSpu.getSpuId();
            // 4.2.2再根据spu_id的值,去查询pms_spu表的对应的内容
            SpuStandardVO spuStandardVO = forSeckillSpuService.getSpuById(spuId);
            // 4.2.3将spuStandardVO中的属性值复制到seckillSpuVO
            BeanUtils.copyProperties(spuStandardVO, seckillSpuVO);
            seckillSpuVOS.add(seckillSpuVO);
        }
        return JsonPage.restPage(new PageInfo<>(seckillSpuVOS));
    }

    @Override
    public SeckillSpuVO getSeckillSpu(Long spuId) {
        // 1.先获取布隆过滤器的key值
        String bloomTodayKey = SeckillCacheUtils.getBloomFilterKey(LocalDate.now());
        log.info("当前批次的布隆过滤器的key为{}", bloomTodayKey);
        // 2.判断布隆过滤器中是否存在当前批次
        if (!redisBloomUtils.bfexists(bloomTodayKey, spuId + "")) {
            throw new CoolSharkServiceException(ResponseCode.NOT_FOUND, "您访问的秒杀产品不存在!");
        }
        // 3.如果存在,继续执行
        SeckillSpuVO seckillSpuVO = null;
        // 获取key mall:seckill:spu:vo:1
        String seckillSpuVOKey = SeckillCacheUtils.getSeckillSpuVOKey(spuId);
        if (redisTemplate.hasKey(seckillSpuVOKey)) {
            seckillSpuVO = (SeckillSpuVO) redisTemplate.boundValueOps(seckillSpuVOKey).get();
        } else {
            // 4.缓存中没有,则查询数据库 seckill_spu
            SeckillSpu seckillSpu = seckillSpuMapper.findSeckillSpuBySpuId(spuId);
            // 5.判断seckillSpu是否为空,布隆过滤波器有1%的错误率,可能会误判商品信息存在
            if (seckillSpu == null) {
                throw new CoolSharkServiceException(ResponseCode.NOT_FOUND, "您访问的商品不存在");
            }
            // 6.查询查询具体的商品信息 pms_spu
            SpuStandardVO spuStandardVO = forSeckillSpuService.getSpuById(spuId);
            // 7.将SpuStandardVO转换为SeckillSpuVO
            seckillSpuVO = new SeckillSpuVO();
            BeanUtils.copyProperties(spuStandardVO, seckillSpuVO);
            // 8.将查询的seckillSpu中的内容也赋值到seckillSpuVO
            seckillSpuVO.setSeckillListPrice(seckillSpu.getListPrice()); //秒杀价格
            seckillSpuVO.setStartTime(seckillSpu.getStartTime()); //秒杀开始时间
            seckillSpuVO.setEndTime(seckillSpu.getEndTime()); //秒杀结束时间
            // 9.将seckillSpuVO缓存到redis中
            redisTemplate.boundValueOps(seckillSpuVOKey).set(
                    seckillSpuVO,
                    24 * 60 * 60 + RandomUtils.nextInt(100),
                    TimeUnit.SECONDS
            );
        }
        // 10.判断当前时间是否在秒杀时间范围
        LocalDateTime startTime = seckillSpuVO.getStartTime();
        LocalDateTime endTime = seckillSpuVO.getEndTime();
        LocalDateTime nowTime = LocalDateTime.now();
        // 11.nowTime小于endTime,并且大于startTime startTime < nowTime < endTime
        // Duration是一个时间对象,其中的between方法,是用于计算指定的时间的差值
        // between(参数1,参数2) → 参数2 - 参数1
        // 时间差为负数(参数2 - 参数1 < 0),返回的信息是negative
        // startTime < nowTime → startTime - nowTime < 0
        // nowTime < endTime → nowTime - endTime < 0
        Duration time1 = Duration.between(nowTime, startTime);
        Duration time2 = Duration.between(endTime, nowTime);
        //12.判斷time1 和time2是否為negative 如果是 則說明當前時間在秒殺時間區間內
        if (time1.isNegative()&& time2.isNegative())   {
            //13.程式碼運行到這 說明可以進行購買的操作 後端需要將隨機數值傳送給前端
            String randCodeKey = SeckillCacheUtils.getRandCodeKey(spuId);
            //13.2 基於隨機數值的key 去從redis當中獲取隨依數值
            int randCode = (int)  redisTemplate.boundValueOps(randCodeKey).get();
            //14.將隨機數值拼接到url並且返回給前端 ex. /seckill/378278787872828282
            seckillSpuVO.setUrl("/seckill/"+randCode);
        }
        //15.返回 seckillSpuVo, 該物件包含了商品的向細資料與隨機數值的URL

        return seckillSpuVO;
    }

    @Override
    public SeckillSpuDetailSimpleVO getSeckillSpuDetail(Long spuId) {
        // 1.获取key
        String seckillDetailKey = SECKILL_SPU_DETAIL_VO_PREFEX + spuId;
        // 2.判断缓存中是否包商品详细信息
        SeckillSpuDetailSimpleVO seckillSpuDetailSimpleVO = null;
        if (redisTemplate.hasKey(seckillDetailKey)) {
            seckillSpuDetailSimpleVO =
                    (SeckillSpuDetailSimpleVO) redisTemplate.boundValueOps(seckillDetailKey).get();
        } else {
            // 3.缓存中不存在商品详细数据,所以查询数据库
            SpuDetailStandardVO spuDetailById = forSeckillSpuService.getSpuDetailById(spuId);
            seckillSpuDetailSimpleVO = new SeckillSpuDetailSimpleVO();
            BeanUtils.copyProperties(spuDetailById, seckillSpuDetailSimpleVO);
            // 4.将查询到的内容,添加到缓存中
            redisTemplate.boundValueOps(seckillDetailKey).set(
                    seckillSpuDetailSimpleVO,
                    24 * 60 * 60 + RandomUtils.nextInt(100),
                    TimeUnit.SECONDS
            );
        }
        return seckillSpuDetailSimpleVO;
    }
}
