/*
 *
 * Copyright (C) 2016, Telco Cloud Trading & Logistic Ltd
 *
 * This file is part of dodicall.
 * dodicall is free software : you can redistribute it and / or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * dodicall is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with dodicall.If not, see <http://www.gnu.org/licenses/>.
 */

package ru.swisstok.dodicall.api;

import ru.uls_global.dodicall.Currency;

public class Balance {

    public static final int CURRENCY_RUBLE = Currency.CurrencyRuble.swigValue();
    public static final int CURRENCY_EURO = Currency.CurrencyEur.swigValue();
    public static final int CURRENCY_USD = Currency.CurrencyUsd.swigValue();

    public Integer currency;
    public Double value;

    public boolean isSuccessful() {
        return currency != null && value != null;
    }
}
