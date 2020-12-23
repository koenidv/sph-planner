package de.koenidv.sph.parsing

import java.lang.Integer.parseInt

//  Created by koenidv on 23.12.2020.
//  Rewrite of some jCryption functions we need
class Cryption {

    private val b64map = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    private val b64padding = "="

    /**
     * Converts a Hex String to a Base64 String
     */
    fun hexToBase64(hexString: String): String {
        var iter = 0
        var char: Int
        var ret = ""
        while (iter + 3 <= hexString.length) {
            char = parseInt(hexString.substring(iter, iter + 3), 16)
            ret += b64map[char shr 6]
            ret += b64map[char and 63]
            iter += 3
        }
        if (iter + 1 == hexString.length) {
            char = parseInt(hexString.substring(iter, iter + 1), 16)
            ret += b64map[char shl 2]
        } else if (iter + 2 == hexString.length) {
            char = parseInt(hexString.substring(iter, iter + 2), 16)
            ret += b64map[char shr 2]
            ret += b64map[(char and 3) shl 4]
        }
        while ((ret.length and 3) > 0) ret += b64padding
        return ret
    }
}