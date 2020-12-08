package de.koenidv.sph.parsing

import android.util.Log
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.objects.Change

//  Created by koenidv on 08.12.2020.
class ChangeParser {

    // todo documentation
    // todo do not return null
    fun parseChangesFromRaw(rawResponse : String) : List<Change>? {

        // todo check if response is valid
        // we could check the week type (a/b) here: <span class="badge woche">
        // todo get last refresh

        // Remove stuff that we don't need
        var rawContent = rawResponse.substring(rawResponse.indexOf("<div class=\"panel panel-primary\""))
        rawContent = rawContent.substring(0, rawContent.indexOf("<link"))
        Log.d(SphPlanner.TAG, "Test")

        // Both starting with "<table class=\"table"

        // For every changes table, i.e. every available day
        var rawToday : String
        while (rawContent.contains("<table class=\"table")) {
            // Get today's table only
            rawToday = rawContent.substring(rawContent.indexOf("<table class=\"table"), rawContent.indexOf("<div class=\"fixed-table-footer"))
        }

        return null
    }

}