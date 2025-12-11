package com.lagab.eventz.app.domain.cart.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartMessage {
    private String code;
    private String defaultMessage;
    private Map<String, Object> parameters;
    private MessageSeverity severity;

    public CartMessage(String code, String defaultMessage, MessageSeverity severity) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.severity = severity;
    }

    public static CartMessage error(CartErrorCode errorCode, String defaultMessage) {
        return new CartMessage(errorCode.getCode(), defaultMessage, MessageSeverity.ERROR);
    }

    public static CartMessage error(CartErrorCode errorCode, String defaultMessage, Map<String, Object> parameters) {
        return new CartMessage(errorCode.getCode(), defaultMessage, parameters, MessageSeverity.ERROR);
    }

    public static CartMessage warning(CartWarningCode warningCode, String defaultMessage) {
        return new CartMessage(warningCode.getCode(), defaultMessage, MessageSeverity.WARNING);
    }

    public static CartMessage warning(CartWarningCode warningCode, String defaultMessage, Map<String, Object> parameters) {
        return new CartMessage(warningCode.getCode(), defaultMessage, parameters, MessageSeverity.WARNING);
    }
}
