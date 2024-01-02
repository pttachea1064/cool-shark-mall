package cn.tedu.mall.search;

import cn.tedu.mall.pojo.search.entity.SpuForElastic;
import cn.tedu.mall.search.repository.SpuRepository;
import cn.tedu.mall.search.service.ISearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.function.Consumer;

@SpringBootTest
public class TestSearch {
    @Autowired
    private ISearchService service;

    @Test
    public void loadData() {
        service.loadSpuByPage();
        System.out.println("加载数据到ES中成功!");
    }

    @Autowired
    private SpuRepository spuRepository;

    @Test
    public void showAll() {
        Iterable<SpuForElastic> all = spuRepository.findAll();
        all.forEach(s -> System.out.println(s));
    }
}
