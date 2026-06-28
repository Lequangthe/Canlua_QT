package com.quangthe.canluav3.data

import kotlinx.coroutines.flow.Flow

class RiceRepository(private val riceDao: RiceDao) {
    val activeTickets: Flow<List<RiceTicket>> = riceDao.getActiveTickets()
    val deletedTickets: Flow<List<RiceTicket>> = riceDao.getDeletedTickets()
    val appSettings: Flow<AppSettings?> = riceDao.getAppSettings()

    suspend fun insertTicket(ticket: RiceTicket): Long = riceDao.insertTicket(ticket)
    suspend fun updateTicket(ticket: RiceTicket) = riceDao.updateTicket(ticket)
    suspend fun softDeleteTicket(id: Int) = riceDao.softDeleteTicket(id)
    suspend fun restoreTicket(id: Int) = riceDao.restoreTicket(id)
    suspend fun permanentDeleteTicket(id: Int) = riceDao.permanentDeleteTicket(id)
    suspend fun getTicketById(id: Int) = riceDao.getTicketById(id)

    // Sheets
    fun getSheetsForTicket(ticketId: Int) = riceDao.getSheetsForTicket(ticketId)
    suspend fun getSheetByIndex(ticketId: Int, sheetIndex: Int) = riceDao.getSheetByIndex(ticketId, sheetIndex)
    suspend fun insertSheet(sheet: RiceSheet) = riceDao.insertSheet(sheet)
    suspend fun deleteSheetsForTicket(ticketId: Int) = riceDao.deleteSheetsAndCellsForTicket(ticketId)

    // Cells
    fun getCellsForSheet(sheetId: Int) = riceDao.getCellsForSheet(sheetId)
    suspend fun insertCell(cell: RiceCell) = riceDao.insertCell(cell)

    suspend fun updateAppSettings(settings: AppSettings) = riceDao.updateAppSettings(settings)

    // Backup/Restore
    suspend fun getAllTickets(): List<RiceTicket> = riceDao.getAllTickets()
    suspend fun getAllSheets(): List<RiceSheet> = riceDao.getAllSheets()
    suspend fun getAllCells(): List<RiceCell> = riceDao.getAllCells()
    suspend fun clearAndRestore(tickets: List<RiceTicket>, sheets: List<RiceSheet>, cells: List<RiceCell>) = 
        riceDao.clearAndRestore(tickets, sheets, cells)
}
