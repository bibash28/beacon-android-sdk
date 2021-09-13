package it.airgap.beaconsdk.core.internal

import android.content.Context
import it.airgap.beaconsdk.core.internal.chain.Chain
import it.airgap.beaconsdk.core.internal.crypto.Crypto
import it.airgap.beaconsdk.core.internal.crypto.data.KeyPair
import it.airgap.beaconsdk.core.internal.data.BeaconApplication
import it.airgap.beaconsdk.core.internal.di.CoreDependencyRegistry
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.storage.SecureStorage
import it.airgap.beaconsdk.core.internal.storage.Storage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.storage.StoragePlugin
import it.airgap.beaconsdk.core.internal.utils.failWithUninitialized
import it.airgap.beaconsdk.core.internal.utils.toHexString

public class BeaconSdk(context: Context) {
    var isInitialized: Boolean = false
        private set

    val applicationContext: Context = context.applicationContext

    private var _dependencyRegistry: DependencyRegistry? = null
    val dependencyRegistry: DependencyRegistry
        get() = _dependencyRegistry ?: failWithUninitialized(TAG)

    private var _app: BeaconApplication? = null
    val app: BeaconApplication
        get() = _app ?: failWithUninitialized(TAG)

    val beaconId: String
        get() = app.keyPair.publicKey.toHexString().asString()

    suspend fun init(
        appName: String,
        appIcon: String?,
        appUrl: String?,
        chainFactories: List<Chain.Factory<*>>,
        storage: Storage,
        secureStorage: SecureStorage,
        storagePlugins: List<StoragePlugin>,
    ) {
        if (isInitialized) return

        _dependencyRegistry = CoreDependencyRegistry(chainFactories, storage, secureStorage, storagePlugins)

        val storageManager = dependencyRegistry.storageManager
        val crypto = dependencyRegistry.crypto

        setSdkVersion(storageManager)

        _app = BeaconApplication(
            loadOrGenerateKeyPair(storageManager, crypto),
            appName,
            appIcon,
            appUrl,
        )

        isInitialized = true
    }

    private suspend fun setSdkVersion(storageManager: StorageManager) {
        storageManager.setSdkVersion(BeaconConfiguration.sdkVersion)
    }

    private suspend fun loadOrGenerateKeyPair(storageManager: StorageManager, crypto: Crypto): KeyPair {
        val seed = storageManager.getSdkSecretSeed()
            ?: crypto.guid().getOrThrow().also { storageManager.setSdkSecretSeed(it) }

        return crypto.getKeyPairFromSeed(seed).getOrThrow()
    }

    companion object {
        const val TAG = "BeaconSdk"

        @Suppress("ObjectPropertyName")
        private var _instance: BeaconSdk? = null
        val instance: BeaconSdk
            get() = _instance ?: failWithUninitialized(TAG)

        fun create(context: Context) {
            _instance = BeaconSdk(context)
        }
    }
}