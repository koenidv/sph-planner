package de.koenidv.sph.networking

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.google.firebase.crashlytics.FirebaseCrashlytics
import de.koenidv.sph.BuildConfig
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner.Companion.TAG
import de.koenidv.sph.SphPlanner.Companion.applicationContext
import de.koenidv.sph.networking.NetworkManager.Companion.FAILED_CRYPTION
import de.koenidv.sph.networking.NetworkManager.Companion.FAILED_SERVER_ERROR
import de.koenidv.sph.networking.NetworkManager.Companion.FAILED_UNKNOWN
import de.koenidv.sph.networking.NetworkManager.Companion.SUCCESS
import kotlinx.coroutines.*
import org.json.JSONObject
import org.mozilla.javascript.Function
import org.mozilla.javascript.Scriptable
import java.util.*
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.system.measureTimeMillis

//  Created by koenidv on 04.02.2021.
// If it ain't broken, don't fix it. Unless you really know what you're doing.
class Cryption {

    companion object {
        fun start(current: Cryption? = null, callback: (success: Int, cryption: Cryption?) -> Unit) =
                if (current != null && current.vmrunnning)
                    callback(SUCCESS, current)
                else
                    Cryption().getCryptor(callback)
    }


    private lateinit var privateKey: String

    // The VM has to be called from the same thread
    @ObsoleteCoroutinesApi // singleThreadContext
    private val cryptionCoroutine = newSingleThreadContext("cryption")
    private lateinit var rhino: org.mozilla.javascript.Context
    private lateinit var scope: Scriptable
    private var vmrunnning = false

    /**
     * Decrypts data with the stored secret
     * @param data Encrypted data to be decrypted
     * @param callback Callback when decryption is complete
     */
    fun decrypt(data: String, callback: (decrypted: String?) -> Unit) =
            decrypt(data, privateKey, callback)

    /**
     * Encrypts data with the stored secret
     * @param data Data to be encrypted
     * @param callback Callback when encryption is complete
     */
    fun encrypt(data: String, callback: (encrypted: String?) -> Unit) =
            encrypt(data, privateKey, callback)

    /**
     * Stops the Rhino VM
     * The VM will not stop by itself ;)
     */
    fun stop() {
        try {
            // Exit Rhino VM
            org.mozilla.javascript.Context.exit()
            vmrunnning = false
        } catch (e: IllegalStateException) {
            // VM hasn't been started
        }
        // Close coroutine context
        @Suppress("EXPERIMENTAL_API_USAGE")
        cryptionCoroutine.close()
    }

    /**
     * Returns an Cryption object with a set secret value
     * Or null if authentication failed
     */
    private fun getCryptor(callback: (Int, Cryption?) -> Unit) {
        val prefs = applicationContext().getSharedPreferences(
                "sharedPrefs", Context.MODE_PRIVATE)
        // Get the current session id token to check if it has changed
        TokenManager.getToken { tokensuccess, token ->
            // Cancel if token authentication was not successful
            if (tokensuccess != SUCCESS) {
                callback(tokensuccess, null)
                // Stop the vm, is probly still starting
                stop()
                return@getToken
            }

            // Keep the same secret only for up to 15 minutes and only if the token hasn't changed
            if (Date().time - prefs.getLong("cryption_time", 0) <= 15 * 60 * 1000 &&
                    prefs.getString("cryption_token", null) == token &&
                    prefs.getString("cryption_secret", null) != null) {
                // Use cached secret (todo responsible to store the secret in sharedprefs?)
                privateKey = prefs.getString("cryption_secret", null)!!
                callback(SUCCESS, this)
            } else {
                // We need to create a new secret and authenticate it
                authenticate { success ->
                    // If authentication was successful, call back with this authenticated Cryption
                    if (success == SUCCESS) {
                        // Save the secret in sharedPrefs for use within the next 15 minutes
                        prefs.edit().putLong("cryption_time", Date().time)
                                .putString("cryption_token", token)
                                .putString("cryption_secret", privateKey)
                                .apply()
                        // Callback with this Cryption object, which is now authenticated with privateKey
                        callback(SUCCESS, this)
                    } else {
                        callback(success, null)
                        // Stop the vm as it will not be needed anymore
                        stop()
                    }
                }
            }

        }

    }

