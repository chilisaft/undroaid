package com.chilisaft.undroid.data.repository

import com.apollographql.apollo.ApolloClient
import com.chilisaft.undroid.data.models.PciDevice
import com.chilisaft.undroid.data.models.PciDeviceKind
import com.chilisaft.undroid.data.models.UsbDevice
import com.chilisaft.undroid.data.models.WidgetResult
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class DevicesRepositoryTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var repository: DevicesRepository

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        repository = DevicesRepository(ApolloClient.Builder().serverUrl(mockWebServer.url("/").toString()).build())
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    private fun pci(address: String, typeid: String, product: String?, vendor: String? = "Intel") =
        """{"id": "pci/$address", "typeid": "$typeid", "vendorname": ${vendor?.let { "\"$it\"" }}, "productname": ${product?.let { "\"$it\"" }}}"""

    /** A real server's PCI list (Unraid 7.3.4, ASRock H610M-ITX/ac with an i3-12100). */
    private val realPci = listOf(
        pci("00:00.0", "0600", "Device"),
        pci("00:01.0", "0604", "12th Gen Core Processor PCIe x16 Controller #1"),
        pci("00:02.0", "0300", "UHD Graphics 730"),
        pci("00:14.0", "0c03", "600 Series Chipset Family USB 3.2 Gen 2x2 (20Gbs) XHCI Host Controller"),
        pci("00:14.2", "0500", "600 Series Chipset Family Shared SRAM"),
        pci("00:15.0", "0c80", "600 Series Chipset Family I2C Controller #0"),
        pci("00:16.0", "0780", "600 Series Chipset Family CSME HECI #1"),
        pci("00:17.0", "0106", "600 Series Chipset Family SATA Controller (AHCI)"),
        pci("00:1c.0", "0604", "600 Series Chipset Family PCIe Root Port #1"),
        pci("00:1c.7", "0604", "600 Series Chipset Family PCIe Root Port #8"),
        pci("00:1f.0", "0601", "H610 Chipset eSPI Controller"),
        pci("00:1f.4", "0c05", "600 Series Chipset Family SMBus"),
        pci("00:1f.5", "0c80", "600 Series Chipset Family SPI (flash) Controller"),
        pci("00:1f.6", "0200", "Ethernet Connection (17) I219-V"),
        pci("01:00.0", "0108", "NVMe SSD Controller MAP1202 (DRAM-less)", vendor = "MAXIO Technology (Hangzhou) Ltd."),
        pci("02:00.0", "0108", "NVMe SSD Controller MAP1202 (DRAM-less)", vendor = "MAXIO Technology (Hangzhou) Ltd."),
        pci("03:00.0", "0280", "Dual Band Wireless-AC 3168NGW [Stone Peak]")
    )

    @Test
    fun `classifies a real server's PCI devices by class code - 10 of 17 are core`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"info": {"devices": {"pci": [${realPci.joinToString()}]}}}}"""))

        val devices = (repository.getPciDevices() as WidgetResult.Success).data
        val visible = devices.filter { it.kind != PciDeviceKind.CORE }

        assertThat(devices.count { it.kind == PciDeviceKind.CORE }).isEqualTo(10)
        assertThat(visible.map { it.address to it.kind }).containsExactly(
            "00:02.0" to PciDeviceKind.GRAPHICS,
            "00:14.0" to PciDeviceKind.USB_CONTROLLER,
            "00:17.0" to PciDeviceKind.STORAGE,
            "00:1f.6" to PciDeviceKind.NETWORK,
            "01:00.0" to PciDeviceKind.STORAGE,
            "02:00.0" to PciDeviceKind.STORAGE,
            "03:00.0" to PciDeviceKind.NETWORK
        ).inOrder()
    }

    @Test
    fun `a generic "Device" product name falls back to the class name`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"info": {"devices": {"pci": [${pci("00:00.0", "0600", "Device")}]}}}}"""))

        val device = (repository.getPciDevices() as WidgetResult.Success).data.single()

        assertThat(device).isEqualTo(PciDevice("00:00.0", PciDeviceKind.CORE, "Host bridge", "Intel"))
    }

    @Test
    fun `tolerates missing names and an unknown class`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"info": {"devices": {"pci": [${pci("05:00.0", "0403", null, vendor = " ")}]}}}}"""))

        val device = (repository.getPciDevices() as WidgetResult.Success).data.single()

        assertThat(device).isEqualTo(PciDevice("05:00.0", PciDeviceKind.OTHER, "PCI device", null))
    }

    @Test
    fun `a null PCI list reads as no devices`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"info": {"devices": {"pci": null}}}}"""))

        assertThat(repository.getPciDevices()).isEqualTo(WidgetResult.Success(emptyList<PciDevice>()))
    }

    @Test
    fun `the server's own failure on a list is reported as a failure, not an empty list`() = runBlocking {
        // What a real server sent for the full field set: one null non-nullable field fails the list.
        mockWebServer.enqueue(
            MockResponse().setBody("""{"data": {"info": {"devices": {"pci": null}}}, "errors": [{"message": "Cannot return null for non-nullable field InfoPci.type."}]}""")
        )

        val result = repository.getPciDevices() as WidgetResult.Failure

        assertThat(result.permissionDenied).isFalse()
    }

    @Test
    fun `USB root hubs are core devices`() = runBlocking {
        mockWebServer.enqueue(
            MockResponse().setBody(
                """{"data": {"info": {"devices": {"usb": [
                {"name": "Linux Foundation 2.0 root hub"}, {"name": "ASRock LED Controller"}, {"name": "SanDisk Corp. Ultra Fit"}
                ]}}}}"""
            )
        )

        val devices = (repository.getUsbDevices() as WidgetResult.Success).data

        assertThat(devices).containsExactly(
            UsbDevice("Linux Foundation 2.0 root hub", isCore = true),
            UsbDevice("ASRock LED Controller", isCore = false),
            UsbDevice("SanDisk Corp. Ultra Fit", isCore = false)
        ).inOrder()
    }
}
