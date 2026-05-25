package com.telas.notification;

import com.telas.enums.NotificationReference;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class NotificationHandlerRegistryBridge {

    private static NotificationHandlerRegistry registry;

    public NotificationHandlerRegistryBridge(NotificationHandlerRegistry registry) {
        NotificationHandlerRegistryBridge.registry = registry;
    }

    public static Optional<NotificationHandler> find(NotificationReference reference) {
        if (registry == null) {
            return Optional.empty();
        }
        return registry.find(reference);
    }
}
