package com.xiaokai.task;

import com.xiaokai.entity.Orders;
import com.xiaokai.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static com.xiaokai.constant.MessageConstant.ORDER_OVER_TIME;

/**
 * 定时任务类, 定时处理订单状态
 */
@Component
@Slf4j
public class TimeTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 处理超时订单
     */
    @Scheduled(cron = "15 * * * * ?") // 每分钟执行一次
    public void processTimeoutOrder() {
        log.info("定时处理超时订单：{}", LocalDateTime.now());

        LocalDateTime time = LocalDateTime.now().minusMinutes(15);

        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, time);

        if (ordersList != null && !ordersList.isEmpty()) {
            for (Orders orders : ordersList) {
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelReason(ORDER_OVER_TIME);
                orders.setCancelTime(LocalDateTime.now());
                orderMapper.update(orders);
            }
        }
    }

    /**
     * 定时自动接单订单
     */
    @Scheduled(cron = "10 * * * * ?") // 每分钟执行一次
    public void processConfirmedOrder() {
        log.info("自动接单订单：{}", LocalDateTime.now());

        LocalDateTime time = LocalDateTime.now();

        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.TO_BE_CONFIRMED, time);

        if (ordersList != null && !ordersList.isEmpty()) {
            for (Orders orders : ordersList) {
                orders.setStatus(Orders.CONFIRMED);
                orderMapper.update(orders);
            }
        }
    }

    /**
     * 处理一直处于派送中状态的订单
     * */
    @Scheduled(cron="0 0 0 * * ?") // 每天凌晨12点更新订单状态
    @Scheduled(cron="5 0 0/2 * * ?") // 每两小时更新订单状态
    public void processDeliveryOrder(){
        log.info("定时处理派送中订单：{}", LocalDateTime.now());

        LocalDateTime time = LocalDateTime.now();
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.DELIVERY_IN_PROGRESS, time);

        if (ordersList != null && !ordersList.isEmpty()) {
            for (Orders orders : ordersList) {
                orders.setStatus(Orders.COMPLETED);
                orderMapper.update(orders);
            }
        }
    }

}
