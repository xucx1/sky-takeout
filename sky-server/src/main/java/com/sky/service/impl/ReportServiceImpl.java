package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;

    /**
     * 统计指定时间内的营业额数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        // 存放 begin-end 范围内的日期
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            // 日期计算
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        // 将日期以逗号分隔，使用String工具类简化代码
        String dateListString = StringUtils.join(dateList, ",");

        // 遍历每个日期，根据日期查询营业额
        List<Double> turnoverList = new ArrayList<>();  // 存放每天的营业额
        for (LocalDate date : dateList) {
            // 查询date日期对应的营业额数据，营业额指的是：状态为“已完成”的订单金额总和
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);    // date当天的0时0分0秒
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);      // date当天的23时59分59秒99999

            HashMap<Object, Object> map = new HashMap<>();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);

            Double turnover = orderMapper.getByMap(map);
            // 业务优化：如果当天无订单，则返回的turnover为null，此时应该令营业额为0
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);
        }
        String turnoverListString = StringUtils.join(turnoverList, ",");
        // 封装返回结果
        TurnoverReportVO turnoverReportVO = TurnoverReportVO.builder()
                .dateList(dateListString)
                .turnoverList(turnoverListString)
                .build();
        return turnoverReportVO;
    }

    /**
     * 用户统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        // 存放 begin-end 范围内的日期
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            // 日期计算
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        // 遍历每个日期，根据日期查询用户数据
        List<Integer> newUserList = new ArrayList<>();
        List<Integer> totalUserList = new ArrayList<>();
        for (LocalDate date : dateList) {
            // 将传入的参数转换成sql表中相同的格式：精确到秒
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);    // date当天的0时0分0秒
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);      // date当天的23时59分59秒99999
            HashMap<Object, Object> newUserMap = new HashMap<>();
            HashMap<Object, Object> totalUserMap = new HashMap<>();
            newUserMap.put("begin", beginTime);
            newUserMap.put("end", endTime);
            totalUserMap.put("end", endTime);
            Integer newUser = userMapper.getByMap(newUserMap);
            Integer totalUser = userMapper.getByMap(totalUserMap);

            // 如果没有新用户，则返回0
            newUser = newUser == null ? 0 : newUser;
            totalUser = totalUser == null ? 0 : totalUser;
            newUserList.add(newUser);
            totalUserList.add(totalUser);
        }
        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .build();
    }
}
