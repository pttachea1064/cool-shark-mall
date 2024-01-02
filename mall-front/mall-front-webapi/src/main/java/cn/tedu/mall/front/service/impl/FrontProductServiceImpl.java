package cn.tedu.mall.front.service.impl;

import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.front.service.IFrontProductService;
import cn.tedu.mall.pojo.product.vo.*;
import cn.tedu.mall.product.service.front.IForFrontAttributeService;
import cn.tedu.mall.product.service.front.IForFrontSkuService;
import cn.tedu.mall.product.service.front.IForFrontSpuService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FrontProductServiceImpl implements IFrontProductService {
    @DubboReference
    private IForFrontSpuService forFrontSpuService;
    @DubboReference
    private IForFrontSkuService forFrontSkuService;
    @DubboReference
    private IForFrontAttributeService forFrontAttributeService;

    /**
     * 根据分类id查询spu列表 -- 分页查询
     *
     * @param categoryId 分类id
     * @param page       第几页
     * @param pageSize   每页的记录数
     * @return 指定页的数据
     */
    @Override
    public JsonPage<SpuListItemVO> listSpuByCategoryId(Long categoryId, Integer page, Integer pageSize) {
        JsonPage<SpuListItemVO> jsonPage =
                forFrontSpuService.listSpuByCategoryId(categoryId, page, pageSize);
        return jsonPage;
    }

    /**
     * 根据id查询spuvo对象
     *
     * @param id
     */
    @Override
    public SpuStandardVO getFrontSpuById(Long id) {
        SpuStandardVO spuStandardVO = forFrontSpuService.getSpuById(id);
        return spuStandardVO;
    }

    /**
     * 根据spuId查询sku信息
     *
     * @param spuId
     * @return 因为SPU是由一条或者多条SKU组成的
     */
    @Override
    public List<SkuStandardVO> getFrontSkusBySpuId(Long spuId) {
        List<SkuStandardVO> skuStandardVOS = forFrontSkuService.getSkusBySpuId(spuId);
        return skuStandardVOS;
    }

    /**
     * 利用spuId查询spu详情
     *
     * @param spuId
     * @return
     */
    @Override
    public SpuDetailStandardVO getSpuDetail(Long spuId) {
        SpuDetailStandardVO spuDetailById = forFrontSpuService.getSpuDetailById(spuId);
        return spuDetailById;
    }

    /**
     * 微服务调用pms查询一个spu绑定的所有属性和值
     *
     * @param spuId
     * @return
     */
    @Override
    public List<AttributeStandardVO> getSpuAttributesBySpuId(Long spuId) {
        List<AttributeStandardVO> spuAttributesBySpuId = forFrontAttributeService.getSpuAttributesBySpuId(spuId);
        return spuAttributesBySpuId;
    }
}
