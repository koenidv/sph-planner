package de.koenidv.sph.objects

import java.time.LocalDate
import java.time.LocalTime

//  Created by StKL on 06.12.2021.
data class Timebar(
        var lssnSetup: Array<Array<LocalTime>>,
        var lssnValid: LocalDate
) {
        override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Timebar

                if (!lssnSetup.contentDeepEquals(other.lssnSetup)) return false
                if (lssnValid != other.lssnValid) return false

                return true
        }

        override fun hashCode(): Int {
                var result = lssnSetup.contentDeepHashCode()
                result = 31 * result + lssnValid.hashCode()
                return result
        }
}