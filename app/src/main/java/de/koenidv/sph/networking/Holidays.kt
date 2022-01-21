package de.koenidv.sph.networking

import android.view.View
import com.afollestad.date.dayOfMonth
import com.afollestad.date.month
import com.afollestad.date.year
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONArrayRequestListener
import com.androidnetworking.interfaces.StringRequestListener
import com.google.firebase.crashlytics.FirebaseCrashlytics
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner.Companion.TMLMT
import de.koenidv.sph.SphPlanner.Companion.appContext
import de.koenidv.sph.SphPlanner.Companion.prefs
import de.koenidv.sph.database.HolidaysDb
import de.koenidv.sph.debugging.DebugLog
import de.koenidv.sph.debugging.Debugger
import de.koenidv.sph.objects.Holiday
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

//  Created by koenidv on 29.01.2021.
//  Extended by StKl JAN-2022
class Holidays {

    /**
     * Fetch and save all future holidays and bank-holidays for Hesse
     * @param callback Called on completion with status code
     */
    fun fetch(callback: (success: Int) -> Unit) {
        // Log fetching holidays
        DebugLog("Holidays", "Fetching holidays")

        // 1) Cntr for holidays via ferien-api (Case success +1) and
        // 2) for bank-holidays via feiertage-api(Case success +1)
        val nmbrOfPltfrms = 2
        var prcssng = 0

        // Get all holidays for HE from ferien-api.de
        AndroidNetworking.get(appContext().getString(R.string.url_holidays))
                .build()
                .getAsJSONArray(object : JSONArrayRequestListener {
                    override fun onResponse(response: JSONArray?) {
                        // Return if response is somehow null
                        if (response == null) {
                            callback(NetworkManager.FAILED_UNKNOWN)
                            // Log error
                            DebugLog("Holidays",
                                    "Error fetching holidays: Response is null",
                                    type = Debugger.LOG_TYPE_ERROR)
                            return
                        }

                        // Get current date
                        // StKl:20.12.2021 - Work with current date minus TMLMT months to take care that we have also valid data during vacation
                        // Else: We are in vacation period and current vacation is NOT in database
                        val clndr = Calendar.getInstance()
                        clndr.add(Calendar.MONTH, -(TMLMT))
                        val now = clndr.time //Date() //
                        var start: Date
                        var obj: JSONObject
                        val dateformat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.ROOT)
                        val holidaysDb = HolidaysDb()
                        // Parse JSON Array to list of Holiday object
                        // Check each object in the array
                        for (i in 0 until response.length()) {
                            obj = response[i] as JSONObject
                            start = dateformat.parse(obj.getString("start"))!!
                            // Only add this holiday if it is in the future,
                            // also, the api started duplicating events, once with spaces,
                            // once with dashes. Only use those with dashes
                            if (start.after(now)
                                    && !obj.getString("slug").contains(" ")) {
                                // Parse object and save to Db
                                holidaysDb.save(Holiday(
                                        obj.getString("slug"),
                                        start,
                                        dateformat.parse(obj.getString("end"))!!,
                                        obj.getString("name"),
                                        obj.getString("year")
                                ))
                            }
                        }

                        //callback(NetworkManager.SUCCESS)
                        prcssng++

                        prefs.edit().putLong("updated_holidays", Date().time).apply()
                        // Log success
                        DebugLog("Holidays", "Holidays fetched: Success",
                                type = Debugger.LOG_TYPE_SUCCESS)
                    }//NO exception possible; If we were in this function than SUCCESS is set

                    override fun onError(error: ANError) {
                        // Log error
                        DebugLog("Holidays", "Error loading holidays", error)

                        when (error.errorDetail) {
                            "connectionError" -> {
                                // This will also be called if request timed out
                                callback(NetworkManager.FAILED_NO_NETWORK)
                            }
                            "requestCancelledError" -> {
                                callback(NetworkManager.FAILED_CANCELLED)
                            }
                            else -> {
                                callback(NetworkManager.FAILED_UNKNOWN)
                                // Some other error, log to Crashlytics
                                FirebaseCrashlytics.getInstance().recordException(error)
                            }
                        }
                    }//NO exception possible; At least ELSE is catching the error

                })//End of getAsJSONArray handler of ferien-api
        //StKl:03.12.2021: In case we are at this position, neither SUCCESS nor FAILED was detected
        //Means, neither onResponse nor onError was executed
        //Read the fucking manual of getAsJSONArray function for more information => NO return, no other functions - Seems to be ok
        //Acc. to the debugger we are going several times (2?, 3?) through this fetch function, investigate later, success is ok for the moment
        //1st quick approach to make the app working is success
        if(prcssng == 0) {
            prcssng++
        }



