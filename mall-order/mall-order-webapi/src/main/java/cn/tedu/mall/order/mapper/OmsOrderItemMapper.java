package cn.tedu.mall.order.mapper;

import cn.tedu.mall.pojo.order.model.OmsOrder;
import cn.tedu.mall.pojo.order.model.OmsOrderItem;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OmsOrderItemMapper {
    // 新增订单到oms_order_item表中
    // 一个订单中可能包含多个订单项(商品信息)
    void insertOrderItem(List<OmsOrderItem> omsOrderItems);
}
