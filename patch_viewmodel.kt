    val bufferedPredictions: StateFlow<List<BufferedPredictionRecord>> = combine(allDraws, targetPoolSize, predictionResult) { draws, poolSize, predRes ->
        if (draws.isEmpty()) emptyList()
        else {
            val all10 = AnalyzerEngine.generate10PeriodPredictionBuffer(
                draws = draws,
                bufferSize = 10,
                maxPool = poolSize,
                overridePendingNumbers = predRes?.predictedExcludedNumbers
            )
            
            val prefs = getApplication<Application>().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
            var installPeriod = prefs.getString("install_period", null)
            
            if (installPeriod == null) {
                val nextPeriod = draws.maxByOrNull { it.period }?.let { AnalyzerEngine.deriveNextPeriod(it.period) } ?: "2026001"
                installPeriod = nextPeriod
                prefs.edit().putString("install_period", installPeriod).apply()
            }
            
            all10.filter { it.period >= installPeriod }
        }
    }.stateIn(