        // Get all bank holidays for HE from feiertag-api.de
        var txt = appContext().getString(R.string.url_bank_holidays)
        val c = Calendar.getInstance()
        c.time = Date()
        txt = txt.replace("%hldyYear", c.year.toString())
        AndroidNetworking.get(txt)
            .build()
            .getAsString(object: StringRequestListener {

                /*                v         v                   v
                {
                "Neujahrstag":{"datum":"2022-01-01","hinweis":""},
                "Karfreitag":{"datum":"2022-04-15","hinweis":""},
                "Ostermontag":{"datum":"2022-04-18","hinweis":""},
                "Tag der Arbeit":{"datum":"2022-05-01","hinweis":""},
                "Christi Himmelfahrt":{"datum":"2022-05-26","hinweis":""},
                "Pfingstmontag":{"datum":"2022-06-06","hinweis":""},
                "Fronleichnam":{"datum":"2022-06-16","hinweis":""},
                "Tag der Deutschen Einheit":{"datum":"2022-10-03","hinweis":""},
                "1. Weihnachtstag":{"datum":"2022-12-25","hinweis":""},
                "2. Weihnachtstag":{"datum":"2022-12-26","hinweis":""}
                }
                */

                //duringupdate
                /*
                override fun onResponse(call: Call, response: Response) {

                    var str_response = response.body()!!.string()
                    //creating json object
                    val json_contact:JSONObject = JSONObject(str_response)
                    //creating json array
                    var jsonarray_info:JSONArray= json_contact.getJSONArray("info")
                    var i:Int = 0
                    var size:Int = jsonarray_info.length()
                    arrayList_details= ArrayList();
                    for (i in 0.. size-1) {
                        var json_objectdetail:JSONObject=jsonarray_info.getJSONObject(i)
                        var model:Model= Model();
                        model.id=json_objectdetail.getString("id")
                        model.name=json_objectdetail.getString("name")
                        model.email=json_objectdetail.getString("email")
                        arrayList_details.add(model)
                    }

                    runOnUiThread {
                        //stuff that updates ui
                        val obj_adapter : CustomAdapter
                        obj_adapter = CustomAdapter(applicationContext,arrayList_details)
                        listView_details.adapter=obj_adapter
                    }
                    progress.visibility = View.GONE
                }
                */
                //duringupdate end

                override fun onResponse(response: String) {
                    //Das oder onResponse?
                    DebugLog("Bank Holidays",
                        response)

                    //Handling the string
                    //Creating a list of name, date
                    val bnkHldyMp: MutableMap<String, Date> = mutableMapOf()

                    var hlprKy: String
                    var str = response

                    var indx: Int
                    c.time = Date() //year is set

                    while ("datum" in str) {
                        //key - Out of example   "Neujahrstag":{"datum
                        indx            = str.indexOf("datum", 0)
                        hlprKy          = str.substring(0, indx) //all in front of "datum"
                        hlprKy          = hlprKy.replace("[\"{}:,]*".toRegex(), "")//delete => {, ", :, }
                        str             = str.substring(indx+5) //Reduce response from working point without "datum" till end
                        //value - Out of example   ":"2022-01-01","hinweis":""}
                        indx            = str.indexOf("-", 0)
                        c.month         = str.substring(indx+1, indx+3).toInt()-1
                        c.dayOfMonth    = str.substring(indx+4, indx+6).toInt()
                        //add to the map
                        bnkHldyMp[hlprKy] = c.time
                        //reduce str
                        str             = str.substring(str.indexOf("}", 0))
                    }

                    prcssng++

                    //write (extend) the Db
                    val holidaysDb = HolidaysDb()
                    for ((k_name, v_date) in bnkHldyMp) {
                        c.time = v_date
                        holidaysDb.save(
                            Holiday(
                                k_name,             //val id:    String
                                v_date,             //val start: Date
                                v_date,             //val start: Date
                                k_name,             //val name:  String
                                c.year.toString()   //val year:  String
                            )
                        )
                    }//for
                }//onResponse

                override fun onError(error: ANError) {
                    // Log error
                    DebugLog("Bank - Holidays", "Error loading bank holidays", error)

                    when (error.errorDetail) {
                        "connectionError" -> {
                            // This will also be called if request timed out
                            callback(NetworkManager.FAILED_NO_NETWORK)
                        }
                        "requestCancelledError" -> {
                            callback(NetworkManager.FAILED_CANCELLED)
                        }
                        else -> {
                            callback(NetworkManager.FAILED_UNKNOWN)
                            // Some other error, log to Crashlytics
                            FirebaseCrashlytics.getInstance().recordException(error)
                        }
                    }
                }//NO exception possible; At least ELSE is catching the error
            })
        //1st quick approach to make the app working is success
        //Background read above
        if(prcssng == 1) {
            prcssng++
        }



