package it.airgap.beaconsdk.blockchain.tezos.internal.creator

import it.airgap.beaconsdk.blockchain.tezos.data.TezosAppMetadata
import it.airgap.beaconsdk.blockchain.tezos.data.TezosPermission
import it.airgap.beaconsdk.blockchain.tezos.internal.utils.failWithUnknownMessage
import it.airgap.beaconsdk.blockchain.tezos.internal.wallet.TezosWallet
import it.airgap.beaconsdk.blockchain.tezos.message.request.PermissionTezosRequest
import it.airgap.beaconsdk.blockchain.tezos.message.response.PermissionTezosResponse
import it.airgap.beaconsdk.core.data.Permission
import it.airgap.beaconsdk.core.internal.blockchain.creator.DataBlockchainCreator
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.IdentifierCreator
import it.airgap.beaconsdk.core.internal.utils.asHexString
import it.airgap.beaconsdk.core.internal.utils.currentTimestamp
import it.airgap.beaconsdk.core.internal.utils.failWithIllegalState
import it.airgap.beaconsdk.core.message.PermissionBeaconRequest
import it.airgap.beaconsdk.core.message.PermissionBeaconResponse

internal class DataTezosCreator(
    private val wallet: TezosWallet,
    private val storageManager: StorageManager,
    private val identifierCreator: IdentifierCreator,
) : DataBlockchainCreator {
    override suspend fun extractPermission(
        request: PermissionBeaconRequest,
        response: PermissionBeaconResponse,
    ): Result<List<Permission>> =
        runCatching {
            if (request !is PermissionTezosRequest) failWithUnknownMessage(request)
            if (response !is PermissionTezosResponse) failWithUnknownMessage(response)

            val address = wallet.address(response.publicKey).getOrThrow()
            val accountId = identifierCreator.accountId(address, response.network).getOrThrow()
            val appMetadata = storageManager.findInstanceAppMetadata<TezosAppMetadata> { it.senderId == request.senderId } ?: failWithAppMetadataNotFound()
            val senderId = identifierCreator.senderId(request.origin.id.asHexString().toByteArray()).getOrThrow()

            listOf(
                TezosPermission(
                    accountId,
                    senderId,
                    connectedAt = currentTimestamp(),
                    address,
                    response.publicKey,
                    response.network,
                    appMetadata,
                    response.scopes,
                ),
            )
        }

    private fun failWithAppMetadataNotFound(): Nothing = failWithIllegalState("Permission could not be extracted, matching appMetadata not found.")
}