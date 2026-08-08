package com.quangthe.canluav3.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "rice_tickets")
data class RiceTicket(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ticketName: String,
    val tarePerBag: Int = 8, // Mặc định 8 bao / 1kg
    val impurityPerTon: Int = 0,
    val unitPrice: Int = 0,
    val deposit: Long = 0,      // Tiền cọc, ứng (kết hợp)
    val phoneNumber: String = "",
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "rice_sheets",
    foreignKeys = [
        ForeignKey(
            entity = RiceTicket::class,
            parentColumns = ["id"],
            childColumns = ["ticketId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("ticketId")]
)
data class RiceSheet(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ticketId: Int,
    val sheetIndex: Int,        // 0, 1, 2...
    val numCols: Int,
    val numRows: Int,
    val colTitles: String,      // JSON array
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "rice_cells",
    primaryKeys = ["sheetId", "rowIndex", "colIndex"],
    foreignKeys = [
        ForeignKey(
            entity = RiceSheet::class,
            parentColumns = ["id"],
            childColumns = ["sheetId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class RiceCell(
    val sheetId: Int,
    val rowIndex: Int,
    val colIndex: Int,
    val value: Double
)

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val decimalPlaces: Int = 1,
    val maxIntegerDigits: Int = 2,
    val defaultNumCols: Int = 5,
    val defaultNumRows: Int = 5,
    val speakOnCellComplete: Boolean = true,
    val speakOnColumnComplete: Boolean = false,
    val vibrateOnColumnComplete: Boolean = true,
    val autoFocusNext: Boolean = true,
    val ttsSpeechRate: Float = 1.0f,
    val ttsPitch: Float = 1.0f,
    val tableFontSize: Float = 24f,
    val globalFontScale: Float = 1.0f,
    val fontFamilyName: String = "Default"
)


