package com.example.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "weather_cache")
data class WeatherCache(
    @PrimaryKey val id: Int = 0,
    val temp: Double,
    val feelsLike: Double,
    val condition: String,
    val weatherCode: Int,
    val humidity: Int,
    val windSpeed: Double,
    val windDirection: Double,
    val chanceOfRain: Int,
    val uvIndex: Double,
    val sunrise: String,
    val sunset: String,
    val lastUpdatedTimeMs: Long,
    val latitude: Double,
    val longitude: Double
)

@Dao
interface WeatherDao {
    @Query("SELECT * FROM weather_cache WHERE id = 0 LIMIT 1")
    fun getWeatherFlow(): Flow<WeatherCache?>

    @Query("SELECT * FROM weather_cache WHERE id = 0 LIMIT 1")
    suspend fun getWeatherDirect(): WeatherCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeather(weather: WeatherCache)

    @Query("DELETE FROM weather_cache")
    suspend fun clearWeather()
}
