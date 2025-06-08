package finsim.order.infra;

import finsim.order.entities.Order;

import java.util.List;
import java.util.stream.Collectors;

public final class OrderUtils {
    private OrderUtils() {}

    public static finsim.common.entities.Order convertOrder(Order order) {
        return new finsim.common.entities.Order(
            String.valueOf(order.id), // Convert int to String for common entity
            order.account_id,
            order.instrument_id,
            order.quantity,
            order.price,
            order.buy_sell,
            order.date_time,
            order.status,
            order.duration,
            order.expiry_date_time
        );
    }

    public static List<finsim.common.entities.Order> convertOrders(List<Order> orders) {
        return orders.stream()
                .map(OrderUtils::convertOrder)
                .collect(Collectors.toList());
    }
}