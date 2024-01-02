package cn.tedu.mall.seckill.service.impl;

import cn.tedu.mall.common.exception.CoolSharkServiceException;
import cn.tedu.mall.common.pojo.domain.CsmallAuthenticationInfo;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.order.service.IOmsOrderService;
import cn.tedu.mall.pojo.order.dto.OrderAddDTO;
import cn.tedu.mall.pojo.order.dto.OrderItemAddDTO;
import cn.tedu.mall.pojo.order.vo.OrderAddVO;
import cn.tedu.mall.pojo.seckill.dto.SeckillOrderAddDTO;
import cn.tedu.mall.pojo.seckill.model.Success;
import cn.tedu.mall.pojo.seckill.vo.SeckillCommitVO;
import cn.tedu.mall.seckill.config.RabbitMqComponentConfiguration;
import cn.tedu.mall.seckill.service.ISeckillService;
import cn.tedu.mall.seckill.utils.SeckillCacheUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class SeckillServiceImpl implements ISeckillService {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @DubboReference
    private IOmsOrderService omsOrderService;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public SeckillCommitVO commitSeckill(SeckillOrderAddDTO seckillOrderAddDTO) {
        // 第一部分:利用redis去检查是否重复购买,并且库存是否充足
        // 1.获取用户id
        Long userId = getUserId();
        // 2.获取商品id
        Long skuId = seckillOrderAddDTO.getSeckillOrderItemAddDTO().getSkuId();
        // 3.判断user_id的用户是否重复购买了sku_id的商品
        // 3.1将userId和skuId组合成一个key值,存储到redis中,值是购买次数
        String reseckillCheckKey = SeckillCacheUtils.getReseckillCheckKey(skuId, userId);
        // 3.2为reseckillCheckKey设置值 increment()在原有值基础上+1
        Long increment = stringRedisTemplate.boundValueOps(reseckillCheckKey).increment();
        // 3.3判断increment的值,是否大于1,大于1说明重复购买
        if (increment > 1) {
            throw new CoolSharkServiceException(ResponseCode.FORBIDDEN, "您已经购买过该商品了!");
        }
        // 4.程序执行到这里,说明是第一次购买商品,开始判断库存是否充足
        // 4.1根据skuId获取key值,在秒杀预热时,库存和随机码已经都缓存进入了
        String stockKey = SeckillCacheUtils.getStockKey(skuId);
        // 4.2默认执行一次会-1
        Long decrement = stringRedisTemplate.boundValueOps(stockKey).decrement();
        // 4.3如果库存数不小于0,说明库存充足
        if (decrement < 0) {
            // 4.4删除购买记录
            stringRedisTemplate.boundValueOps(reseckillCheckKey).decrement();
            // 4.5抛出异常
            throw new CoolSharkServiceException(ResponseCode.BAD_REQUEST, "对不起,您购买的商品已售罄");
        }
        // 第二部分:将秒杀订单转换为普通订单
        // 1.转换为普通订单
        OrderAddDTO orderAddDTO = converSeckillOrderToOrder(seckillOrderAddDTO);
        // 2.补充用户id
        orderAddDTO.setUserId(userId);
        // 3.将订单信息存储到订单表中
        OrderAddVO orderAddVO = omsOrderService.addOrder(orderAddDTO);

        //第三部分:使用訊息佇列保存秒殺活動商品成功的資料紀錄(RabbitMQ)
        //1.創建秒殺成功物件
        Success success = new Success();
        //2.針對秒殺成功物件賦予數值
        success.setUserId(userId);
        success.setOrderSn(orderAddVO.getSn());
        //3.將成功資料內容發送到RabbitMQ當中
        rabbitTemplate.convertAndSend(
                RabbitMqComponentConfiguration.SECKILL_EX,
                RabbitMqComponentConfiguration.SECKILL_RK,
                success);
        //4.聲明當前方法函數返回物件內容
        SeckillCommitVO seckillCommitVO = new SeckillCommitVO();
        BeanUtils.copyProperties(orderAddDTO,seckillCommitVO);

        return seckillCommitVO;
    }

    private OrderAddDTO converSeckillOrderToOrder(SeckillOrderAddDTO seckillOrderAddDTO) {
        OrderAddDTO orderAddDTO = new OrderAddDTO();
        //同名属性赋值
        BeanUtils.copyProperties(seckillOrderAddDTO, orderAddDTO);
        ArrayList<OrderItemAddDTO> orderItemAddDTOS = new ArrayList<>();
        OrderItemAddDTO orderItemAddDTO = new OrderItemAddDTO();
        BeanUtils.copyProperties(seckillOrderAddDTO.getSeckillOrderItemAddDTO(), orderItemAddDTO);
        orderItemAddDTOS.add(orderItemAddDTO);
        orderAddDTO.setOrderItems(orderItemAddDTOS);
        return orderAddDTO;
    }

    public CsmallAuthenticationInfo getUserInfo() {
        UsernamePasswordAuthenticationToken token =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        if (token == null)
            throw new CoolSharkServiceException(ResponseCode.UNAUTHORIZED, "没有登录信息");
        CsmallAuthenticationInfo userInfo = (CsmallAuthenticationInfo) token.getCredentials();
        return userInfo;
    }

    public Long getUserId() {
        return getUserInfo().getId();
    }
}
