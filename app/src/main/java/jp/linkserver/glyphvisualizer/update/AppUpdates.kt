package jp.linkserver.glyphvisualizer.update

import android.content.Context
import jp.linkserver.goldy.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale

/** Host preference boundary. Catalog, selection, cache and ignore state belong to Goldy. */
class AppUpdates private constructor(context: Context) {
    private val app = context.applicationContext
    private val legacy = app.getSharedPreferences("github_release_updates", 0)
    private val host = app.getSharedPreferences("goldy_host_updates", 0)
    private val store = UpdateStateStores.preferences(app, ApkUpdateProfile.repository)
    private val engine = Goldy.create(app, GoldyConfig(ApkUpdateProfile.repository, ApkUpdateProfile.policy,
        userAgent="GlyphBarty/${installedVersionName(app)}"), store)
    private val gate = Mutex()
    private var initialized = false
    private val scope = CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private val hostFailure = MutableStateFlow<CheckOutcome.Failure?>(null)
    val state = combine(engine.state,hostFailure) { current,error ->
        if(error==null) current else current.copy(lastOutcome=error)
    }.stateIn(scope,SharingStarted.Eagerly,GoldyState())
    companion object {
        @Volatile private var instance: AppUpdates? = null
        fun get(context: Context): AppUpdates = instance ?: synchronized(this) {
            instance ?: AppUpdates(context.applicationContext).also { instance=it }
        }
    }
    private suspend fun initialize() = gate.withLock {
        if (!initialized) {
            if (!host.getBoolean("migration_v1",false)) {
                migrateIgnoredTag(store,legacy.getString("dismissed_update_tag",null))
                withContext(Dispatchers.IO) {
                    kotlin.check(host.edit().putLong("legacy_last_success_ms",legacy.getLong("last_check_ms",0))
                        .putString("legacy_last_check_version",legacy.getString("last_check_version",null))
                        .putBoolean("migration_v1",true).commit())
                }
                // Legacy DTOs have no trusted IDs/size/digest; retain them and fetch real metadata.
            }
            engine.updateOptions(updateOptions(app))
            engine.restoreCachedState()
            if (host.getString("hidden_banner_tag",null)?.equals(engine.state.value.notificationUpdate?.release?.tag,true)==true)
                engine.hideBanner()
            initialized=true
        }
    }
    internal suspend fun restoreCachedState() = initialize()
    suspend fun check(force: Boolean=false): CheckOutcome = try {
        initialize(); engine.updateOptions(updateOptions(app)); hostFailure.value=null
        engine.check(if(force) CheckTrigger.Manual else CheckTrigger.Automatic).also {
            if(host.getString("hidden_banner_tag",null)?.equals(engine.state.value.notificationUpdate?.release?.tag,true)==true) engine.hideBanner()
        }
    } catch(cancel: CancellationException) { throw cancel }
    catch(error: Exception) { CheckOutcome.Failure(GoldyError.Storage).also { hostFailure.value=it } }
    private fun command(action: suspend () -> Unit) { scope.launch {
        try { initialize(); action(); hostFailure.value=null }
        catch(cancel: CancellationException) { throw cancel }
        catch(error: Exception) { hostFailure.value=CheckOutcome.Failure(GoldyError.Storage) }
    } }
    fun hideBanner() { command {
        initialize(); val tag=engine.state.value.notificationUpdate?.release?.tag; engine.hideBanner()
        withContext(Dispatchers.IO) { kotlin.check(host.edit().putString("hidden_banner_tag",tag).commit()) }
    } }
    fun ignoreVersion(tag: String) { command { engine.ignoreVersion(tag) } }
    fun resetIgnoredVersions() { command {
        initialize(); engine.resetIgnoredVersions()
        withContext(Dispatchers.IO) { kotlin.check(legacy.edit().remove("dismissed_update_tag").commit()) }
    } }
}
internal suspend fun migrateIgnoredTag(store: UpdateStateStore, tag: String?) {
    val value=tag?.takeIf { it.isNotBlank() && it.length<=512 }?.lowercase(Locale.ROOT)
    val previous=store.read(); store.write(previous.copy(ignoredTags=previous.ignoredTags+listOfNotNull(value)))
}
fun createAppUpdates(context: Context): AppUpdates = AppUpdates.get(context)
