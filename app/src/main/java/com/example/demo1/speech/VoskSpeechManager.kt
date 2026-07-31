package com.example.demo1.speech

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService

class VoskSpeechManager(
    private val context: Context,
    private val onTextResult: (String) -> Unit,
    private val onError: (String) -> Unit
) {

    private var model: Model? = null
    private var speechService: SpeechService? = null
    private var isModelReady = false

    private val mainHandler = Handler(Looper.getMainLooper())

    fun initialize() {
        StorageService.unpack(
            context,
            "models/vosk-model-small-en-us",
            "vosk-model-small-en-us",
            { loadedModel ->
                model = loadedModel
                isModelReady = true
            },
            { exception ->
                mainHandler.post {
                    onError("Vosk model yüklenemedi: ${exception.message}")
                }
            }
        )
    }

    fun startListening() {
        val currentModel = model

        if (!isModelReady || currentModel == null) {
            onError("Vosk modeli henüz hazır değil.")
            return
        }

        try {
            val recognizer = Recognizer(currentModel, 16000.0f)

            speechService = SpeechService(
                recognizer,
                16000.0f
            )

            speechService?.startListening(
                object : RecognitionListener {

                    override fun onPartialResult(hypothesis: String?) {
                        // Ara sonuç. Şimdilik kullanmıyoruz.
                    }

                    override fun onResult(hypothesis: String?) {
                        val text = extractTextFromVoskJson(hypothesis)

                        if (text.isNotBlank()) {
                            mainHandler.post {
                                onTextResult(text)
                            }
                        }
                    }

                    override fun onFinalResult(hypothesis: String?) {
                        val text = extractTextFromVoskJson(hypothesis)

                        if (text.isNotBlank()) {
                            mainHandler.post {
                                onTextResult(text)
                            }
                        }
                    }

                    override fun onError(exception: Exception?) {
                        mainHandler.post {
                            onError("Vosk hata: ${exception?.message}")
                        }
                    }

                    override fun onTimeout() {
                        stopListening()
                    }
                }
            )
        } catch (e: Exception) {
            onError("Dinleme başlatılamadı: ${e.message}")
        }
    }

    fun stopListening() {
        speechService?.stop()
        speechService = null
    }

    fun close() {
        stopListening()
        model?.close()
        model = null
        isModelReady = false
    }


                 //Burada JSON şunun için var: Vosk konuşmayı yazıya çevirdiğinde sonucu düz metin olarak değil, JSON formatında döndürüyor. Bu yüzden biz o JSON’un içinden sadece "text" alanını çekiyoruz.
    private fun extractTextFromVoskJson(jsonText: String?): String {
        if (jsonText.isNullOrBlank()) {
            return ""
        }

        return try {
            val jsonObject = JSONObject(jsonText)
            jsonObject.optString("text", "")
        } catch (e: Exception) {
            ""
        }
    }
}