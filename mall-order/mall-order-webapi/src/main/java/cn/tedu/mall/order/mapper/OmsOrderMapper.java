package cn.tedu.mall.order.mapper;

import cn.tedu.mall.pojo.order.dto.OrderListTimeDTO;
import cn.tedu.mall.pojo.order.model.OmsOrder;
import cn.tedu.mall.pojo.order.vo.OrderListVO;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OmsOrderMapper {
    // 新增订单到oms_order表中
    void insertOrder(OmsOrder omsOrder);

    //查询当前用户指定时间范围的订单 -- 分页查询
    List<OrderListVO> selectOrderByTimes(OrderListTimeDTO orderListTimeDTO);

    //修改订单信息
    void updateOrderById(OmsOrder omsOrder);
}