    /**
     * Generates a key and authenticates it
     * The secret will be saved in the secret field and a success type returned
     */
    private fun authenticate(callback: (success: Int) -> Unit) {
        // Generate a private key
        generateKey { secret ->

            // Encrypt this private key with sph's public key
            encryptKey(secret) { encryptedKey ->
                if (encryptedKey != null) {

                    // Now perform a handshake to let sph know how it should encrypt data
                    handshake(encryptedKey) { success, challenge ->
                        if (success == SUCCESS && challenge != null) {

                            // Check if the returned challenge matches
                            challenge(challenge, secret) { matches ->
                                Log.d("$TAG crypt", "Challenge successful? $matches")
                                if (matches) {
                                    // Set private key if challenge was successful
                                    privateKey = secret
                                    callback(SUCCESS)
                                } else callback(FAILED_SERVER_ERROR)

                            }
                        } else callback(success)
                    }
                } else callback(FAILED_CRYPTION)
            }
        }
    }

    /**
     * Encrypts our private key with sph's public key
     * Used for the handshake
     * @param secret Our private key from #generateKey
     * @param callback Callback with the encrypted key
     */
    private fun encryptKey(secret: String, callback: (encrypted: String?) -> Unit) {
        getPublicKey { publicKey ->
            if (publicKey != null) {
                execute("encryptWithPublicKey", arrayOf(
                        secret, publicKey
                ), callback)
            } else {
                callback(null)
            }
        }
    }

