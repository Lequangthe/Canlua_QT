package com.quangthe.canluav3.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RiceDao {
    // Tickets
    @Query("SELECT * FROM rice_tickets WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getActiveTickets(): Flow<List<RiceTicket>>

    @Query("SELECT * FROM rice_tickets WHERE isDeleted = 1 ORDER BY createdAt DESC")
    fun getDeletedTickets(): Flow<List<RiceTicket>>

    @Query("SELECT * FROM rice_tickets WHERE id = :id")
    suspend fun getTicketById(id: Int): RiceTicket?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: RiceTicket): Long

    @Update
    suspend fun updateTicket(ticket: RiceTicket)

    @Query("UPDATE rice_tickets SET isDeleted = 1 WHERE id = :id")
    suspend fun softDeleteTicket(id: Int)

    @Query("UPDATE rice_tickets SET isDeleted = 0 WHERE id = :id")
    suspend fun restoreTicket(id: Int)

    @Query("DELETE FROM rice_tickets WHERE id = :id")
    suspend fun permanentDeleteTicket(id: Int)

    // Rice Sheets
    @Query("SELECT * FROM rice_sheets WHERE ticketId = :ticketId ORDER BY sheetIndex ASC")
    fun getSheetsForTicket(ticketId: Int): Flow<List<RiceSheet>>

    @Query("DELETE FROM rice_sheets WHERE ticketId = :ticketId")
    suspend fun deleteSheetsForTicket(ticketId: Int)

    @Query("DELETE FROM rice_cells WHERE sheetId IN (SELECT id FROM rice_sheets WHERE ticketId = :ticketId)")
    suspend fun deleteCellsForTicket(ticketId: Int)

    @Query("SELECT * FROM rice_sheets WHERE ticketId = :ticketId AND sheetIndex = :sheetIndex")
    suspend fun getSheetByIndex(ticketId: Int, sheetIndex: Int): RiceSheet?

    @Transaction
    suspend fun deleteSheetsAndCellsForTicket(ticketId: Int) {
        deleteCellsForTicket(ticketId)
        deleteSheetsForTicket(ticketId)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSheet(sheet: RiceSheet): Long

    // Rice Cells
    @Query("SELECT * FROM rice_cells WHERE sheetId = :sheetId")
    fun getCellsForSheet(sheetId: Int): Flow<List<RiceCell>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCell(cell: RiceCell)

    @Query("DELETE FROM rice_cells WHERE sheetId = :sheetId AND rowIndex = :rowIndex AND colIndex = :colIndex")
    suspend fun deleteCell(sheetId: Int, rowIndex: Int, colIndex: Int)

    // App Settings
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun getAppSettings(): Flow<AppSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateAppSettings(settings: AppSettings)

    // Backup/Restore
    @Query("SELECT * FROM rice_sheets")
    suspend fun getAllSheets(): List<RiceSheet>

    @Query("SELECT * FROM rice_cells")
    suspend fun getAllCells(): List<RiceCell>

    @Query("SELECT * FROM rice_tickets")
    suspend fun getAllTickets(): List<RiceTicket>

    @Transaction
    suspend fun clearAndRestore(tickets: List<RiceTicket>, sheets: List<RiceSheet>, cells: List<RiceCell>) {
        deleteAllTickets()
        deleteAllSheets()
        deleteAllCells()
        insertTickets(tickets)
        insertSheets(sheets)
        insertCells(cells)
    }

    @Query("DELETE FROM rice_tickets")
    suspend fun deleteAllTickets()

    @Query("DELETE FROM rice_sheets")
    suspend fun deleteAllSheets()

    @Query("DELETE FROM rice_cells")
    suspend fun deleteAllCells()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTickets(tickets: List<RiceTicket>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSheets(sheets: List<RiceSheet>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCells(cells: List<RiceCell>)
}
