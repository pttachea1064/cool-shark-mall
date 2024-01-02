package cn.tedu.mall.order.mapper;

import cn.tedu.mall.pojo.order.model.OmsCart;
import cn.tedu.mall.pojo.order.vo.CartStandardVO;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OmsCartMapper {
    // 1.判断当前用户的购物车中是否包含指定的sku信息(商品信息)
    OmsCart selectExistsCart(Long userId, Long skuId);

    // 2.新增sku信息到购物车表(oms_cart)
    void saveCart(OmsCart omsCart);

    // 3.修改购物车中的sku信息的数量(quantity)
    void updateQuantityById(OmsCart omsCart);

    // 4.根据用户id查询购物车中的sku信息
    List<CartStandardVO> selectCartsByUserId(Long userId);

    // 5.删除购物车中指定的商品(可以删除多个商品)
    int deleteCartsByIds(Long[] ids);

    // 6.清空指定用户购物车中所有商品的方法
    int deleteCartsByUserId(Long userId);

    // 7.根据userId和skuId删除商品
    void deleteCartByUserIdAndSkuId(OmsCart omsCart);
}
