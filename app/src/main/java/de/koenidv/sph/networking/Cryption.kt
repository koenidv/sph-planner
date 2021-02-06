package de.koenidv.sph.networking

import android.util.Log
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner.Companion.TAG
import de.koenidv.sph.SphPlanner.Companion.applicationContext
import org.json.JSONObject
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.system.measureTimeMillis

//  Created by koenidv on 04.02.2021.
class Cryption {

    fun authenticate(callback: (key: String?) -> Unit) {
        val secret = generateKey()
        encryptKey(secret) { encryptedKey ->
            Log.d("$TAG js", "encrypted key: $encryptedKey")
            if (encryptedKey != null) {
                Log.d("$TAG js", "-- KEY GENERATION COMPLETE, NOW AUTHENTICATING")
                handshake(encryptedKey) { challenge ->
                    challenge(challenge, secret) {
                        Log.d("$TAG js", "Challenge successful? $it")
                    }
                }
            }
        }
    }

    /**
     * Encrypts our private key with sph's public key
     */
    private fun encryptKey(secret: String, callback: (key: String?) -> Unit) {
        getPublicKey { publicKey ->
            if (publicKey != null) {
                callback(execute("encryptWithPublicKey", arrayOf(
                        secret, publicKey
                )))
            } else {
                callback(null)
            }
        }
    }

    /**
     * Gets sph's public key from their website
     */
    private fun getPublicKey(callback: (publicKey: String?) -> Unit) {
        NetworkManager().getJSON(
                applicationContext().getString(R.string.url_cryption_publickey)) { success, result ->
            if (success == NetworkManager.SUCCESS && result?.get("publickey") != null) {
                callback(result.get("publickey").toString())
            } else {
                callback(null)
            }
        }
    }

    /**
     * Creates an AES key for encryption / decryption
     * Creates a (sudo-)random password that will be used
     * UUIDs aren't actually meant to be random, but that was sph's choice
     */
    private fun generateKey(): String {
        val pattern = "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx-xxxxxx3xx"
        var uuid = ""
        for (i in pattern.indices) {
            if (pattern[i] == 'x' || pattern[i] == 'y') {
                uuid += Integer.toHexString((floor(Math.random() * 17)).toInt())
            } else {
                uuid += pattern[i]
            }
        }
        Log.d("$TAG js", "uuid: $uuid")
        val key = execute("encrypt", arrayOf(uuid, uuid)).toString()
        Log.d("$TAG js", "key: $key")
        return key
    }

    /**
     * Performs a handshake with sph's server
     * @param encryptedKey The secret, encrypted with sph's public key
     */
    private fun handshake(encryptedKey: String, callback: (challenge: String) -> Unit) {

        val url = applicationContext().getString(
                R.string.url_cryption_handshake, (Math.random() * 2000).roundToInt().toString())

        Log.d("$TAG js", "Sending handshake to $url")

        // Send a post to the handshake server
        AndroidNetworking.post(url)
                .addBodyParameter("key", encryptedKey)
                .build()
                .getAsJSONObject(object : JSONObjectRequestListener {
                    override fun onResponse(response: JSONObject) {
                        Log.d("$TAG js", "Handshake challenge: " + response.get("challenge").toString())
                        callback(response.get("challenge").toString())
                    }

                    override fun onError(anError: ANError) {
                        TODO("Not yet implemented")
                    }

                })
    }

    /**
     * Check if the response from a handshake matches our secret when decrypted
     */
    private fun challenge(challenge: String, secret: String, callback: (success: Boolean) -> Unit) {
        decrypt(challenge, secret) { decrypted ->
            Log.d("$TAG js", "Decrypted: $decrypted")
            callback(decrypted == secret)
        }
    }

    /**
     * Decrypts data with the secret
     * @param data Encrypted data to be decrypted
     * @param secret Authenticated private key to decrypt
     * @param callback Callback when decryption is complete
     */
    fun decrypt(data: String, secret: String, callback: (decrypted: String?) -> Unit) {
        val dataJs = data.replace("\\", "")
        callback(execute("decrypt", arrayOf(dataJs, secret)))
    }

    /**
     * Executes the specified js function within cryption.js
     */
    private fun execute(jsFunction: String, params: Array<Any> = emptyArray()): String? {
        // todo Don't exit if multiple calls are made
        // Get the JavaScript in previous section
        try {
            // Get the js file
            val source = applicationContext().resources.openRawResource(R.raw.cryption)
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
                Log.d("$TAG js", "Execution for $jsFunction took $time ms")
                //Log.d("$TAG js", "Time to execute: $time ms")
                // Return the output
                Context.toString(result)
            } else {
                "Not a function"
            }
        } catch (ex: Exception) {
            Log.e(TAG, ex.stackTraceToString())
        } finally {
            // Exit Rhino VM
            Context.exit()
        }
        return "Execution failed"
    }

}