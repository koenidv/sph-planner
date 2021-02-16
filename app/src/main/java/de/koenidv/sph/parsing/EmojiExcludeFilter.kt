package de.koenidv.sph.parsing

import android.text.InputFilter
import android.text.Spanned

//  Created by koenidv on 15.02.2021.
/**
 * SPH's messages do not support emoji, therefore we need to exclude them
 */
class EmojiExcludeFilter : InputFilter {
    override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int): CharSequence? {
        for (i in start until end) {
            val type = Character.getType(source[i])
            // If this is an emoji character, replace it or remove it
            if (type == Character.SURROGATE.toInt() || type == Character.OTHER_SYMBOL.toInt()) {
                return when (source) {
                    "\uD83D\uDE02" -> "xD"
                    "\uD83D\uDE42" -> ":)"
                    "\uD83D\uDE00", "\uD83D\uDE03", "\uD83D\uDE04", "\uD83D\uDE01" -> ":D"
                    "\uD83D\uDE10" -> ":|"
                    "\uD83D\uDE11" -> "-_-"
                    "\uD83D\uDE06" -> ">.<"
                    "\uD83D\uDE41", "\uD83D\uDE15" -> ":("
                    "☹️", "\uD83D\uDE1F" -> "D:"
                    else -> ""
                }
            }
        }
        // Return null if char should not be filtered
        return null
    }
}