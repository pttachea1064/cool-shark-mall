package cn.tedu.mall.search.controller;

import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.pojo.search.entity.SpuForElastic;
import cn.tedu.mall.search.service.ISearchService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/search")
@Api(tags = "搜索模块")
public class SearchController {
    @Autowired
    private ISearchService searchService;

    /*
     * 由于搜索模块中的controller方法就一个,所以不需要写后缀
     */
    @GetMapping()
    @ApiOperation("根据用户输入的关键字进行商品的分页查询")
    @ApiImplicitParams({
            @ApiImplicitParam(value = "搜索关键字", name = "keyword", dataType = "string"),
            @ApiImplicitParam(value = "page", name = "page", dataType = "int"),
            @ApiImplicitParam(value = "pageSize", name = "pageSize", dataType = "int")
    })
    public JsonResult<JsonPage<SpuForElastic>> searchByKeyword(String keyword, Integer page, Integer pageSize) {
        JsonPage<SpuForElastic> search = searchService.search(keyword, page, pageSize);
        return JsonResult.ok(search);
    }
}
