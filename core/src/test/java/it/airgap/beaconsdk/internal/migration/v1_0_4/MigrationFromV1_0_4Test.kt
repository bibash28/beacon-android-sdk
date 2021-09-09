package it.airgap.beaconsdk.internal.migration.v1_0_4

import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.internal.BeaconConfiguration
import it.airgap.beaconsdk.internal.migration.VersionedMigration
import it.airgap.beaconsdk.internal.storage.MockSecureStorage
import it.airgap.beaconsdk.internal.storage.MockStorage
import it.airgap.beaconsdk.internal.storage.StorageManager
import it.airgap.beaconsdk.internal.utils.AccountUtils
import kotlinx.coroutines.test.runBlockingTest
import mockLog
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Suppress("ClassName")
internal class MigrationFromV1_0_4Test {

    @MockK
    private lateinit var accountUtils: AccountUtils

    private lateinit var storageManager: StorageManager
    private lateinit var migration: MigrationFromV1_0_4

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        mockLog()

        storageManager = StorageManager(MockStorage(), MockSecureStorage(), accountUtils)
        migration = MigrationFromV1_0_4(storageManager)
    }

    @Test
    fun `targets SDK version from 1_0_4`() {
        assertEquals("1.0.4", migration.fromVersion)
    }

    @Test
    fun `targets Matrix relay server migration only`() {
        assertTrue(migration.targets(VersionedMigration.Target.MatrixRelayServer(listOf())))
    }

    @Test
    fun `creates valid migration identifier`() {
        assertEquals(
            "from_1.0.4@matrixRelayServer",
            migration.migrationIdentifier(VersionedMigration.Target.MatrixRelayServer(listOf())),
        )
    }

    @Test
    fun `skips if relay server is already set`() {
        val relayServer = "relayServer"
        val matrixNodes = listOf("node1", "node2")
        val target = VersionedMigration.Target.MatrixRelayServer(matrixNodes)

        runBlockingTest {
            storageManager.setMatrixRelayServer(relayServer)

            migration.perform(target).getOrThrow()

            val expectedInStorage = relayServer
            val actualInStorage = storageManager.getMatrixRelayServer()

            assertEquals(expectedInStorage, actualInStorage)
        }
    }

    @Test
    fun `skips if list of nodes differs from defaults`() {
        val matrixNodes = listOf("node1", "node2")
        val target = VersionedMigration.Target.MatrixRelayServer(matrixNodes)

        runBlockingTest {
            storageManager.setMatrixRelayServer(null)

            migration.perform(target).getOrThrow()

            val actualInStorage = storageManager.getMatrixRelayServer()

            assertNull(actualInStorage)
        }
    }

    @Test
    fun `skips if there is no active Matrix connection to maintain`() {
        val matrixNodes = BeaconConfiguration.defaultRelayServers
        val target = VersionedMigration.Target.MatrixRelayServer(matrixNodes)

        runBlockingTest {
            with(storageManager) {
                setMatrixRelayServer(null)
                setMatrixSyncToken(null)
                setMatrixRooms(emptyList())
            }

            migration.perform(target).getOrThrow()

            val actualInStorage = storageManager.getMatrixRelayServer()

            assertNull(actualInStorage)
        }
    }

    @Test
    fun `sets old default node if migration is needed`() {
        val matrixNodes = BeaconConfiguration.defaultRelayServers
        val target = VersionedMigration.Target.MatrixRelayServer(matrixNodes)

        runBlockingTest {
            with(storageManager) {
                setMatrixRelayServer(null)
                setMatrixSyncToken("token")
            }

            migration.perform(target).getOrThrow()

            val expectedInStorage = MigrationFromV1_0_4.V1_0_4_DEFAULT_NODE
            val actualInStorage = storageManager.getMatrixRelayServer()

            assertEquals(expectedInStorage, actualInStorage)
        }
    }
}