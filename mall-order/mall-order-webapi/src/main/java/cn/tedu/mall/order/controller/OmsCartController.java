package cn.tedu.mall.order.controller;

import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.order.service.IOmsCartService;
import cn.tedu.mall.order.utils.WebConsts;
import cn.tedu.mall.pojo.order.dto.CartAddDTO;
import cn.tedu.mall.pojo.order.dto.CartUpdateDTO;
import cn.tedu.mall.pojo.order.vo.CartStandardVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/oms/cart")
@Api(tags = "购物车管理模块")
public class OmsCartController {
    @Autowired
    private IOmsCartService omsCartService;

    @PostMapping("/add")
    @ApiOperation("新增购物车信息")
    // 判断当前用户是否登录,并具备普通用户的权限ROLE_user,访问前台的普通用户
    @PreAuthorize("hasAuthority('ROLE_user')")
    // 参数CartAddDTO中需要经由SpringValidation框架校验参数是否完整方可新增
    public JsonResult addCart(@Validated CartAddDTO cartAddDTO) {
        omsCartService.addCart(cartAddDTO);
        return JsonResult.ok("新增购物车sku完成");
    }

    @GetMapping("/list")
    @ApiOperation("分页查询用户购物车中商品信息")
    @ApiImplicitParams({
            @ApiImplicitParam(value = "页码", name = "page", dataType = "int"),
            @ApiImplicitParam(value = "每页条数", name = "pageSize", dataType = "int")
    })
    //如果调用hasAuthority方法,那么角色要写全,如果调用的是hasRole方法,那么会自动补全ROLE_,所以此时user等价于ROLE_user
    @PreAuthorize("hasRole('user')")
    public JsonResult<JsonPage<CartStandardVO>> listCartByPage(
            @RequestParam(required = false, defaultValue = WebConsts.DEFAULT_PAGE) Integer page,
            @RequestParam(required = false, defaultValue = WebConsts.DEFAULT_PAGE_SIZE) Integer pageSize) {
        JsonPage<CartStandardVO> jsonPage = omsCartService.listCarts(page, pageSize);
        return JsonResult.ok(jsonPage);
    }

    // 根据id的数组删除购物车中sku商品的方法
    @PostMapping("/delete")
    @ApiOperation("根据id的数组删除购物车中sku商品")
    @ApiImplicitParam(value = "要删除的购物车id数组", name = "ids",
            required = true, dataType = "array")
    @PreAuthorize("hasRole('user')")
    public JsonResult removeCartsByIds(Long[] ids) {
        omsCartService.removeCart(ids);
        return JsonResult.ok();
    }

    @PostMapping("/delete/all")
    @ApiOperation("清空当前登录用户的购物车")
    @PreAuthorize("hasRole('user')")
    public JsonResult removeCartsByUserId() {
        omsCartService.removeAllCarts();
        return JsonResult.ok("购物车清空完成");
    }

    // 修改购物车数量
    @PostMapping("/update/quantity")
    @ApiOperation("修改购物车数量")
    @PreAuthorize("hasRole('user')")
    public JsonResult updateQuantity(@Validated CartUpdateDTO cartUpdateDTO) {
        omsCartService.updateQuantity(cartUpdateDTO);
        return JsonResult.ok("修改完成!");
    }
}
