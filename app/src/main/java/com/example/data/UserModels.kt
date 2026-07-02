package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.mindrot.jbcrypt.BCrypt
import java.io.Serializable

@Entity(tableName = "users")
data class User(
    @PrimaryKey val email: String,
    val name: String,
    val passwordHash: String,
    val avatar: String = "avatar_you",
    val heightCm: Double = 175.0,
    val weightKg: Double = 70.0,
    val birthday: String = "1990-01-01",
    val gender: String = "Other"
) : Serializable

object HashUtils {
    fun hashPassword(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }

    fun checkPassword(password: String, hashed: String): Boolean {
        return try {
            BCrypt.checkpw(password, hashed)
        } catch (e: Exception) {
            false
        }
    }
}
