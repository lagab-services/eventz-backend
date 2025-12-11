package com.lagab.eventz.app.domain.cart.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.lagab.eventz.app.domain.cart.dto.CartErrorCode;
import com.lagab.eventz.app.domain.cart.dto.CartMessage;
import com.lagab.eventz.app.domain.cart.dto.CartWarningCode;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartValidationResult {
    private Cart cart;
    private List<CartMessage> warnings = new ArrayList<>();
    private List<CartMessage> errors = new ArrayList<>();
    private boolean isValid = true;
    private boolean hasChanges = false;

    public CartValidationResult(Cart cart) {
        this.cart = cart;
    }

    public void addError(CartErrorCode errorCode, String defaultMessage) {
        this.errors.add(CartMessage.error(errorCode, defaultMessage));
        this.isValid = false;
    }

    public void addError(CartErrorCode errorCode, String defaultMessage, Map<String, Object> parameters) {
        this.errors.add(CartMessage.error(errorCode, defaultMessage, parameters));
        this.isValid = false;
    }

    public void addWarning(CartWarningCode warningCode, String defaultMessage) {
        this.warnings.add(CartMessage.warning(warningCode, defaultMessage));
    }

    public void addWarning(CartWarningCode warningCode, String defaultMessage, Map<String, Object> parameters) {
        this.warnings.add(CartMessage.warning(warningCode, defaultMessage, parameters));
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    // Méthode utilitaire pour obtenir tous les messages combinés
    public List<CartMessage> getAllMessages() {
        List<CartMessage> allMessages = new ArrayList<>();
        allMessages.addAll(errors);
        allMessages.addAll(warnings);
        return allMessages;
    }
}
