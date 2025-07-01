package com.telas.shared.utils;

import com.telas.dtos.request.AttachmentRequestDto;
import com.telas.entities.Ad;
import com.telas.entities.Attachment;
import com.telas.infra.exceptions.BusinessRuleException;

public class AttachmentUtils {
  private static final String DOT = ".";

  private AttachmentUtils() {
  }

  public static String format(Object attachment) {
    if (attachment instanceof Attachment) {
      return format((Attachment) attachment);
    } else if (attachment instanceof Ad) {
      return format((Ad) attachment);
    } else if (attachment instanceof AttachmentRequestDto) {
      return format((AttachmentRequestDto) attachment);
    } else {
      throw new BusinessRuleException("Unsupported attachment type: " + attachment.getClass().getName());
    }
  }

  public static String format(Attachment attachment) {
    return attachment.getId().toString() + DOT + getType(attachment.getName());
  }

  public static String format(Ad attachment) {
    return attachment.getId().toString() + DOT + getType(attachment.getName());
  }

  public static String format(AttachmentRequestDto attachment) {
    return attachment.getId().toString() + DOT + getType(attachment.getName());
  }

  private static String getType(String name) {
    return name.substring(name.lastIndexOf(DOT) + 1);
  }
}
