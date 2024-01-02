package cn.tedu.mall.front.controller;

import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.front.service.IFrontProductService;
import cn.tedu.mall.pojo.product.vo.SpuDetailStandardVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/front/spu/detail")
@Api(tags = "前台spuDetail模块")
public class FrontSpuDetailController {
    @Autowired
    private IFrontProductService frontProductService;

    // 根据SpuId查询Spu的detail信息
    @GetMapping("/{spuId}")
    @ApiOperation("根据SpuId查询Spu的detail信息")
    @ApiImplicitParam(value = "spuId", name = "spuId", example = "1",
            required = true, dataType = "long")
    public JsonResult<SpuDetailStandardVO> getSpuDetail(@PathVariable Long spuId) {
        SpuDetailStandardVO spuDetailStandardVO =
                frontProductService.getSpuDetail(spuId);
        return JsonResult.ok(spuDetailStandardVO);
    }
}