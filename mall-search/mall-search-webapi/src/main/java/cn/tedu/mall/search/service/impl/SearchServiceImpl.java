package cn.tedu.mall.search.service.impl;

import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.pojo.product.model.Spu;
import cn.tedu.mall.pojo.search.entity.SpuForElastic;
import cn.tedu.mall.product.service.front.IForFrontSpuService;
import cn.tedu.mall.search.repository.SpuRepository;
import cn.tedu.mall.search.service.ISearchService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SearchServiceImpl implements ISearchService {
    //针对数据库
    @DubboReference
    private IForFrontSpuService forFrontSpuService;
    //针对ES
    @Autowired
    private SpuRepository spuRepository;

    @Override
    public void loadSpuByPage() {
        //页码
        int page = 1;
        //每页条数
        int pageSize = 10;
        //总页数(目前不知道有多少页,所以这是初始值)
        int pages = 0;
        //循环处理数据
        do {
            //根据分页信息,查询当前页的内容
            JsonPage<Spu> jsonPage = forFrontSpuService.getSpuByPage(page, pageSize);
            //准备一个集合,用于封装ES的数据实例
            List<SpuForElastic> elastics = new ArrayList<>();
            //遍历JsonPage,将其中的数据一条条的转换为SpuForElastic类型,封装到集合中
            for (Spu spu : jsonPage.getList()) {
                SpuForElastic spuForElastic = new SpuForElastic();
                BeanUtils.copyProperties(spu, spuForElastic);
                elastics.add(spuForElastic);
            }
            //将这一页的spu信息,存储到ES中
            spuRepository.saveAll(elastics);
            //封装的总页数
            pages = jsonPage.getTotalPage();
            //第一页数据存储完毕,进行第二页
            page++;
            //判断条件就是当前页数小于等于总页数时,则没存储完,继续存储
        } while (page <= pages);
    }

    /**
     * ES分页查询
     *
     * @param keyword  查询关键字
     * @param page     页数
     * @param pageSize 每页记录数
     * @return 匹配的数据
     */
    @Override
    public JsonPage<SpuForElastic> search(String keyword, Integer page, Integer pageSize) {
        //0才是第一页
        Page<SpuForElastic> spuForElastics = spuRepository.querySearch(keyword, PageRequest.of(page - 1, pageSize));
        JsonPage<SpuForElastic> jsonPage = new JsonPage<>();
        //设置当前页码
        jsonPage.setPage(page);
        //设置每页条数
        jsonPage.setPageSize(pageSize);
        //设置总页数
        jsonPage.setTotalPage(spuForElastics.getTotalPages());
        //设置总条数
        jsonPage.setTotal(spuForElastics.getTotalElements());
        jsonPage.setList(spuForElastics.getContent());
        return jsonPage;
    }
}
