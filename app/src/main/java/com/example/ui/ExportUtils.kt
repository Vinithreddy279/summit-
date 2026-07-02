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
}
