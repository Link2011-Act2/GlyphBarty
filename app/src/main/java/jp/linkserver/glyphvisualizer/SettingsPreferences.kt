package jp.linkserver.glyphvisualizer

import android.content.Context

object SettingsPreferences {
    private const val PREFS_NAME = "glyph_visualizer_settings"
    private const val KEY_INITIAL_SETUP_COMPLETED = "initial_setup_completed"
    private const val KEY_NOTIFICATION_PERMISSION_PROMPT_SHOWN =
        "notification_permission_prompt_shown"
    private const val KEY_PHONE4B_EMULATION_ENABLED = "phone4b_emulation_enabled"

    fun defaultParameters(): CaptureUiState = CaptureUiState()

    fun parameterStateOf(state: CaptureUiState): CaptureUiState =
        PersistedSettingsSchema.resolve(PersistedSettingsSchema.fromState(state)).state

    fun loadPersisted(context: Context): PersistedSettings =
        PersistedSettingsPreferenceCodec.read(preferences(context))

    fun loadEffective(context: Context): EffectiveSettings =
        PersistedSettingsSchema.resolve(loadPersisted(context))

    fun load(context: Context): CaptureUiState = loadEffective(context).state

    fun save(context: Context, state: CaptureUiState) {
        val preferences = preferences(context)
        val persisted = PersistedSettingsSchema.mergeForSave(
            previous = PersistedSettingsPreferenceCodec.read(preferences),
            state = state
        )
        PersistedSettingsPreferenceCodec.write(preferences.edit(), persisted)
    }

    fun loadPhone4bEmulationEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(
            KEY_PHONE4B_EMULATION_ENABLED,
            defaultParameters().phone4bEmulationEnabled
        )
    }

    fun hasCompletedInitialSetup(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.contains(KEY_INITIAL_SETUP_COMPLETED)) {
            return prefs.getBoolean(KEY_INITIAL_SETUP_COMPLETED, false)
        }
        return prefs.all.isNotEmpty()
    }

    fun markInitialSetupCompleted(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_INITIAL_SETUP_COMPLETED, true)
            .apply()
    }

    fun hasShownNotificationPermissionPrompt(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_NOTIFICATION_PERMISSION_PROMPT_SHOWN, false)
    }

    fun markNotificationPermissionPromptShown(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_NOTIFICATION_PERMISSION_PROMPT_SHOWN, true)
            .apply()
    }

    fun exportJson(state: CaptureUiState): String =
        PersistedSettingsJsonCodec.export(
            PersistedSettingsSchema.fromState(parameterStateOf(state))
        )

    fun importJson(jsonText: String): CaptureUiState =
        PersistedSettingsSchema.resolve(
            PersistedSettingsJsonCodec.import(
                jsonText = jsonText,
                defaults = PersistedSettingsSchema.defaults()
            )
        ).state

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
