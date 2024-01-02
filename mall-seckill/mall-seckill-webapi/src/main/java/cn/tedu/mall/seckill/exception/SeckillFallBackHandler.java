package cn.tedu.mall.seckill.exception;

import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.pojo.seckill.dto.SeckillOrderAddDTO;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import lombok.extern.slf4j.Slf4j;

@Slf4j

public class SeckillFallBackHandler {
    public static JsonResult seckillBlock(String randCode,
                                          SeckillOrderAddDTO seckillOrderAddDTO,
                                          Throwable e){
        /**自訂限定流量方法
         * 1.訪問修飾是public
         * 2.添加static修飾 不添加不能調用
         * 3.返回類型要與被限定流量的方法一致
         * 4.參數列表也要與被限定流量的方法保持一致 並且添加上Throwable*/
        log.info("一個請求被降低等級了");
        return JsonResult.failed(ResponseCode.INTERNAL_SERVER_ERROR,"服務忙碌當中");

    }
}
