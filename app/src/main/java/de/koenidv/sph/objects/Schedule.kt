package de.koenidv.sph.objects

import java.util.*

//  Created by StKl JAN-2022
data class Schedule(
        var nme:    String  = "",           //Unique identifier, used for identification purpose, e.g. db handling

        var strt:   Date    = Date(0), //Year, month, day, hour, minute is important
        var nd:     Date    = Date(0), //Counterpart of strt
        var hr:     String  = "",           //School lesson "2#3#", can be more than 1, e.g. <td>2., 3.</td>
        var drtn:   Int     = 0,            //Duration of the entry [min], e.g. 60

        var txt:    String  = "",           //Display text
        var crs:    String  = "none",       //Can this schedule assigned to a course or not

        var ctgr:   String  = "none",       //Other ctgrs to define later
        var src:    String  = "portal",     //Other src could be own
        var shr:    Boolean = false ,       //Only interesting in case  src : own; src := portal will be read-in in every mobile

        var plc:    String  = "",           //Location information
        var rsp:    String  = ""            //Responsible info

        //Later on maybe repeating information
) {


}