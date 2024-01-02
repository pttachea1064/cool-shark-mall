package cn.tedu.mall.order.controller;

import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.order.service.IOmsCartService;
import cn.tedu.mall.order.service.IOmsOrderService;
import cn.tedu.mall.order.utils.WebConsts;
import cn.tedu.mall.pojo.order.dto.*;
import cn.tedu.mall.pojo.order.vo.CartStandardVO;
import cn.tedu.mall.pojo.order.vo.OrderAddVO;
import cn.tedu.mall.pojo.order.vo.OrderListVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/oms/order")
@Api(tags = "订单功能")
public class OmsOrderController {
    @Autowired
    private IOmsOrderService orderService;

    @PostMapping("/add")
    @ApiOperation("新增订单")
    @PreAuthorize("hasRole('user')")
    public JsonResult<OrderAddVO> addOrder(@Validated OrderAddDTO orderAddDTO) {
        OrderAddVO orderAddVO = orderService.addOrder(orderAddDTO);
        return JsonResult.ok(orderAddVO);
    }

    @GetMapping("/list")
    @ApiOperation("按指定时间进行分页查询")
    @PreAuthorize("hasRole('user')")
    public JsonResult<JsonPage<OrderListVO>> listOrdersByTimes(OrderListTimeDTO orderListTimeDTO) {
        JsonPage<OrderListVO> jsonPage = orderService.listOrdersBetweenTimes(orderListTimeDTO);
        return JsonResult.ok(jsonPage);
    }

    @PostMapping("/update/state")
    @ApiOperation("根据订单id对订单状态进行修改")
    @PreAuthorize("hasRole('user')")
    public JsonResult updateOrderState(OrderStateUpdateDTO orderStateUpdateDTO) {
        orderService.updateOrderState(orderStateUpdateDTO);
        return JsonResult.ok("订单状态修改成功");
    }
}
