/*
 * Matkakortti Widget is licensed under GPLv2.
 * See LICENSE.txt for more information.
 */

package fi.iki.dezgeg.matkakorttiwidget.matkakortti;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

public class Card implements Serializable {
    private String name;
    private String id;

    private BigDecimal money;
    private Date periodExpiryDate;

    public Card(String name, String id, BigDecimal money, Date periodExpiryDate) {
        this.name = name;
        this.id = id;
        this.money = money;
        this.periodExpiryDate = periodExpiryDate;
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

    public Date getPeriodExpiryDate() {
        return periodExpiryDate;
    }

    @Override
    public String toString() {
        return "Card{" +
                "name='" + name + '\'' +
                ", id='" + id + '\'' +
                ", money=" + money +
                ", periodExpiryDate=" + periodExpiryDate +
                '}';
    }
}
