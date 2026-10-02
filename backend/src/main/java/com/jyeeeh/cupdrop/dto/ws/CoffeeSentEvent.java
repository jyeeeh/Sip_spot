package com.jyeeeh.cupdrop.dto.ws;

import com.jyeeeh.cupdrop.dto.CoffeeResponse;

public record CoffeeSentEvent(String type, CoffeeResponse coffee) {

    public CoffeeSentEvent(CoffeeResponse coffee) {
        this("COFFEE_SENT", coffee);
    }
}
