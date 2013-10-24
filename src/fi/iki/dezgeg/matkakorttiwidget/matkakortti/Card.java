package fi.iki.dezgeg.matkakorttiwidget.matkakortti;

import java.math.BigDecimal;

public class Card {
    private String name;
    private String id;

    private BigDecimal money;

    public Card(String name, String id, BigDecimal money) {
        this.name = name;
        this.id = id;
        this.money = money;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public BigDecimal getMoney() {
        return money;
    }
}
