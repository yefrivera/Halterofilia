package edu.unicauca.halterfilia_cauca.core.utils

import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object DateUtils {
    fun calculateAge(birthDate: String): Int? {
        return try {
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val parsedBirthDate = LocalDate.parse(birthDate, formatter)
            Period.between(parsedBirthDate, LocalDate.now()).years
        } catch (e: DateTimeParseException) {
            null
        }
    }
}
