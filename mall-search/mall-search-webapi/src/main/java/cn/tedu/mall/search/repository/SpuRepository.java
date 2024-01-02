package cn.tedu.mall.search.repository;

import cn.tedu.mall.pojo.search.entity.SpuForElastic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpuRepository extends ElasticsearchRepository<SpuForElastic, Long> {
    /*
     * 后期如果条件比较复杂,不建议使用固定的方法名来执行ES的自定义查询,太麻烦,效率也低
     * 所以可以使用@Query这个注解,将固定的搜索格式,作为参数,
     * 只不过不固定的值部分,则有 ?0 占位
     */
    @Query("{\"bool\": {\n" +
            "      \"should\": [\n" +
            "        {\"match\": {\"name\": \"?0\"}},\n" +
            "        {\"match\": {\"description\": \"?0\"}\n" +
            "        }]\n" +
            "    }}")
    Page<SpuForElastic> querySearch(String keyword, Pageable pageable);
}
