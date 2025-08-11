
package edu.unicauca.halterfilia_cauca.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileRepository(private val context: Context) {

    private val directory = context.filesDir

    fun saveMeasurementToFile(jsonData: String): Result<File> {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "medicion_$timestamp.json"
            val file = File(directory, filename)
            file.writeText(jsonData)
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun readLastMeasurementFile(): Result<String> {
        return try {
            val lastModifiedFile = directory.listFiles()
                ?.filter { it.name.startsWith("medicion_") && it.extension == "json" }
                ?.maxByOrNull { it.lastModified() }

            if (lastModifiedFile != null) {
                val fileContent = lastModifiedFile.readText()
                // Pretty print the JSON
                val jsonArray = JSONArray(fileContent)
                Result.success(jsonArray.toString(4))
            } else {
                Result.failure(Exception("No measurement file found."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
