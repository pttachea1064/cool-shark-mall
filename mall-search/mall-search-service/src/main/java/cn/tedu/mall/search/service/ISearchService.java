package cn.tedu.mall.search.service;

import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.pojo.search.entity.SpuForElastic;

public interface ISearchService {

    /**
     * 向ES中加载数据的方法
     */
    void loadSpuByPage();

    /**
     * ES分页查询
     *
     * @param keyword  查询关键字
     * @param page     页数
     * @param pageSize 每页记录数
     * @return 匹配的数据
     */
    JsonPage<SpuForElastic> search(String keyword, Integer page, Integer pageSize);
}








