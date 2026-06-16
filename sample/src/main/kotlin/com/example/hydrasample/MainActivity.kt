package com.example.hydrasample

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.github.iamjosephmj.hydra.Hydra

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Decrypted at runtime via the obfuscated native runtime; the plaintext
        // never appears in classes.dex — only ciphertext + a Hydra.secret(...) call.
        val apiUrl = Hydra.secret("apiUrl")
        Log.d("HydraSample", "resolved api host len=${apiUrl.length}")
    }
}
