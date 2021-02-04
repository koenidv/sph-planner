package de.koenidv.sph.networking

import android.util.Log
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import kotlin.system.measureTimeMillis

//  Created by koenidv on 04.02.2021.
class Cryption {

    fun execute(jsFunction: String, params: Array<Any> = emptyArray()): String? {
        // todo Don't exit if multiple calls are made
        // Get the JavaScript in previous section
        try {
            // Get the js file
            val source = SphPlanner.applicationContext().resources
                    .openRawResource(R.raw.cryption)
                    .bufferedReader().use { it.readText() }

            // Initialize the Rhino VM using enter()
            val rhino: Context = Context.enter()

            // Turn off optimization to make Rhino Android compatible
            rhino.optimizationLevel = -1

            // Initialize standard objects like String in the VM
            val scope: Scriptable = rhino.initStandardObjects()

            // Evaluate text from the js file as code
            rhino.evaluateString(scope, source, "Cryption JS", 1, null)

            // Check if the specified object is actually a function and execute it
            val obj: Any = scope.get(jsFunction, scope)
            return if (obj is org.mozilla.javascript.Function) {
                // Call the function with the params
                val result: Any
                val time = measureTimeMillis {
                    result = obj.call(rhino, scope, scope, params)
                }
                Log.d(SphPlanner.TAG, "Time to execute: $time ms")
                // Return the output
                Context.toString(result)
            } else {
                null
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        } finally {
            // Exit Rhino VM
            Context.exit()
        }
        return null
    }

}