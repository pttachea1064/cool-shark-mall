package cn.tedu.mall.order.service.impl;

import cn.tedu.mall.common.exception.CoolSharkServiceException;
import cn.tedu.mall.common.pojo.domain.CsmallAuthenticationInfo;
import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.order.mapper.OmsOrderItemMapper;
import cn.tedu.mall.order.mapper.OmsOrderMapper;
import cn.tedu.mall.order.service.IOmsCartService;
import cn.tedu.mall.order.service.IOmsOrderItemService;
import cn.tedu.mall.order.service.IOmsOrderService;
import cn.tedu.mall.order.utils.IdGeneratorUtils;
import cn.tedu.mall.pojo.order.dto.OrderAddDTO;
import cn.tedu.mall.pojo.order.dto.OrderItemAddDTO;
import cn.tedu.mall.pojo.order.dto.OrderListTimeDTO;
import cn.tedu.mall.pojo.order.dto.OrderStateUpdateDTO;
import cn.tedu.mall.pojo.order.model.OmsCart;
import cn.tedu.mall.pojo.order.model.OmsOrder;
import cn.tedu.mall.pojo.order.model.OmsOrderItem;
import cn.tedu.mall.pojo.order.vo.OrderAddVO;
import cn.tedu.mall.pojo.order.vo.OrderDetailVO;
import cn.tedu.mall.pojo.order.vo.OrderListVO;
import cn.tedu.mall.product.service.order.IForOrderSkuService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@DubboService //秒杀模块中会调用
@Service
@Slf4j
public class OmsOrderServiceImpl implements IOmsOrderService {
    //远程调用减少库存的方法
    @DubboReference
    private IForOrderSkuService forOrderSkuService;
    //删除购物车中商品
    @Autowired
    private IOmsCartService omsCartService;
    //新增订单
    @Autowired
    private OmsOrderMapper omsOrderMapper;
    //新增订单项(订单中的商品)
    @Autowired
    private OmsOrderItemMapper omsOrderItemMapper;

    /**
     * 新增订单
     *
     * @param orderAddDTO 订单信息(包含订单内容和订单项内容)
     * @return 订单编号
     */
    @Override
    public OrderAddVO addOrder(OrderAddDTO orderAddDTO) {
        // 1.收集订单信息
        // 1.1将提交的订单信息(包含订单项内容)分离,先处理订单信息
        OmsOrder omsOrder = new OmsOrder();
        BeanUtils.copyProperties(orderAddDTO, omsOrder);
        // 1.2将订单信息进行计算操作,这部分内容比较多,所以封装为一个方法
        loadOrder(omsOrder);
        // 2.收集订单项信息
        // 2.1将提交的订单项信息分离,处理订单项信息
        List<OrderItemAddDTO> orderItems = orderAddDTO.getOrderItems();
        // 2.2判断订单项是否为null,为null直接抛出异常
        if (orderItems == null) {
            throw new CoolSharkServiceException(ResponseCode.BAD_REQUEST, "订单中必须要包含订单项");
        }
        // 2.3准备List<OmsOrderItem>对象
        List<OmsOrderItem> omsOrderItems = new ArrayList<>();
        // 2.4遍历订单项集合,将订单项内容封装到omsOrderItems中
        for (OrderItemAddDTO orderItem : orderItems) {
            // 2.4.1将遍历的OrderItemAddDTO转换为OmsOrderItem
            OmsOrderItem omsOrderItem = new OmsOrderItem();
            BeanUtils.copyProperties(orderItem, omsOrderItem);
            // 2.4.2针对OmsOrderItem中不能为null值的属性进行补充
            // 2.4.2.1判断id是否为null,如果为null,则使用Leaf填写分布式ID
            if (omsOrderItem.getId() == null) {
                Long id = IdGeneratorUtils.getDistributeId("order_item");
                omsOrderItem.setId(id);
            }
            // 2.4.2.2判断skuId是否为null,如果为null,则抛出异常
            if (omsOrderItem.getSkuId() == null) {
                throw new CoolSharkServiceException(ResponseCode.BAD_REQUEST, "订单商品中必须要包含skuId");
            }
            // 2.4.3将omsOrder的id赋值给omsOrderItem的order_id字段
            omsOrderItem.setOrderId(omsOrder.getId());
            // 2.4.4将omsOrderItem保存到对应的集合中
            omsOrderItems.add(omsOrderItem);
            // 3.数据库的操作
            // 3.1减少商品库存
            Long skuId = omsOrderItem.getSkuId();
            int rows = forOrderSkuService.reduceStockNum(skuId, omsOrderItem.getQuantity());
            if (rows == 0) {
                log.info("商品{},库存不足", skuId);
                throw new CoolSharkServiceException(ResponseCode.BAD_REQUEST, "库存不足");
            }
            // 3.2删除购物车信息
            OmsCart omsCart = new OmsCart();
            omsCart.setUserId(omsOrder.getUserId());
            omsCart.setSkuId(skuId);
            omsCartService.removeUserCarts(omsCart);
        }
        // 3.3新增订单
        omsOrderMapper.insertOrder(omsOrder);
        // 3.4新增订单项
        omsOrderItemMapper.insertOrderItem(omsOrderItems);
        // 4.返回订单信息
        OrderAddVO orderAddVO = new OrderAddVO();
        // 4.1订单ID
        orderAddVO.setId(omsOrder.getId());
        // 4.2订单编号
        orderAddVO.setSn(omsOrder.getSn());
        // 4.3订单生成时间
        orderAddVO.setCreateTime(omsOrder.getGmtCreate());
        // 4.4实际支付金额
        orderAddVO.setPayAmount(omsOrder.getAmountOfActualPay());
        // 4.5将封装结果返回
        return orderAddVO;
    }

