package cn.tedu.mall.order.service.impl;

import cn.tedu.mall.common.exception.CoolSharkServiceException;
import cn.tedu.mall.common.pojo.domain.CsmallAuthenticationInfo;
import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.order.mapper.OmsCartMapper;
import cn.tedu.mall.order.service.IOmsCartService;
import cn.tedu.mall.pojo.order.dto.CartAddDTO;
import cn.tedu.mall.pojo.order.dto.CartUpdateDTO;
import cn.tedu.mall.pojo.order.model.OmsCart;
import cn.tedu.mall.pojo.order.vo.CartStandardVO;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OmsCartServiceImpl implements IOmsCartService {
    @Autowired
    private OmsCartMapper omsCartMapper;

    /**
     * 新增购物车
     *
     * @param cartDTO 新增的商品的信息
     */
    @Override
    public void addCart(CartAddDTO cartDTO) {
        // 1.获取当前登录的用户id
        Long userId = getUserId();
        // 2.根据用户id查询购物车中是否包含商品
        OmsCart omsCart = omsCartMapper.selectExistsCart(userId, cartDTO.getSkuId());
        // 3.判断omsCart是否为null
        if (omsCart != null) {
            // 4.如果不为空,说明购物车中已经包含这个sku商品,那么我们来修改商品的数量
            Integer oldQuantity = omsCart.getQuantity(); //购物车原有商品数量
            Integer newQuantity = cartDTO.getQuantity(); //新增的商品数量
            Integer nowQuantity = oldQuantity + newQuantity; //最新的商品数量是两者相加
            omsCart.setQuantity(nowQuantity);
            // 5.修改购物车商品数量
            omsCartMapper.updateQuantityById(omsCart);
        } else {
            // 6.如果为空,说明购物车中不含当前商品,所以做新增操作
            OmsCart newOmsCart = new OmsCart();
            BeanUtils.copyProperties(cartDTO, newOmsCart);
            // 7.由于CartDTO中不包含用户id,所以我们要存储用户id
            newOmsCart.setUserId(userId);
            // 8.执行新增操作
            omsCartMapper.saveCart(newOmsCart);
        }
    }

    /**
     * 查询我的购物车
     *
     * @param page     页数
     * @param pageSize 每页的记录数
     * @return 购物车商品信息
     */
    @Override
    public JsonPage<CartStandardVO> listCarts(Integer page, Integer pageSize) {
        Long userId = getUserId();
        PageHelper.startPage(page, pageSize);
        List<CartStandardVO> cartStandardVOS = omsCartMapper.selectCartsByUserId(userId);
        return JsonPage.restPage(new PageInfo<>(cartStandardVOS));
    }


    /**
     * 批量删除购物车
     *
     * @param ids 删除的购物车商品id(可以多个)
     */
    @Override
    public void removeCart(Long[] ids) {
        int rows = omsCartMapper.deleteCartsByIds(ids);
        if (rows == 0) {
            throw new CoolSharkServiceException(ResponseCode.NOT_FOUND, "您要删除的商品不存在");
        }
    }

    /**
     * 清空购物车
     */
    @Override
    public void removeAllCarts() {
        Long userId = getUserId();
        int rows = omsCartMapper.deleteCartsByUserId(userId);
        if (rows == 0) {
            throw new CoolSharkServiceException(ResponseCode.NOT_FOUND, "您的购物车中没有商品");
        }
    }

    /**
     * 删除指定用户购物车中的执行商品
     *
     * @param omsCart
     */
    @Override
    public void removeUserCarts(OmsCart omsCart) {
        omsCartMapper.deleteCartByUserIdAndSkuId(omsCart);
    }

    /**
     * 更新购物车商品数量
     *
     * @param cartUpdateDTO 包含商品的id和数量
     */
    @Override
    public void updateQuantity(CartUpdateDTO cartUpdateDTO) {
        OmsCart omsCart = new OmsCart();
        BeanUtils.copyProperties(cartUpdateDTO, omsCart);
        omsCartMapper.updateQuantityById(omsCart);
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
