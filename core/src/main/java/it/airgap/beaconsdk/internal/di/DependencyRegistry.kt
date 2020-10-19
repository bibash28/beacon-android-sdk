package it.airgap.beaconsdk.internal.di

import it.airgap.beaconsdk.internal.storage.ExtendedStorage
import it.airgap.beaconsdk.internal.BeaconConfig
import it.airgap.beaconsdk.internal.client.ConnectionClient
import it.airgap.beaconsdk.internal.client.SdkClient
import it.airgap.beaconsdk.internal.controller.MessageController
import it.airgap.beaconsdk.internal.crypto.Crypto
import it.airgap.beaconsdk.internal.crypto.provider.CryptoProvider
import it.airgap.beaconsdk.internal.crypto.provider.LazySodiumCryptoProvider
import it.airgap.beaconsdk.internal.matrix.MatrixClient
import it.airgap.beaconsdk.internal.matrix.data.client.MatrixClientEvent
import it.airgap.beaconsdk.internal.protocol.ProtocolRegistry
import it.airgap.beaconsdk.internal.serializer.Serializer
import it.airgap.beaconsdk.internal.serializer.provider.Base58CheckSerializerProvider
import it.airgap.beaconsdk.internal.serializer.provider.SerializerProvider
import it.airgap.beaconsdk.internal.transport.p2p.P2pTransport
import it.airgap.beaconsdk.internal.transport.Transport
import it.airgap.beaconsdk.internal.transport.p2p.P2pCommunicationClient
import it.airgap.beaconsdk.internal.utils.AccountUtils
import it.airgap.beaconsdk.internal.utils.Base58Check
import it.airgap.beaconsdk.internal.utils.InternalResult
import it.airgap.beaconsdk.internal.utils.failWithUninitialized
import it.airgap.beaconsdk.storage.BeaconStorage
import kotlinx.coroutines.flow.Flow

internal class DependencyRegistry(
    private val appName: String,
    private val matrixNodes: List<String>,
    storage: BeaconStorage,
) {
    val extendedStorage: ExtendedStorage by lazy { ExtendedStorage(storage) }
    val sdkClient: SdkClient by lazy { SdkClient(extendedStorage, crypto) }
    val messageController: MessageController by lazy { MessageController(protocolRegistry, extendedStorage, accountUtils) }
    val protocolRegistry: ProtocolRegistry by lazy { ProtocolRegistry(crypto, base58Check) }
    val crypto: Crypto by lazy { Crypto(cryptoProvider) }
    val serializer: Serializer by lazy { Serializer(serializerProvider) }
    val accountUtils: AccountUtils by lazy { AccountUtils(crypto, base58Check) }
    val base58Check: Base58Check by lazy { Base58Check(crypto) }

    fun connectionController(transportType: Transport.Type): ConnectionClient =
        connectionControllers.getOrPut(transportType) {
            val transport = this.transport(transportType)

            return ConnectionClient(transport, serializer)
        }

    fun transport(type: Transport.Type): Transport =
        transports.getOrPut(type) {
            when (type) {
                Transport.Type.P2P -> {
                    val matrixClients = listOf(matrixClient)
                    val keyPair = sdkClient.keyPair ?: failWithUninitialized(SdkClient.TAG)
                    val client = P2pCommunicationClient(
                        matrixClients,
                        matrixNodes,
                        BeaconConfig.p2pReplicationCount,
                        crypto,
                        keyPair
                    )

                    return P2pTransport(appName, extendedStorage, client)
                }
            }
        }

    private val connectionControllers: MutableMap<Transport.Type, ConnectionClient> = mutableMapOf()
    private val transports: MutableMap<Transport.Type, Transport> = mutableMapOf()
    private val cryptoProvider: CryptoProvider by lazy {
        when (BeaconConfig.cryptoProvider) {
            BeaconConfig.CryptoProvider.LazySodium -> LazySodiumCryptoProvider()
        }
    }
    private val serializerProvider: SerializerProvider by lazy {
        when (BeaconConfig.serializerProvider) {
            BeaconConfig.SerializerProvider.Base58Check -> Base58CheckSerializerProvider(base58Check)
        }
    }

    private val matrixClient: MatrixClient
        get() {
        // TOOD: provide real implementation
        return object : MatrixClient {
            override val joinedRooms: List<Any>
                get() = TODO("Not yet implemented")
            override val invitedRooms: List<Any>
                get() = TODO("Not yet implemented")
            override val leftRooms: List<Any>
                get() = TODO("Not yet implemented")
            override val events: Flow<InternalResult<MatrixClientEvent<*>>>
                get() = TODO("Not yet implemented")

            override suspend fun start(id: String, password: String, deviceId: String) {
                TODO("Not yet implemented")
            }

            override suspend fun createTrustedPrivateRoom(vararg members: String) {
                TODO("Not yet implemented")
            }

            override suspend fun inviteToRooms(user: String, vararg roomIds: String) {
                TODO("Not yet implemented")
            }

            override suspend fun inviteToRooms(user: String, vararg rooms: Any) {
                TODO("Not yet implemented")
            }

            override suspend fun joinRooms(user: String, vararg roomIds: String) {
                TODO("Not yet implemented")
            }

            override suspend fun joinRooms(user: String, vararg rooms: Any) {
                TODO("Not yet implemented")
            }

            override suspend fun sendTextMessage(roomId: String, message: String) {
                TODO("Not yet implemented")
            }

            override suspend fun sendTextMessage(room: Any, message: String) {
                TODO("Not yet implemented")
            }

        }
    }

    companion object {
        const val TAG = "GlobalServiceLocator"
    }
}