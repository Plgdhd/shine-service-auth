package com.plgdhd.authservice.infrastructure;

import org.springframework.stereotype.Component;

// TODO подумать куда его запихнуть
public interface EventSender {

    public void send(String topic, String key, byte[] data);
}
