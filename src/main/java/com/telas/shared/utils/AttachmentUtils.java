package com.telas.shared.utils;

import com.telas.dtos.request.AttachmentRequestDto;
import com.telas.entities.AdvertisingAttachment;
import com.telas.entities.Attachment;

public class AttachmentUtils {
    private static final String DOT = ".";

    private AttachmentUtils() {
    }

    public static String format(Attachment attachment) {
        return attachment.getId().toString() + DOT + getType(attachment.getName());
    }

    public static String format(AdvertisingAttachment attachment) {
        return attachment.getId().toString() + DOT + getType(attachment.getName());
    }

    public static String format(AttachmentRequestDto attachment) {
        return attachment.getId().toString() + DOT + getType(attachment.getName());
    }

    private static String getType(String name) {
        return name.substring(name.lastIndexOf(DOT) + 1);
    }
}
