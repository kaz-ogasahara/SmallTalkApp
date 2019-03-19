package com.example.iscarrot.smalltalkapp

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.annotation.RequiresApi
import com.github.kittinunf.result.Result
import androidx.appcompat.app.AppCompatActivity
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.cUrlString
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*


class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener{
    companion object {
        const val REQUEST_SPEECH_RECOGNIZER = 1000
        const val PATH = "https://api.a3rt.recruit-tech.co.jp/talk/v1/smalltalk"
        const val API_KEY = BuildConfig.ApiKey
    }

    private var tts : TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // only once when Activity is created
        if(savedInstanceState == null) {
            micButton.setOnClickListener { view ->
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                }
                startActivityForResult(intent, REQUEST_SPEECH_RECOGNIZER)
            }

            // The button is (by default) disabled, so you can't click it.  Enable it if the
            // isRecognitionAvailable() supported
            if (SpeechRecognizer.isRecognitionAvailable(this)) {
                micButton.isEnabled = true
            } else {
                Log.i("ONCLICK", "Speech Recognition is not available")
            }

            // Init TextToSpeech
            tts = TextToSpeech(this, this)
            tts?.language = Locale.JAPANESE
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_SPEECH_RECOGNIZER) {
            if (resultCode == RESULT_OK) {
                data?.let {
                    val results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    messageLabel.text = results[0]
                    Fuel.post(PATH, listOf("apikey" to API_KEY, "query" to results[0])).responseString{ request, response, result ->
                        messageLabel2.text = request.cUrlString()
                        when (result) {
                            is Result.Failure -> {
                                val ex = result.getException()
                            }
                            is Result.Success -> {
                                messageLabel2.text = String(response.data)
                                val moshi = Moshi.Builder()
                                    .add(KotlinJsonAdapterFactory())
                                    .build()
                                val res = moshi.adapter(Talk::class.java).fromJson(result.get()) ?: Talk()
                                messageLabel2.text = res.results[0].reply
                                tts?.speak(res.results[0].reply, TextToSpeech.QUEUE_FLUSH, null, "speech1")
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            Log.d("TTS", "Success Init TextToSpeech")

            // 音声再生のイベントリスナを登録
            val listener : SpeechListener? = SpeechListener()
            tts?.setOnUtteranceProgressListener(listener)
        } else {
            Log.e("TTS", "Failed Init TextToSpeech")
        }
    }
}

data class Talk (
    val status: Int = 9999,
    val message: String = "Error",
    val results: List<TalkResult> = listOf(TalkResult())
)

data class TalkResult (
    val perplexity: Float = 0.0f,
    val reply: String = "Error"
)

