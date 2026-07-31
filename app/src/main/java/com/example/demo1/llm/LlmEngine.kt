package com.example.demo1.llm


import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import java.io.File

class LlmEngine(
    context: Context
) {
    private val appContext: Context = context.applicationContext

    private val modelFile: File

    private var llmInference: LlmInference? = null

    init {
        val modelFolder = File(
            appContext.getExternalFilesDir(null),
            "models"
        )

        if (!modelFolder.exists()) {
            modelFolder.mkdirs()
        }

        modelFile = File(
            modelFolder,
            "gemma-3n-E2B-it-int4.task"
        )
    }

    fun initialize() {
        if (!modelFile.exists()) {
            throw IllegalStateException(
                "Model dosyası bulunamadı: ${modelFile.absolutePath}"
            )
        }

        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelFile.absolutePath)
            .setMaxTokens(1024) //dene bakayım bunu daha iyi cevaplar verecek mi
            .build()

        llmInference = LlmInference.createFromOptions(
            appContext,
            options
        )
    }

    fun ask(prompt: String): String {
        return try {
            if (llmInference == null) {
                initialize()
            }

            llmInference!!.generateResponse(prompt)
        } catch (e: Exception) {
            "LLM hatası: ${e.message}"
        }
    }

    fun close() {
        llmInference?.close()
        llmInference = null
    }

    fun getModelPath(): String {
        return modelFile.absolutePath
    }
}