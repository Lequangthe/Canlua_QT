package com.quangthe.canluav3.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quangthe.canluav3.data.*
import com.google.gson.Gson
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale

data class MultiTicketTotals(
    val totalWeight: Double = 0.0,
    val totalBags: Int = 0,
    val totalRiceValue: Long = 0L,
    val totalBalance: Long = 0L
)

class RiceViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {
    private val repository: RiceRepository
    val activeTickets: StateFlow<List<RiceTicket>>
    val deletedTickets: StateFlow<List<RiceTicket>>
    val appSettings: StateFlow<AppSettings>

    private val _selectedTicket = MutableStateFlow<RiceTicket?>(null)
    val selectedTicket: StateFlow<RiceTicket?> = _selectedTicket

    private val _sheets = MutableStateFlow<List<RiceSheet>>(emptyList())
    val sheets: StateFlow<List<RiceSheet>> = _sheets

    private val _currentSheetIndex = MutableStateFlow(0)
    val currentSheetIndex: StateFlow<Int> = _currentSheetIndex

    private val _currentCells = MutableStateFlow<List<RiceCell>>(emptyList())
    val currentCells: StateFlow<List<RiceCell>> = _currentCells

    private val _lastFocusPosition = mutableStateMapOf<Int, Pair<Int, Int>>()

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting
    private val _isBackingUp = MutableStateFlow(false)
    val isBackingUp: StateFlow<Boolean> = _isBackingUp
    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring

    fun setLastFocusPosition(sheetIndex: Int, row: Int, col: Int) {
        _lastFocusPosition[sheetIndex] = Pair(row, col)
    }

    fun getLastFocusPosition(sheetIndex: Int): Pair<Int, Int> {
        return _lastFocusPosition[sheetIndex] ?: Pair(0, 0)
    }

    private val _totalWeight = MutableStateFlow(0.0)
    val totalWeight: StateFlow<Double> = _totalWeight

    private var ticketJob: Job? = null
    private var cellsJob: Job? = null
    private var totalWeightJob: Job? = null
    private val gson = Gson()

    init {
        val dao = RiceDatabase.getDatabase(application).riceDao()
        repository = RiceRepository(dao)
        activeTickets = repository.activeTickets.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        deletedTickets = repository.deletedTickets.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        appSettings = repository.appSettings.map { it ?: AppSettings() }
            .stateIn(viewModelScope, SharingStarted.Lazily, AppSettings())

        tts = TextToSpeech(application, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            ttsReady = true
            tts?.apply {
                language = Locale("vi", "VN")
                setSpeechRate(appSettings.value.ttsSpeechRate)
                setPitch(appSettings.value.ttsPitch)
            }
        }
    }

    fun speak(text: String) {
        if (!ttsReady) return
        if (appSettings.value.speakOnCellComplete || appSettings.value.speakOnColumnComplete) {
            tts?.speak(text, TextToSpeech.QUEUE_ADD, null, null)
        }
    }

