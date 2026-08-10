package com.example.data.remote

import com.example.data.model.DrawRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import kotlin.random.Random

object MacauDataFetcher {

    private const val DEFAULT_URL = "https://macaujc.ddcdn.cloudns.org/"

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /**
     * Fetches real lottery data from web URL or parses raw HTML response.
     */
    suspend fun fetchFromRemoteUrl(url: String = DEFAULT_URL): List<DrawRecord> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) Chrome/120.0.0.0 Mobile Safari/537.36")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val bodyText = response.body?.string() ?: return@withContext emptyList()
                return@withContext parseDrawsFromHtmlOrText(bodyText)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    /**
     * Parses period and numbers from HTML/Text body.
     */
    fun parseDrawsFromHtmlOrText(text: String): List<DrawRecord> {
        val records = mutableListOf<DrawRecord>()
        try {
            // Match pattern like: 2026222 21:35 05,12,23,31,38,45
            val periodPattern = Pattern.compile("(202[0-9]{3,4})")
            val numberPattern = Pattern.compile("(\\d{1,2})[\\s,，\\-]+(\\d{1,2})[\\s,，\\-]+(\\d{1,2})[\\s,，\\-]+(\\d{1,2})[\\s,，\\-]+(\\d{1,2})[\\s,，\\-]+(\\d{1,2})")

            val periodMatcher = periodPattern.matcher(text)
            val numberMatcher = numberPattern.matcher(text)

            val periods = mutableListOf<String>()
            while (periodMatcher.find()) {
                periods.add(periodMatcher.group(1))
            }

            val sdf = SimpleDateFormat("yyyy-MM-dd 21:35", Locale.getDefault())
            var idx = 0

            while (numberMatcher.find() && idx < periods.size) {
                val p = periods[idx]
                val n1 = numberMatcher.group(1).toIntOrNull() ?: 1
                val n2 = numberMatcher.group(2).toIntOrNull() ?: 2
                val n3 = numberMatcher.group(3).toIntOrNull() ?: 3
                val n4 = numberMatcher.group(4).toIntOrNull() ?: 4
                val n5 = numberMatcher.group(5).toIntOrNull() ?: 5
                val n6 = numberMatcher.group(6).toIntOrNull() ?: 6

                val dateStr = sdf.format(Date(System.currentTimeMillis() - idx * 86400000L))

                records.add(
                    DrawRecord(
                        period = p,
                        dateStr = dateStr,
                        numbers = listOf(n1, n2, n3, n4, n5, n6)
                    )
                )
                idx++
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return records
    }

    /**
     * Generates 100 realistic historical draw records with embedded interval-1 duplicate triggers (隔期同号).
     */
    fun generateInitialHistoricalData(count: Int = 100): List<DrawRecord> {
        val list = mutableListOf<DrawRecord>()
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.AUGUST, 9, 21, 35, 0)
        val sdf = SimpleDateFormat("yyyy-MM-dd 21:35", Locale.getDefault())

        val startPeriod = 2026222L

        val random = Random(42) // Fixed seed for reproducible realistic data

        // Generate base draws
        for (i in 0 until count) {
            val periodStr = (startPeriod - i).toString()
            val dateStr = sdf.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, -1)

            // Generate 6 distinct sorted numbers between 1 and 49
            val numbers = mutableSetOf<Int>()
            while (numbers.size < 6) {
                numbers.add(random.nextInt(1, 50))
            }

            list.add(
                DrawRecord(
                    period = periodStr,
                    dateStr = dateStr,
                    numbers = numbers.toList()
                )
            )
        }

        // Sort oldest to newest to inject triggers safely
        val sortedAsc = list.sortedBy { it.period }.toMutableList()

        // Inject "隔期同号" (Interval-1 duplicate numbers) every 3-5 draws
        for (idx in 2 until sortedAsc.size step 3) {
            val drawNMinus2 = sortedAsc[idx - 2]
            val drawN = sortedAsc[idx]

            val targetPos = random.nextInt(0, 6)
            val repeatedNum = drawNMinus2.numbers[targetPos]

            val newNumbers = drawN.numbers.toMutableList()
            newNumbers[targetPos] = repeatedNum

            // Ensure numbers stay unique
            for (j in newNumbers.indices) {
                if (j != targetPos && newNumbers[j] == repeatedNum) {
                    newNumbers[j] = (repeatedNum % 49) + 1
                }
            }

            sortedAsc[idx] = drawN.copy(numbers = newNumbers)
        }

        return sortedAsc.sortedByDescending { it.period } // Return newest first
    }
}
