package com.telas.notification;

import com.telas.enums.NotificationReference;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class NotificationHandlerRegistry {

    private final Map<NotificationReference, NotificationHandler> handlers;

    public NotificationHandlerRegistry(List<NotificationHandler> handlerList) {
        this.handlers = new EnumMap<>(NotificationReference.class);
        for (NotificationHandler handler : handlerList) {
            if (handler.getReference() != null) {
                handlers.put(handler.getReference(), handler);
            }
        }
    }

    public Optional<NotificationHandler> find(NotificationReference reference) {
        return Optional.ofNullable(handlers.get(reference));
    }
}