    /**
     * Gets sph's public key from their website
     * @param callback Sph's public key
     */
    private fun getPublicKey(callback: (publicKey: String?) -> Unit) {
        NetworkManager().getJson(
                applicationContext().getString(R.string.url_cryption_publickey)) { success, result ->
            if (success == SUCCESS && result?.get("publickey") != null) {
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
     * @return A generated uuid, encrypted with itself
     */
    private fun generateKey(callback: (key: String) -> Unit) {
        // Generate a uuid
        val pattern = "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx-xxxxxx3xx"
        var uuid = ""
        for (i in pattern.indices) {
            if (pattern[i] == 'x' || pattern[i] == 'y') {
                uuid += Integer.toHexString((floor(Math.random() * 17)).toInt())
            } else {
                uuid += pattern[i]
            }
        }
        // Encrypt the uuid with itself
        execute("encrypt", arrayOf(uuid, uuid), callback)
    }

    /**
     * Performs a handshake with sph's server
     * @param encryptedKey The secret, encrypted with sph's public key
     * @param callback Success type and Challenge value from sph's response
     */
    private fun handshake(encryptedKey: String, callback: (success: Int, challenge: String?) -> Unit) {
        // The handshake needs to be sent with a session id
        TokenManager.getToken { tokensuccess, _ ->
            // Cancel if token authentication was not successful
            if (tokensuccess != SUCCESS) {
                callback(tokensuccess, null)
                return@getToken
            }

            // Get the handshake url with a random value for s between 0 and 2000
            val url = applicationContext().getString(
                    R.string.url_cryption_handshake, (Math.random() * 2000).roundToInt().toString())

            // Send a post to the handshake server
            AndroidNetworking.post(url)
                    .addBodyParameter("key", encryptedKey)
                    .addHeaders("X-Requested-With", "XMLHttpRequest")
                    .build()
                    .getAsJSONObject(object : JSONObjectRequestListener {
                        override fun onResponse(response: JSONObject) {
                            try {
                                callback(SUCCESS, response.get("challenge").toString())
                            } catch (e: java.lang.Exception) {
                                callback(FAILED_UNKNOWN, null)
                            }
                        }

                        override fun onError(error: ANError) {
                            // Handle error
                            when (error.errorDetail) {
                                "connectionError" -> callback(NetworkManager.FAILED_NO_NETWORK, null)
                                "requestCancelledError" -> callback(NetworkManager.FAILED_CANCELLED, null)
                                else -> {
                                    callback(FAILED_UNKNOWN, null)
                                    Toast.makeText(applicationContext(), "Error for $url",
                                            Toast.LENGTH_LONG).show()
                                    Toast.makeText(applicationContext(),
                                            error.errorCode.toString()
                                                    + ": " + error.errorDetail,
                                            Toast.LENGTH_LONG).show()
                                    // Log the error in Crashlytics
                                    FirebaseCrashlytics.getInstance().recordException(error)
                                }
                            }
                        }

                    })

        }
    }

    /**
     * Check if the response from a handshake matches our secret when decrypted
     * @param challenge Challenge value from sph's handshake response
     * @param secret Private key to decrypt the challenge data
     * @param callback True if decrypted data matches the secret
     */
    private fun challenge(challenge: String, secret: String = privateKey, callback: (success: Boolean) -> Unit) {
        // Decrypt the data using the provided secret
        decrypt(challenge, secret) { decrypted ->
            callback(decrypted == secret)
        }
    }

    /**
     * Decrypts data with the secret
     * @param data Encrypted data to be decrypted
     * @param secret Authenticated private key to decrypt
     * @param callback Callback when decryption is complete
     */
    private fun decrypt(data: String, secret: String, callback: (decrypted: String?) -> Unit) {
        val dataJs = data.replace("\\", "")
        execute("decrypt", arrayOf(dataJs, secret), callback)
        Log.d("SPH-PLANNER", "DECR KEY IS $secret")
    }

    /**
     * Encrypts data with the secret
     * @param data Data to be encrypted
     * @param secret Authenticated private key to decrypt
     * @param callback Callback when encryption is complete
     */
    private fun encrypt(data: String, secret: String, callback: (encrypted: String?) -> Unit) {
        val dataJs = data.replace("\\", "")
        execute("encrypt", arrayOf(dataJs, secret), callback)
        Log.d("SPH-PLANNER", "ENCR KEY IS $secret")
    }

    /**
     * Executes the specified js function within cryption.js
     * THIS WILL NOT START OR STOP THE VM
     * Call #startVm or #stop respectively
     * @param jsFunction Function to execute. Must be valid within res/raw/cryption.js
     * @param params Array of parameters to be passed to the called function
     */
    private fun execute(jsFunction: String,
                        params: Array<Any> = emptyArray(),
                        callback: (String) -> Unit) {
        // Run asynchronously
        @Suppress("EXPERIMENTAL_API_USAGE")
        CoroutineScope(cryptionCoroutine).launch {

            // If the vm wasn't already started, start it now
            if (!::rhino.isInitialized || !::scope.isInitialized) {
                Log.d("$TAG js", "Starting Rhino VM")
                // Get the js file
                val source = applicationContext().resources.openRawResource(R.raw.cryption)
                        .bufferedReader().use { it.readText() }

                // Initialize the Rhino VM using enter()
                rhino = org.mozilla.javascript.Context.enter()

                // Turn off optimization to make Rhino Android compatible
                rhino.optimizationLevel = -1

                // Initialize standard objects like String in the VM
                scope = rhino.initStandardObjects()

                // Evaluate text from the js file as code
                rhino.evaluateString(scope, source, "Cryption JS", 1, null)

                vmrunnning = true
                // Set up a warning if the vm wasn't stopped after 1 minute
                CoroutineScope(Dispatchers.Unconfined).launch {
                    delay(60 * 1000)
                    if (vmrunnning)
                        Log.w("$TAG js", "The VM has been running for 1 minute! Forgot to call stop()?")
                }
            }

            if (BuildConfig.DEBUG && (!::rhino.isInitialized || !::scope.isInitialized)) {
                error("Initializing Rhino VM failed")
            }

            // Check if the specified object is actually a function and execute it
            val obj: Any? = scope.get(jsFunction, scope)
            if (obj is Function) {
                val execTime = measureTimeMillis {
                    // Call the function with the params
                    val result: Any = obj.call(rhino,
                            scope,
                            scope,
                            params)
                    // Return the output
                    callback(org.mozilla.javascript.Context.toString(result))
                }
                Log.d("$TAG js", "Execution for $jsFunction took $execTime ms")
            } else {
                throw Exception("$jsFunction is not a valid Function at Cryption#execute")
            }
        }
    }

}