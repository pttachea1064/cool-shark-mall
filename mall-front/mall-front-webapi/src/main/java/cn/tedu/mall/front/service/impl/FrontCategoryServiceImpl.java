package cn.tedu.mall.front.service.impl;

import cn.tedu.mall.common.exception.CoolSharkServiceException;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.front.service.IFrontCategoryService;
import cn.tedu.mall.pojo.front.entity.FrontCategoryEntity;
import cn.tedu.mall.pojo.front.vo.FrontCategoryTreeVO;
import cn.tedu.mall.pojo.product.vo.CategoryStandardVO;
import cn.tedu.mall.product.service.front.IForFrontCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.sql.Time;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

//Dubbo服务的提供者
@DubboService
@Service
@Slf4j
public class FrontCategoryServiceImpl implements IFrontCategoryService {
    // 为了降低redis中的key值的拼写错误的风险,所以我们经常会将redis的key定义为常量,方便使用
    public static final String CATEGORY_TREE_KEY = "category_tree";
    @Autowired
    private RedisTemplate redisTemplate;
    // 远程调用该接口中的查询所有分类信息的方法
    @DubboReference
    private IForFrontCategoryService dubboCategoryService;

    /**
     * 首页查询分类树
     * 需要返回固定结构
     */
    @Override
    public FrontCategoryTreeVO categoryTree() {
        FrontCategoryTreeVO treeVO = null;
        // 1.查询redis中是否包含key为CATEGORY_TREE_KEY值的value
        if (redisTemplate.hasKey(CATEGORY_TREE_KEY)) {
            // 2.如果判断redis中包含值,直接将该值返回即可
            treeVO = (FrontCategoryTreeVO) redisTemplate.boundValueOps(CATEGORY_TREE_KEY).get();
            return treeVO;
        }
        // 3.代码执行到这里,说明redis中没有三级分类树信息,直接查询数据库
        List<CategoryStandardVO> categoryList = dubboCategoryService.getCategoryList();
        // 4.创建一个Map集合,用于封装相同的parent_id的分类信息
        // FrontCategoryEntity包含了children属性,可以接受下一层级的分类信息
        Map<Long, List<FrontCategoryEntity>> map = new HashMap<>();
        // 5.判断查询数据库的结果是否为空,为空,则抛出异常
        if (categoryList == null) {
            throw new CoolSharkServiceException(ResponseCode.INTERNAL_SERVER_ERROR, "分类信息不能为空");
        }
        log.info("当前分类对象总数:{}", categoryList.size());
        // 6.开始遍历集合,获取分类信息,进行组装
        for (CategoryStandardVO categoryStandardVO : categoryList) {
            // 6.1由于CategoryStandardVO没有children属性,所以我们需要使用FrontCategoryEntity
            FrontCategoryEntity frontCategoryEntity = new FrontCategoryEntity();
            // 6.2利用BeanUtils工具类,将CategoryStandardVO的属性值赋值到FrontCategoryEntity中
            BeanUtils.copyProperties(categoryStandardVO, frontCategoryEntity);
            // 6.3获取每个分类对象的parentId
            Long parentId = frontCategoryEntity.getParentId();
            // 6.4判断map中是否包含parentId这个key
            if (map.containsKey(parentId)) {
                // 6.5如果包含,说明已经创建集合了,直接将遍历的分类信息存储到该集合中即可
                map.get(parentId).add(frontCategoryEntity);
            } else {
                // 6.6如果不包含,说明parentId是一个新的值,创建一个集合,用于封装相同parent_id的分类信息
                ArrayList<FrontCategoryEntity> value = new ArrayList<>();
                // 6.7将当前遍历的分类信息存储到集合中
                value.add(frontCategoryEntity);
                // 6.8将parentId作为key,集合作为value存储
                map.put(parentId, value);
            }
        }
        // 7.将子类对象关联到父类对象的children属性中,获取所有的一级分类
        List<FrontCategoryEntity> firstLevels = map.get(0L);
        // 8.判断一级分类信息是否为空,如果为空,抛出异常
        if (firstLevels == null) {
            throw new CoolSharkServiceException(ResponseCode.INTERNAL_SERVER_ERROR, "当前项目没有一级分类!");
        }
        // 9.如果有一级分类,遍历所有的一级分类,拼接二级分类
        for (FrontCategoryEntity firstLevel : firstLevels) {
            // 9.1获取当前一级分类的id(是二级分类的parentId)
            Long secondLevelParentId = firstLevel.getId();
            // 9.2获取当前分类对象的所有的二级分类
            List<FrontCategoryEntity> secondLevels = map.get(secondLevelParentId);
            // 9.3判断二级分类是否为空
            if (secondLevels == null) {
                //9.4如果二级分类为空,就终止循环
                log.warn("当前一级分类ID: {},缺少二级分类内容", secondLevelParentId);
                continue;
            }
            // 9.5遍历二级分类集合
            for (FrontCategoryEntity secondLevel : secondLevels) {
                // 9.5.1获取当前二级分类的id(是三级分类的parentId)
                Long thirdLevelParentId = secondLevel.getId();
                // 9.5.2获取当前分类对象的所有的三级分类
                List<FrontCategoryEntity> thirdLevels = map.get(thirdLevelParentId);
                // 9.5.3判断三级分类是否为空
                if (thirdLevels == null) {
                    // 9.5.4如果三级分类为空,就终止循环
                    log.warn("当前而级分类ID: {},缺少三级分类内容", thirdLevelParentId);
                    continue;
                }
                // 9.5.5将三级分类对象(集合)添加到二级分类对象的children属性中
                secondLevel.setChildrens(thirdLevels);
            }
            // 9.6将二级分类对象(集合)添加到一级分类对象的children属性中
            firstLevel.setChildrens(secondLevels);
        }
        // 10.到此为止,所有分类对象的父子关系已经构建完成,将一级分类对象返回给前端
        treeVO = new FrontCategoryTreeVO();
        treeVO.setCategories(firstLevels);
        // 11.将查询的树结构数据,存储到redis中,并且设置一下生效时间为一天
        redisTemplate.boundValueOps(CATEGORY_TREE_KEY).set(treeVO, 24, TimeUnit.HOURS);
        return treeVO;
    }
}