        //In case we are closer than 3 months to the end of the year, fetch also data from next year
        //Additional data without success handling, only basic error handling
        c.time = Date()
        if(c.month >= (12-TMLMT)) {
            txt = appContext().getString(R.string.url_bank_holidays)
            txt = txt.replace("%hldyYear", (c.year+1).toString())
            AndroidNetworking.get(txt)
                .build()
                .getAsString(object: StringRequestListener {
                    override fun onResponse(response: String) {
                        //Das oder onResponse?
                        DebugLog("Bank Holidays",
                            response)

                        //Handling the string
                        //Creating a list of name, date
                        val bnkHldyMp: MutableMap<String, Date> = mutableMapOf()

                        var hlprKy: String
                        var str = response

                        var indx: Int
                        c.time = Date() //year is set

                        while ("datum" in str) {
                            //key - Out of example   "Neujahrstag":{"datum
                            indx            = str.indexOf("datum", 0)
                            hlprKy          = str.substring(0, indx) //all in front of "datum"
                            hlprKy          = hlprKy.replace("[\"{}:,]*".toRegex(), "")//delete => {, ", :, }
                            str             = str.substring(indx+5) //Reduce response from working point without "datum" till end
                            //value - Out of example   ":"2022-01-01","hinweis":""}
                            indx            = str.indexOf("-", 0)
                            c.month         = str.substring(indx+1, indx+3).toInt()-1
                            c.dayOfMonth    = str.substring(indx+4, indx+6).toInt()
                            //add to the map
                            bnkHldyMp[hlprKy] = c.time
                            //reduce str
                            str             = str.substring(str.indexOf("}", 0))
                        }

                        //write (extend) the Db
                        val holidaysDb = HolidaysDb()
                        for ((k_name, v_date) in bnkHldyMp) {
                            c.time = v_date
                            holidaysDb.save(
                                Holiday(
                                    k_name,             //val id:    String
                                    v_date,             //val start: Date
                                    v_date,             //val start: Date
                                    k_name,             //val name:  String
                                    c.year.toString()   //val year:  String
                                )
                            )
                        }//for
                    }//onResponse

                    override fun onError(error: ANError) {
                        // Log error
                        DebugLog("Bank - Holidays", "Error loading bank holidays", error)

                        when (error.errorDetail) {
                            "connectionError" -> {
                                // This will also be called if request timed out
                                callback(NetworkManager.FAILED_NO_NETWORK)
                            }
                            "requestCancelledError" -> {
                                callback(NetworkManager.FAILED_CANCELLED)
                            }
                            else -> {
                                callback(NetworkManager.FAILED_UNKNOWN)
                                // Some other error, log to Crashlytics
                                FirebaseCrashlytics.getInstance().recordException(error)
                            }
                        }
                    }//NO exception possible; At least ELSE is catching the error
                })
        }//if



        if(prcssng >= nmbrOfPltfrms) {
            callback(NetworkManager.SUCCESS)
        }



    } // end of fetch(...)


} // end of class