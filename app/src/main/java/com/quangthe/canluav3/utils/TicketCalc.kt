package com.quangthe.canluav3.utils

object TicketCalc {

    fun tareKg(totalBags: Int, tarePerBag: Int): Double =
        if (tarePerBag > 0) totalBags.toDouble() / tarePerBag else 0.0

    fun impurityKg(totalWeight: Double, impurityPerTon: Int): Double =
        (totalWeight / 1000.0) * impurityPerTon

    fun netKg(totalWeight: Double, totalBags: Int, tarePerBag: Int, impurityPerTon: Int, decimalPlaces: Int = 1): Double {
        val rawNet = totalWeight - tareKg(totalBags, tarePerBag) - impurityKg(totalWeight, impurityPerTon)
        val factor = Math.pow(10.0, decimalPlaces.toDouble())
        return Math.round(rawNet * factor) / factor
    }

    fun priceVnd(netKg: Double, unitPrice: Int): Long = Math.round(netKg * unitPrice)

    fun balanceVnd(price: Long, deposit: Long): Long = price - deposit
}
