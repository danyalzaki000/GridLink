package com.tether.depin

import com.tether.depin.data.local.NodeStats
import com.tether.depin.data.local.NodeStatsDao
import com.tether.depin.data.local.TrafficLog
import com.tether.depin.data.local.TrafficLogDao
import com.tether.depin.data.local.AppDatabase
import com.tether.depin.data.remote.MatchmakerApi
import com.tether.depin.data.remote.RegisterResponse
import com.tether.depin.data.remote.NodeStatusResponse
import com.tether.depin.data.repository.NodeRepository
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class NodeRepositoryTest {

    @MockK
    lateinit var mockApi: MatchmakerApi

    @MockK
    lateinit var mockDatabase: AppDatabase

    @MockK
    lateinit var mockNodeStatsDao: NodeStatsDao

    @MockK
    lateinit var mockTrafficLogDao: TrafficLogDao

    private lateinit var repository: NodeRepository

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        every { mockDatabase.nodeStatsDao() } returns mockNodeStatsDao
        every { mockDatabase.trafficLogDao() } returns mockTrafficLogDao
        every { mockNodeStatsDao.observeStats() } returns flowOf(NodeStats())
        every { mockTrafficLogDao.observeAll() } returns flowOf(emptyList())
        every { mockTrafficLogDao.observeRecent(any()) } returns flowOf(emptyList())

        repository = NodeRepository(mockDatabase, mockApi)
    }

    @Test
    fun `registerNode returns success on valid response`() = runTest {
        val response = Response.success(RegisterResponse(nodeId = "node-123", status = "active"))
        coEvery { mockApi.registerNode(any()) } returns response

        val result = repository.registerNode("192.168.1.1", "7Xf2...kQ9R")

        assertTrue(result.isSuccess)
        assertEquals("node-123", result.getOrNull())
    }

    @Test
    fun `registerNode returns failure on exception`() = runTest {
        coEvery { mockApi.registerNode(any()) } throws Exception("Network error")

        val result = repository.registerNode("192.168.1.1", "wallet123")

        assertTrue(result.isFailure)
    }

    @Test
    fun `logTraffic inserts entry into database`() = runTest {
        coEvery { mockTrafficLogDao.insert(any()) } just Runs

        repository.logTraffic(10.0, 0.03, "Rajabazar Grid")

        coVerify {
            mockTrafficLogDao.insert(match { log ->
                log.mbTransferred == 10.0 &&
                log.usdcEarned == 0.03 &&
                log.destinationNode == "Rajabazar Grid"
            })
        }
    }

    @Test
    fun `addBytesAndEarnings updates node stats`() = runTest {
        coEvery { mockNodeStatsDao.addBytesAndEarnings(any(), any()) } just Runs

        repository.addBytesAndEarnings(10_485_760L, 0.03)

        coVerify { mockNodeStatsDao.addBytesAndEarnings(10_485_760L, 0.03) }
    }

    @Test
    fun `pingStatus returns active status`() = runTest {
        val response = Response.success(NodeStatusResponse(isActive = true, connectedPeers = 3, latencyMs = 24))
        coEvery { mockApi.getNodeStatus() } returns response

        val result = repository.pingStatus()

        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrNull())
    }
}
