package com.jyeeeh.sipspot.dto.ws;

public record CoffeeDeletedEvent(String type, Long coffeeId) {

    public CoffeeDeletedEvent(Long coffeeId) {
        this("COFFEE_DELETED", coffeeId);
    }
}
