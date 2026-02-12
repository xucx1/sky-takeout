package com.sky.mapper;

import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderDetailMapper {

    /**
     * 向订单明细表中批量插入数据
     * @param orderDetailList
     */
    void insertBatch(List<OrderDetail> orderDetailList);

    /**
     * 根据商品id从明细表查询详细信息
     * @param orderId
     * @return
     */
    @Select("select * from order_detail where order_id = #{orderId}")
    List<OrderDetail> getByOrderId(Long orderId);
}