    fun speakNumber(number: Double) {
        if (!ttsReady) return
        if (!appSettings.value.speakOnCellComplete) return
        
        val text = convertNumberToVietnamese(number)
        tts?.apply {
            setSpeechRate(appSettings.value.ttsSpeechRate)
            setPitch(appSettings.value.ttsPitch)
            speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun convertNumberToVietnamese(number: Double): String {
        val longVal = number.toLong()
        val symbols = java.text.DecimalFormatSymbols(java.util.Locale.US)
        val df = java.text.DecimalFormat("0.00", symbols)
        val formatted = df.format(number - longVal)
        val decimalStr = formatted.substringAfter(".")

        var result = readLong(longVal)
        
        val dVal = decimalStr.toInt()
        if (dVal > 0) {
            result += " phẩy "
            if (decimalStr.startsWith("0")) {
                result += "không "
                result += readLong(dVal.toLong())
            } else {
                if (decimalStr.endsWith("0")) {
                    result += readLong((dVal / 10).toLong())
                } else {
                    result += readLong(dVal.toLong())
                }
            }
        }
        return result
    }

    private fun readLong(number: Long): String {
        if (number == 0L) return "không"
        val units = arrayOf("", "nghìn", "triệu", "tỷ")
        var n = number
        var i = 0
        var res = ""
        while (n > 0) {
            val chunk = (n % 1000).toInt()
            if (chunk > 0) {
                val s = readThreeDigits(chunk, n > 999)
                res = "$s ${units[i]} $res"
            }
            n /= 1000
            i++
        }
        return res.trim()
    }

    private fun readThreeDigits(n: Int, hasHigher: Boolean): String {
        val digits = arrayOf("không", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín")
        val h = n / 100
        val t = (n % 100) / 10
        val u = n % 10
        var res = ""
        
        if (h > 0 || hasHigher) {
            res += digits[h] + " trăm "
        }
        
        if (t > 1) {
            res += digits[t] + " mươi "
            if (u == 1) res += "mốt"
            else if (u == 5) res += "lăm"
            else if (u > 0) res += digits[u]
        } else if (t == 1) {
            res += "mười "
            if (u == 5) res += "lăm"
            else if (u > 0) res += digits[u]
        } else {
            if (u > 0) {
                if (h > 0 || hasHigher) res += "lẻ "
                res += digits[u]
            }
        }
        return res
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun selectTicket(ticketId: Int) {
        _lastFocusPosition.clear()
        ticketJob?.cancel()
        totalWeightJob?.cancel()
        cellsJob?.cancel()
        
        viewModelScope.launch {
            val ticket = repository.getTicketById(ticketId)
            _selectedTicket.value = ticket
            if (ticket != null) {
                _currentSheetIndex.value = 0
                ticketJob = launch {
                    repository.getSheetsForTicket(ticketId).collectLatest { sheetList ->
                        _sheets.value = sheetList
                        if (sheetList.isEmpty()) {
                            createNewSheet(ticketId, 0)
                        } else {
                            if (_currentSheetIndex.value >= sheetList.size) {
                                _currentSheetIndex.value = sheetList.size - 1
                            }
                            observeCells(sheetList[_currentSheetIndex.value].id)
                        }
                    }
                }
                totalWeightJob = launch {
                    repository.getSheetsForTicket(ticketId).flatMapLatest { sheets ->
                        val flows = sheets.map { repository.getCellsForSheet(it.id) }
                        if (flows.isEmpty()) flowOf(emptyList<RiceCell>())
                        else combine(flows) { it.flatMap { cells -> cells.toList() } }
                    }.collect { allCells ->
                        _totalWeight.value = allCells.sumOf { it.value }
                    }
                }
            }
        }
    }

    private fun observeCells(sheetId: Int) {
        cellsJob?.cancel()
        cellsJob = viewModelScope.launch {
            repository.getCellsForSheet(sheetId).collect {
                _currentCells.value = it
            }
        }
    }

    fun setCurrentSheet(index: Int) {
        if (index >= 0 && index < _sheets.value.size) {
            _currentSheetIndex.value = index
            observeCells(_sheets.value[index].id)
        }
    }

    private suspend fun createNewSheet(ticketId: Int, index: Int): RiceSheet {
        val prevSheet = _sheets.value.lastOrNull()
        val numCols = prevSheet?.numCols ?: appSettings.value.defaultNumCols
        val numRows = prevSheet?.numRows ?: appSettings.value.defaultNumRows
        val colTitles = prevSheet?.colTitles ?: gson.toJson(List(numCols) { "C${it + 1}" })

        val newSheet = RiceSheet(
            ticketId = ticketId,
            sheetIndex = index,
            numCols = numCols,
            numRows = numRows,
            colTitles = colTitles
        )
        val id = repository.insertSheet(newSheet).toInt()
        return newSheet.copy(id = id)
    }

    private var isCreatingSheet = false

    fun autoCreateNextSheet() {
        if (isCreatingSheet) return
        isCreatingSheet = true
        val ticket = _selectedTicket.value ?: run { 
            isCreatingSheet = false
            return 
        }
        val nextIndex = _sheets.value.size
        viewModelScope.launch {
            try {
                val newSheet = createNewSheet(ticket.id, nextIndex)
                repository.getSheetsForTicket(ticket.id).take(1).collect { sheetList ->
                    _sheets.value = sheetList
                    _currentSheetIndex.value = nextIndex
                    observeCells(newSheet.id)
                }
            } finally {
                isCreatingSheet = false
            }
        }
    }

    fun createNewTicket(name: String) {
        viewModelScope.launch {
            val newTicket = RiceTicket(ticketName = name)
            val id = repository.insertTicket(newTicket).toInt()
            selectTicket(id)
        }
    }

    fun updateTicket(ticket: RiceTicket) {
        viewModelScope.launch {
            repository.updateTicket(ticket)
            _selectedTicket.value = ticket
        }
    }

    fun getCellsForSheet(sheetId: Int): Flow<List<RiceCell>> {
        return repository.getCellsForSheet(sheetId)
    }

    fun updateTableCellInSheet(sheetId: Int, rowIndex: Int, colIndex: Int, value: Double) {
        viewModelScope.launch {
            repository.insertCell(RiceCell(sheetId = sheetId, rowIndex = rowIndex, colIndex = colIndex, value = value))
        }
    }

    fun updateTableCell(rowIndex: Int, colIndex: Int, value: Double) {
        val currentSheet = _sheets.value.getOrNull(_currentSheetIndex.value) ?: return
        viewModelScope.launch {
            repository.insertCell(RiceCell(sheetId = currentSheet.id, rowIndex = rowIndex, colIndex = colIndex, value = value))
        }
    }

    fun softDeleteTicket(id: Int) {
        viewModelScope.launch {
            repository.softDeleteTicket(id)
        }
    }

    fun restoreTicket(id: Int) {
        viewModelScope.launch {
            repository.restoreTicket(id)
        }
    }

    fun permanentDeleteTicket(id: Int) {
        viewModelScope.launch {
            if (_selectedTicket.value?.id == id) {
                ticketJob?.cancel()
                totalWeightJob?.cancel()
                cellsJob?.cancel()
                _selectedTicket.value = null
                _sheets.value = emptyList()
                _currentCells.value = emptyList()
                _totalWeight.value = 0.0
            }
            repository.permanentDeleteTicket(id)
        }
    }

    fun exportMultipleTicketsToExcel(context: Context, ticketIds: List<Int>) {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val allData = ticketIds.mapNotNull { id ->
                    val ticket = repository.getTicketById(id) ?: return@mapNotNull null
                    val sheets = repository.getSheetsForTicket(id).first()
                    val cells = getAllCellsForTicket(id).first()
                    Triple(ticket, sheets, cells)
                }
                
                if (allData.isNotEmpty()) {
                    com.quangthe.canluav3.utils.ExportUtils.exportMultipleToExcel(context, allData)
                }
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun updateAppSettings(settings: AppSettings) {
        viewModelScope.launch {
            repository.updateAppSettings(settings)
        }
    }

    fun backupData(context: Context) {
        viewModelScope.launch {
            _isBackingUp.value = true
            try {
                val tickets = repository.getAllTickets()
                val sheets = repository.getAllSheets()
                val cells = repository.getAllCells()
                com.quangthe.canluav3.utils.ExportUtils.exportDatabaseToCsv(context, tickets, sheets, cells)
            } finally {
                _isBackingUp.value = false
            }
        }
    }

    fun restoreData(csvUri: android.net.Uri, context: Context) {
        viewModelScope.launch {
            _isRestoring.value = true
            try {
                val inputStream = context.contentResolver.openInputStream(csvUri)
                val reader = inputStream?.bufferedReader()
                val content = reader?.readText() ?: return@launch
                
                val lines = content.lines()
                val tickets = mutableListOf<RiceTicket>()
                val sheets = mutableListOf<RiceSheet>()
                val cells = mutableListOf<RiceCell>()
                
                var currentSection = ""
                
                lines.forEach { line ->
                    if (line.startsWith("--- TICKETS ---")) { currentSection = "TICKETS"; return@forEach }
                    if (line.startsWith("--- SHEETS ---")) { currentSection = "SHEETS"; return@forEach }
                    if (line.startsWith("--- CELLS ---")) { currentSection = "CELLS"; return@forEach }
                    if (line.isBlank() || line.startsWith("id,") || line.startsWith("sheetId,")) return@forEach
                    
                    try {
                        when (currentSection) {
                            "TICKETS" -> {
                                val parts = line.split(",")
                                if (parts.size >= 8) {
                                    tickets.add(RiceTicket(
                                        id = parts[0].toInt(),
                                        ticketName = parts[1].removeSurrounding("\""),
                                        tarePerBag = parts[2].toInt(),
                                        impurityPerTon = parts[3].toInt(),
                                        unitPrice = parts[4].toInt(),
                                        deposit = parts[5].toLong(),
                                        isDeleted = parts[6].toBoolean(),
                                        createdAt = parts[7].toLong()
                                    ))
                                }
                            }
                            "SHEETS" -> {
                                val parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())
                                if (parts.size >= 6) {
                                    sheets.add(RiceSheet(
                                        id = parts[0].toInt(),
                                        ticketId = parts[1].toInt(),
                                        sheetIndex = parts[2].toInt(),
                                        numRows = parts[3].toInt(),
                                        numCols = parts[4].toInt(),
                                        colTitles = parts[5].removeSurrounding("\"").replace("\"\"", "\"")
                                    ))
                                }
                            }
                            "CELLS" -> {
                                val parts = line.split(",")
                                if (parts.size >= 4) {
                                    cells.add(RiceCell(
                                        sheetId = parts[0].toInt(),
                                        rowIndex = parts[1].toInt(),
                                        colIndex = parts[2].toInt(),
                                        value = parts[3].toDouble()
                                    ))
                                }
                            }
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
                
                if (tickets.isNotEmpty()) {
                    repository.clearAndRestore(tickets, sheets, cells)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isRestoring.value = false
            }
        }
    }

    fun exportTicketToExcel(context: Context, ticketId: Int) {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val ticket = repository.getTicketById(ticketId) ?: return@launch
                val sheets = repository.getSheetsForTicket(ticketId).first()
                val allCells = getAllCellsForTicket(ticketId).first()
                
                val totalWeight = allCells.sumOf { it.value }
                val totalBags = allCells.count { it.value > 0 }
                val totalTare = if (ticket.tarePerBag > 0) totalBags.toDouble() / ticket.tarePerBag else 0.0
                val totalImpurity = (totalWeight / 1000.0) * ticket.impurityPerTon
                val netWeight = totalWeight - totalTare - totalImpurity
                val totalPrice = Math.round(netWeight * ticket.unitPrice)

                com.quangthe.canluav3.utils.ExportUtils.exportToExcel(
                    context, ticket, sheets, allCells, totalWeight, totalPrice
                )
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun exportTicketToExcelFixed(
        context: Context,
        ticket: RiceTicket,
        sheets: List<RiceSheet>,
        allCells: List<RiceCell>,
        totalWeight: Double,
        totalPrice: Long
    ) {
        com.quangthe.canluav3.utils.ExportUtils.exportToExcel(
            context, ticket, sheets, allCells, totalWeight, totalPrice
        )
    }

    fun updateTicketSheetConfig(numRows: Int, numCols: Int) {
        val ticket = _selectedTicket.value ?: return
        viewModelScope.launch {
            repository.deleteSheetsForTicket(ticket.id)
            val colTitles = gson.toJson(List(numCols) { "C${it + 1}" })
            val firstSheet = RiceSheet(
                ticketId = ticket.id,
                sheetIndex = 0,
                numCols = numCols,
                numRows = numRows,
                colTitles = colTitles
            )
            repository.insertSheet(firstSheet)
            _currentSheetIndex.value = 0
            _lastFocusPosition.clear()
        }
    }

    fun triggerVibration() {
        if (!appSettings.value.vibrateOnColumnComplete) return
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getApplication<Application>().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(100)
        }
    }

    fun getAllCellsForTicket(ticketId: Int): Flow<List<RiceCell>> {
        return repository.getSheetsForTicket(ticketId).flatMapLatest { sheets ->
            if (sheets.isEmpty()) flowOf(emptyList())
            else {
                val cellFlows = sheets.map { repository.getCellsForSheet(it.id) }
                combine(cellFlows) { arrays -> arrays.flatMap { it } }
            }
        }
    }

    fun getTotalsForTickets(ticketIds: List<Int>): Flow<MultiTicketTotals> {
        if (ticketIds.isEmpty()) return flowOf(MultiTicketTotals())
        
        return activeTickets.flatMapLatest { allTickets ->
            val flows = ticketIds.map { id ->
                val ticket = allTickets.find { it.id == id }
                if (ticket == null) {
                    flowOf(MultiTicketTotals())
                } else {
                    getAllCellsForTicket(id).map { cells ->
                        val totalWeight = cells.sumOf { it.value }
                        val totalBags = cells.count { it.value > 0 }
                        val totalTare = if (ticket.tarePerBag > 0) totalBags.toDouble() / ticket.tarePerBag else 0.0
                        val totalImpurity = (totalWeight / 1000.0) * ticket.impurityPerTon
                        val remainingWeight = totalWeight - totalTare - totalImpurity
                        val riceValue = Math.round(remainingWeight * ticket.unitPrice)
                        val balance = riceValue - ticket.deposit
                        
                        MultiTicketTotals(
                            totalWeight = totalWeight,
                            totalBags = totalBags,
                            totalRiceValue = riceValue,
                            totalBalance = balance
                        )
                    }
                }
            }
            
            combine(flows) { array ->
                var tw = 0.0
                var tb = 0
                var trv = 0L
                var tbal = 0L
                array.forEach {
                    tw += it.totalWeight
                    tb += it.totalBags
                    trv += it.totalRiceValue
                    tbal += it.totalBalance
                }
                MultiTicketTotals(tw, tb, trv, tbal)
            }
        }
    }
}