    /**
     * 处理订单信息
     *
     * @param omsOrder
     */
    private void loadOrder(OmsOrder omsOrder) {
        // 1.针对订单信息中必须要具备的但是为null的值进行赋值(客户没办法生成的数据,我们后台来生成)
        // 1.1判断id是否为null,为null就利用Leaf生成分布式的ID值
        if (omsOrder.getId() == null) {
            Long id = IdGeneratorUtils.getDistributeId("order");
            omsOrder.setId(id);
        }
        // 1.2判断订单编号是否为null,为null就生成随机的字符串作为订单编号
        if (omsOrder.getSn() == null) {
            String sn = UUID.randomUUID().toString();
            omsOrder.setSn(sn);
        }
        // 1.3判断用户id是否为null,如果为null,自己获取登录的用户id
        if (omsOrder.getUserId() == null) {
            omsOrder.setUserId(getUserId());
        }
        // 1.4判断订单状态是否为null,此处为初始化,设置为0,表示未支付的订单状态
        if (omsOrder.getState() == null) {
            omsOrder.setState(0);
        }
        // 1.5为了保证gmt_order(下单时间)、gmt_create(数据生成时间)和gmt_modified(数据最后修改时间)保持一致,所以判断
        if (omsOrder.getGmtOrder() == null || omsOrder.getGmtCreate() == null || omsOrder.getGmtModified() == null) {
            LocalDateTime now = LocalDateTime.now();
            omsOrder.setGmtOrder(now);
            omsOrder.setGmtCreate(now);
            omsOrder.setGmtModified(now);
        }
        // 1.6计算支付金额
        // 1.6.1在计算之前,先保证原价不能为null,如果为null,直接抛出异常
        if (omsOrder.getAmountOfOriginalPrice() == null) {
            throw new CoolSharkServiceException(ResponseCode.BAD_REQUEST, "订单必须要有原价!");
        }
        // 1.6.2判断运费是否为null,要赋值为0,方便计算
        if (omsOrder.getAmountOfFreight() == null) {
            //运费为了能精准进行浮点数运算,所以使用了BigDecimal
            omsOrder.setAmountOfFreight(new BigDecimal(0));
        }
        // 1.6.3判断优惠是否为null,要赋值为0,方便计算
        if (omsOrder.getAmountOfDiscount() == null) {
            omsOrder.setAmountOfDiscount(new BigDecimal(0));
        }
        // 1.6.4计算实际支付金额 (实际支付金额 = 原价+运费-优惠)
        BigDecimal originalPrice = omsOrder.getAmountOfOriginalPrice(); // 原价
        BigDecimal freight = omsOrder.getAmountOfFreight(); //运费
        BigDecimal discount = omsOrder.getAmountOfDiscount(); //优惠
        // BigDecimal中add()是加法,subtract()是减法,multiply()是乘法,divide()是除法
        BigDecimal actualPrice = originalPrice.add(freight).subtract(discount);
        omsOrder.setAmountOfActualPay(actualPrice);
    }

