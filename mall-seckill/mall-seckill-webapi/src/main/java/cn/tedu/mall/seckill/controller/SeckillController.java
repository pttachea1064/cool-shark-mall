package cn.tedu.mall.seckill.controller;

import cn.tedu.mall.common.exception.CoolSharkServiceException;
import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.pojo.seckill.dto.SeckillOrderAddDTO;
import cn.tedu.mall.pojo.seckill.vo.SeckillCommitVO;
import cn.tedu.mall.seckill.service.ISeckillService;
import cn.tedu.mall.seckill.utils.SeckillCacheUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/seckill")
@Api(tags = "提交活動商品訂單")
public class SeckillController {

    @Autowired
    private ISeckillService seckillService;
    @Autowired
    private RedisTemplate redisTemplate;

    @PostMapping("/{randCode}")
    @ApiOperation("随机码验证并提交订单")
    @ApiImplicitParam(value = "随机码", name = "randCode", dataType = "string")
    @PreAuthorize("hasRole('user')")
    public JsonResult<SeckillCommitVO> commitSeckillOrder(@PathVariable String randCode, SeckillOrderAddDTO seckillOrderAddDTO){
        //1.獲取訂單商品的spu_id
        Long spuId = seckillOrderAddDTO.getSpuId();
        //2.基於spu_id生成對應的隨機數值之key內容
        String randCodeKey = SeckillCacheUtils.getRandCodeKey(spuId);
        //判斷redis當中是否包含key
        if (redisTemplate.hasKey(randCodeKey)){
            //4.如果包含key則檢驗value(隨機數值)是否相同 所以收先獲取隨機數值
            String redisRandCode =redisTemplate.boundValueOps(randCodeKey).get()+"" ;
            // 5.判断redis中的随机码是否丢失
            if (redisRandCode == null) {
                throw new CoolSharkServiceException(ResponseCode.INTERNAL_SERVER_ERROR, "服务器内部问题");
            }
            // 6.判断redis中的随机码和前端传入的随机码是否一致
            if (!redisRandCode.equals(randCode)) {
                throw new CoolSharkServiceException(ResponseCode.NOT_FOUND, "没有指定商品");
            }
            // 7.执行购买操作
            SeckillCommitVO seckillCommitVO = seckillService.commitSeckill(seckillOrderAddDTO);
            return JsonResult.ok(seckillCommitVO);
        } else {
            // 8.如果不包含key,直接抛出异常
            throw new CoolSharkServiceException(ResponseCode.NOT_FOUND, "没有指定商品");
        }

    }

}
