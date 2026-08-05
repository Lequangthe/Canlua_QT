package com.quangthe.canluav3.utils

import org.junit.Test
import org.junit.Assert.*

class TicketCalcTest {

    @Test
    fun tareKg_usesBagsPerKg() {
        assertEquals(12.5, TicketCalc.tareKg(100, 8), 0.0)
        assertEquals(1.0, TicketCalc.tareKg(8, 8), 0.0)
        assertEquals(0.0, TicketCalc.tareKg(100, 0), 0.0)
        assertEquals(0.0, TicketCalc.tareKg(100, -1), 0.0)
    }

    @Test
    fun impurityKg_perTon() {
        assertEquals(5.0, TicketCalc.impurityKg(1000.0, 5), 0.0001)
        assertEquals(6.0, TicketCalc.impurityKg(2000.0, 3), 0.0001)
        assertEquals(0.0, TicketCalc.impurityKg(1000.0, 0), 0.0)
    }

    @Test
    fun netKg_subtractsTareAndImpurity() {
        // 1000 kg gross, 80 bao bì -> trừ 10 kg; tạp chất 5/t -> 5 kg
        assertEquals(985.0, TicketCalc.netKg(1000.0, 80, 8, 5), 0.0001)
        // tarePerBag = 0 -> không trừ bì, chỉ trừ tạp chất
        assertEquals(95.0, TicketCalc.netKg(100.0, 50, 0, 0), 0.0001)
    }

    @Test
    fun priceVnd_roundsToWholeVnd() {
        assertEquals(12_350L, TicketCalc.priceVnd(12.35, 1000))
        assertEquals(0L, TicketCalc.priceVnd(0.4, 1000))
        assertEquals(1_000L, TicketCalc.priceVnd(0.5, 1000))
        assertEquals(0L, TicketCalc.priceVnd(0.0, 10000))
        assertEquals(0L, TicketCalc.priceVnd(12.0, 0))
    }

    @Test
    fun balanceVnd_subtractsDeposit() {
        assertEquals(80_000L, TicketCalc.balanceVnd(100_000L, 20_000L))
        assertEquals(-20_000L, TicketCalc.balanceVnd(0L, 20_000L))
    }
}