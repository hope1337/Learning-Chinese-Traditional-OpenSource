package com.example.chinese2.models

/** Minimal active-model selection per role. Weights live outside git. */
enum class ModelRole { OCR, TRANSLATION }

data class ModelDescriptor(
    val id: String,
    val role: ModelRole,
    val displayName: String,
)

class ModelManager {
    private val active = mutableMapOf<ModelRole, String>()

    fun activeId(role: ModelRole): String? = active[role]

    fun setActive(role: ModelRole, id: String) {
        active[role] = id
    }

    companion object {
        val BUILT_IN = listOf(
            ModelDescriptor("mlkit-chinese-fast", ModelRole.OCR, "ML Kit Chinese (fast)"),
            ModelDescriptor("paddle-ppocrv5-mobile", ModelRole.OCR, "Paddle PP-OCRv5 mobile (accurate)"),
        )
    }
}
