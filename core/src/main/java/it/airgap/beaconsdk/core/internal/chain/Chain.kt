package it.airgap.beaconsdk.core.internal.chain

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.message.VersionedBeaconMessage
import it.airgap.beaconsdk.core.internal.message.v1.V1BeaconMessage
import it.airgap.beaconsdk.core.internal.message.v2.V2BeaconMessage
import it.airgap.beaconsdk.core.message.BeaconMessage

@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface Chain<W : Chain.Wallet, MF : Chain.MessageFactory> {
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    public val wallet: W

    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    public val messageFactory: MF

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public interface Wallet {
        public fun addressFromPublicKey(publicKey: String): Result<String>
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public interface MessageFactory {
        public val v1: VersionedBeaconMessage.Factory<BeaconMessage, V1BeaconMessage>
        public val v2: VersionedBeaconMessage.Factory<BeaconMessage, V2BeaconMessage>
    }

    public interface Factory<T: Chain<*, *>> {
        public val identifier: String

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public fun create(dependencyRegistry: DependencyRegistry): T
    }
}