package com.jyeeeh.sipspot.dto.ws;

import com.jyeeeh.sipspot.dto.CoffeeResponse;

public record CoffeeSentEvent(String type, CoffeeResponse coffee) {

    public CoffeeSentEvent(CoffeeResponse coffee) {
        this("COFFEE_SENT", coffee);
    }
}