    /**
     * 更新订单状态
     *
     * @param orderStateUpdateDTO
     */
    @Override
    public void updateOrderState(OrderStateUpdateDTO orderStateUpdateDTO) {
        OmsOrder omsOrder = new OmsOrder();
        BeanUtils.copyProperties(orderStateUpdateDTO, omsOrder);
        omsOrderMapper.updateOrderById(omsOrder);
    }

    /**
     * 根据起始结束时间查询订单列表
     *
     * @param orderListTimeDTO
     */
    @Override
    public JsonPage<OrderListVO> listOrdersBetweenTimes(OrderListTimeDTO orderListTimeDTO) {
        // 1.取出时间
        LocalDateTime startTime = orderListTimeDTO.getStartTime();
        LocalDateTime endTime = orderListTimeDTO.getEndTime();
        // 2.如果不输入时间,那么则取出最近一个月的订单
        if (startTime == null || endTime == null) {
            // 2.1起始时间就是当前时间减少一个月
            startTime = LocalDateTime.now().minusMonths(1);
            // 2.2结束时间就是当前时间
            endTime = LocalDateTime.now();
            // 2.3将新指定的时间值,赋值到属性中
            orderListTimeDTO.setStartTime(startTime);
            orderListTimeDTO.setEndTime(endTime);
        } else {
            // 3.判断时间是否合理,起始时间要小于终止时间
            // toInstant将时间对象转换为时间戳对象
            // ZoneOffset.of("+8")表示东八区时间,北京时间
            // toEpochMilli()将时间转换为1970年1月1日开始的毫秒值
            if (endTime.toInstant(ZoneOffset.of("+8")).toEpochMilli() < startTime.toInstant(ZoneOffset.of("+8")).toEpochMilli()) {
                throw new CoolSharkServiceException(ResponseCode.BAD_REQUEST, "结束时间应该大于起始时间");
            }
        }
        // 4.进行分页
        PageHelper.startPage(orderListTimeDTO.getPage(), orderListTimeDTO.getPageSize());
        // 5.封装用户id
        Long userId = getUserId();
        orderListTimeDTO.setUserId(userId);
        // 6.执行查询
        List<OrderListVO> orderListVOS = omsOrderMapper.selectOrderByTimes(orderListTimeDTO);
        return JsonPage.restPage(new PageInfo<>(orderListVOS));
    }

    /**
     * 根据sn查询订单详细信息
     *
     * @param id
     * @return
     */
    @Override
    public OrderDetailVO getOrderDetail(Long id) {
        return null;
    }

    /**
     * 获取所有登录用户信息
     */
    public CsmallAuthenticationInfo getUserInfo() {
        // 获取SpringSecurity中的上下文容器对象
        UsernamePasswordAuthenticationToken authentication =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new CoolSharkServiceException(ResponseCode.UNAUTHORIZED, "没有登录信息");
        }
        // 如果封装了信息,则将其中的用户信息取出
        CsmallAuthenticationInfo credentials =
                (CsmallAuthenticationInfo) authentication.getCredentials();
        return credentials;
    }

    /**
     * 获取登录的用户id
     */
    public Long getUserId() {
        return getUserInfo().getId();
    }
}
