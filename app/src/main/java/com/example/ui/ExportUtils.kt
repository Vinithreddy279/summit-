package com.example.ui

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.Activity
import com.example.data.GPSPoint
import com.example.data.JsonHelper
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ExportUtils {

    fun exportGPX(context: Context, activity: Activity) {
        try {
            val points = JsonHelper.jsonToPoints(activity.routePointsJson)
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            val gpxBuilder = StringBuilder()
            gpxBuilder.append("""<?xml version="1.0" encoding="UTF-8"?>
<gpx version="1.1" creator="SummitApp" xmlns="http://www.topografix.com/GPX/1/1">
  <metadata>
    <name>${escapeXml(activity.sportType)} Activity - ${activity.id}</name>
    <desc>${escapeXml(activity.notes)}</desc>
    <time>${sdf.format(Date(activity.timestamp))}</time>
  </metadata>
  <trk>
    <name>${escapeXml(activity.sportType)}</name>
    <type>${activity.sportType.uppercase()}</type>
    <trkseg>
""")

            for (p in points) {
                gpxBuilder.append("""      <trkpt lat="${p.lat}" lon="${p.lng}">
        <ele>${p.elevation}</ele>
        <time>${sdf.format(Date(p.timeMs))}</time>
      </trkpt>
""")
            }

            gpxBuilder.append("""    </trkseg>
  </trk>
</gpx>""")

            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val sanitizedTitle = activity.sportType.lowercase().replace(" ", "_")
            val file = File(exportDir, "summit_${sanitizedTitle}_${activity.id}.gpx")
            FileOutputStream(file).use {
                it.write(gpxBuilder.toString().toByteArray())
            }

            shareFile(context, file, "application/gpx+xml", "Export GPX")

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun exportCSV(context: Context, activities: List<Activity>) {
        try {
            val csvBuilder = StringBuilder()
            csvBuilder.append("ID,Sport Type,Timestamp,Duration (s),Distance (km),Elevation Gain (m),Kudos,Privacy,Notes\n")
            
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

            for (act in activities) {
                val notesEscaped = act.notes.replace("\"", "\"\"")
                csvBuilder.append("${act.id},${act.sportType},\"${sdf.format(Date(act.timestamp))}\",${act.durationSeconds},${act.distanceKm},${act.elevationGainM},${act.kudosCount},${act.privacy},\"$notesEscaped\"\n")
            }

            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val file = File(exportDir, "summit_activities_history.csv")
            FileOutputStream(file).use {
                it.write(csvBuilder.toString().toByteArray())
            }

            shareFile(context, file, "text/csv", "Export CSV")

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun backupDatabase(context: Context) {
        try {
            val dbFile = context.getDatabasePath("summit_database")
            if (!dbFile.exists()) return

            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val backupFile = File(exportDir, "summit_database_backup.db")
            dbFile.inputStream().use { input ->
                backupFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            shareFile(context, backupFile, "application/x-sqlite3", "Backup Summit Database")

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(context, "com.example.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    fun exportSingleCSV(context: Context, activity: Activity) {
        try {
            val csvBuilder = StringBuilder()
            csvBuilder.append("ID,Sport Type,Timestamp,Duration (s),Distance (km),Elevation Gain (m),Kudos,Privacy,Notes\n")
            
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val notesEscaped = activity.notes.replace("\"", "\"\"")
            csvBuilder.append("${activity.id},${activity.sportType},\"${sdf.format(Date(activity.timestamp))}\",${activity.durationSeconds},${activity.distanceKm},${activity.elevationGainM},${activity.kudosCount},${activity.privacy},\"$notesEscaped\"\n")

            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val file = File(exportDir, "summit_activity_${activity.id}.csv")
            FileOutputStream(file).use {
                it.write(csvBuilder.toString().toByteArray())
            }

            shareFile(context, file, "text/csv", "Export CSV")

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun exportSummaryImage(context: Context, activity: Activity) {
        try {
            val width = 1080
            val height = 1080
            val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            
            // Background - Premium Dark Gradient Slate (#121824 to #1A2436)
            val bgPaint = android.graphics.Paint()
            bgPaint.shader = android.graphics.LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                android.graphics.Color.parseColor("#121824"),
                android.graphics.Color.parseColor("#1C263B"),
                android.graphics.Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
            
            // Draw a subtle orange glowing ring at center/top
            val glowPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 3f
                color = android.graphics.Color.parseColor("#4DFF5722") // Transparent Summit Orange
            }
            canvas.drawCircle(width / 2f, 150f, 100f, glowPaint)

            // Header "SUMMIT WORKOUT SUMMARY"
            val textPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.WHITE
                textSize = 40f
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            }
            canvas.drawText("SUMMIT WORKOUT", width / 2f, 120f, textPaint)
            
            // Title text Paint
            val titlePaint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#FF5722") // Summit Orange
                textSize = 50f
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            }
            canvas.drawText(activity.title.uppercase(), width / 2f, 210f, titlePaint)

            // Sport Type Emoji/Label
            val sportEmoji = when (activity.sportType.lowercase()) {
                "ride" -> "🚴 RIDE"
                "run" -> "🏃 RUN"
                "hike" -> "🥾 HIKE"
                "walk" -> "🚶 WALK"
                "swim" -> "🏊 SWIM"
                else -> "💪 WORKOUT"
            }
            val sportPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#A0AEC0")
                textSize = 34f
                textAlign = android.graphics.Paint.Align.CENTER
            }
            val sdf = SimpleDateFormat("MMMM d, yyyy • h:mm a", Locale.US)
            canvas.drawText(sportEmoji, width / 2f, 280f, sportPaint)
            canvas.drawText(sdf.format(Date(activity.timestamp)), width / 2f, 330f, sportPaint)

            // Central stats Card background (#1F293D)
            val cardPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#1F293D")
                style = android.graphics.Paint.Style.FILL
            }
            val cardRect = android.graphics.RectF(100f, 400f, width - 100f, 900f)
            canvas.drawRoundRect(cardRect, 32f, 32f, cardPaint)
            
            // Draw a dividing line inside the card
            val linePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#2D3748")
                strokeWidth = 2f
            }
            canvas.drawLine(width / 2f, 440f, width / 2f, 860f, linePaint)
            canvas.drawLine(140f, 650f, width - 140f, 650f, linePaint)

            // Stats paints
            val labelPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#A0AEC0")
                textSize = 30f
                textAlign = android.graphics.Paint.Align.CENTER
            }
            val valPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.WHITE
                textSize = 54f
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            }

            // Stat 1: Distance
            canvas.drawText("DISTANCE", width / 4f + 25f, 490f, labelPaint)
            val distStr = String.format(Locale.US, "%.2f km", activity.distanceKm)
            canvas.drawText(distStr, width / 4f + 25f, 570f, valPaint)

            // Stat 2: Duration
            canvas.drawText("DURATION", 3f * width / 4f - 25f, 490f, labelPaint)
            val h = activity.durationSeconds / 3600
            val m = (activity.durationSeconds % 3600) / 60
            val s = activity.durationSeconds % 60
            val durStr = if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
            canvas.drawText(durStr, 3f * width / 4f - 25f, 570f, valPaint)

            // Stat 3: Avg Speed
            canvas.drawText("AVG SPEED", width / 4f + 25f, 720f, labelPaint)
            val speedStr = String.format(Locale.US, "%.1f km/h", activity.avgSpeedKmh)
            canvas.drawText(speedStr, width / 4f + 25f, 800f, valPaint)

            // Stat 4: Elevation
            canvas.drawText("ELEVATION", 3f * width / 4f - 25f, 720f, labelPaint)
            val elevStr = String.format(Locale.US, "+%.0f m", activity.elevationGainM)
            canvas.drawText(elevStr, 3f * width / 4f - 25f, 800f, valPaint)

            // Footer branding
            val footerPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#4DFF5722") // Translucent Orange
                textSize = 28f
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            }
            canvas.drawText("POWERED BY SUMMIT TRACKER", width / 2f, 980f, footerPaint)

            // Save and Share
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()
            val file = File(exportDir, "summit_summary_${activity.id}.png")
            FileOutputStream(file).use {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }

            shareFile(context, file, "image/png", "Share Workout Summary Image")

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
