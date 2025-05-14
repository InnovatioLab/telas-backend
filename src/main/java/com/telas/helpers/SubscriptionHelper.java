package com.telas.helpers;

import com.telas.entities.CartItem;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SubscriptionHelper {
    @Value("${monitor.block.price}")
    private String monitorBlockPrice;

    public BigDecimal calculateTotalPrice(List<CartItem> items) {
        BigDecimal blockPrice = new BigDecimal(monitorBlockPrice);
        return items.stream()
                .map(item -> blockPrice.multiply(BigDecimal.valueOf(item.getBlockQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
