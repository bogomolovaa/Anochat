package bogomolov.aa.anochat.android

import android.util.Log
import de.javawi.jstun.attribute.*
import de.javawi.jstun.header.MessageHeader
import de.javawi.jstun.header.MessageHeaderInterface
import de.javawi.jstun.test.DiscoveryInfo
import java.net.*

class DiscoveryTest2(
    var sourceIaddress: InetAddress?,
    var sourcePort: Int,
    var stunServer: String?,
    var stunServerPort: Int
) {
    val timeoutInitValue = 300 //ms

    var ma: MappedAddress? = null
    var ca: ChangedAddress? = null
    var nodeNatted = true
    var socketTest1: DatagramSocket? = null
    var di: DiscoveryInfo? = null


    fun test(): DiscoveryInfo? {
        ma = null
        ca = null
        nodeNatted = true
        socketTest1 = null
        di = DiscoveryInfo(sourceIaddress)
        if (test1()) {
            if (test2()) {
                if (test1Redo()) {
                    test3()
                }
            }
        }
        socketTest1!!.close()
        return di
    }

    private fun test1(): Boolean {
        var timeSinceFirstTransmission = 0
        var timeout = timeoutInitValue
        while (true) {
            try { // Test 1 including response
                socketTest1 = DatagramSocket()
                socketTest1!!.reuseAddress = true
                socketTest1!!.bind(InetSocketAddress(sourceIaddress, sourcePort))
                socketTest1!!.connect(InetAddress.getByName(stunServer), stunServerPort)
                socketTest1!!.soTimeout = timeout
                val sendMH =
                    MessageHeader(MessageHeaderInterface.MessageHeaderType.BindingRequest)
                sendMH.generateTransactionID()
                val changeRequest = ChangeRequest()
                sendMH.addMessageAttribute(changeRequest)
                val data = sendMH.bytes
                val send = DatagramPacket(data, data.size)
                socketTest1!!.send(send)
                Log.i("test", "Test 1: Binding Request sent.")
                var receiveMH =
                    MessageHeader()
                while (!receiveMH.equalTransactionID(sendMH)) {
                    val receive =
                        DatagramPacket(ByteArray(200), 200)
                    socketTest1!!.receive(receive)
                    receiveMH = MessageHeader.parseHeader(receive.data)
                    receiveMH.parseAttributes(receive.data)
                }
                ma =
                    receiveMH.getMessageAttribute(MessageAttributeInterface.MessageAttributeType.MappedAddress) as MappedAddress
                ca =
                    receiveMH.getMessageAttribute(MessageAttributeInterface.MessageAttributeType.ChangedAddress) as ChangedAddress
                val ec =
                    receiveMH.getMessageAttribute(MessageAttributeInterface.MessageAttributeType.ErrorCode) as ErrorCode?
                if (ec != null) {
                    di!!.setError(ec.responseCode, ec.reason)
                    Log.i("test", "Message header contains an Errorcode message attribute.")
                    return false
                }
                return if (ma == null || ca == null) {
                    di!!.setError(
                        700,
                        "The server is sending an incomplete response (Mapped Address and Changed Address message attributes are missing). The client should not retry."
                    )
                    Log.i(
                        "test",
                        "Response does not contain a Mapped Address or Changed Address message attribute."
                    )
                    false
                } else {
                    di!!.publicIP = ma!!.address.inetAddress
                    di!!.publicPort = ma!!.port
                    if (ma!!.port == socketTest1!!.localPort && ma!!.address.inetAddress == socketTest1!!.localAddress) {
                        Log.i("test", "Node is not natted.")
                        nodeNatted = false
                    } else {
                        Log.i("test", "Node is natted.")
                    }
                    true
                }
            } catch (ste: SocketTimeoutException) {
                if (timeSinceFirstTransmission < 7900) {
                    Log.i("test", "Test 1: Socket timeout while receiving the response.")
                    timeSinceFirstTransmission += timeout
                    var timeoutAddValue = timeSinceFirstTransmission * 2
                    if (timeoutAddValue > 1600) timeoutAddValue = 1600
                    timeout = timeoutAddValue
                } else { // node is not capable of udp communication
                    Log.i(
                        "test",
                        "Test 1: Socket timeout while receiving the response. Maximum retry limit exceed. Give up."
                    )
                    di!!.setBlockedUDP()
                    Log.i("test", "Node is not capable of UDP communication.")
                    return false
                }
            }
        }
    }

    private fun test2(): Boolean {
        var timeSinceFirstTransmission = 0
        var timeout = timeoutInitValue
        while (true) {
            try { // Test 2 including response
                val sendSocket =
                    DatagramSocket(InetSocketAddress(sourceIaddress, sourcePort))
                sendSocket.connect(InetAddress.getByName(stunServer), stunServerPort)
                sendSocket.soTimeout = timeout
                val sendMH =
                    MessageHeader(MessageHeaderInterface.MessageHeaderType.BindingRequest)
                sendMH.generateTransactionID()
                val changeRequest = ChangeRequest()
                changeRequest.setChangeIP()
                changeRequest.setChangePort()
                sendMH.addMessageAttribute(changeRequest)
                val data = sendMH.bytes
                val send = DatagramPacket(data, data.size)
                sendSocket.send(send)
                Log.i("test", "Test 2: Binding Request sent.")
                val localPort = sendSocket.localPort
                val localAddress = sendSocket.localAddress
                sendSocket.close()
                val receiveSocket =
                    DatagramSocket(localPort, localAddress)
                receiveSocket.connect(ca!!.address.inetAddress, ca!!.port)
                receiveSocket.soTimeout = timeout
                var receiveMH =
                    MessageHeader()
                while (!receiveMH.equalTransactionID(sendMH)) {
                    val receive =
                        DatagramPacket(ByteArray(200), 200)
                    receiveSocket.receive(receive)
                    receiveMH = MessageHeader.parseHeader(receive.data)
                    receiveMH.parseAttributes(receive.data)
                }
                val ec =
                    receiveMH.getMessageAttribute(MessageAttributeInterface.MessageAttributeType.ErrorCode) as ErrorCode?
                if (ec != null) {
                    di!!.setError(ec.responseCode, ec.reason)
                    Log.i("test", "Message header contains an Errorcode message attribute.")
                    return false
                }
                if (!nodeNatted) {
                    di!!.setOpenAccess()
                    Log.i(
                        "test",
                        "Node has open access to the Internet (or, at least the node is behind a full-cone NAT without translation)."
                    )
                } else {
                    di!!.setFullCone()
                    Log.i("test", "Node is behind a full-cone NAT.")
                }
                return false
            } catch (ste: SocketTimeoutException) {
                if (timeSinceFirstTransmission < 7900) {
                    Log.i("test", "Test 2: Socket timeout while receiving the response.")
                    timeSinceFirstTransmission += timeout
                    var timeoutAddValue = timeSinceFirstTransmission * 2
                    if (timeoutAddValue > 1600) timeoutAddValue = 1600
                    timeout = timeoutAddValue
                } else {
                    Log.i(
                        "test",
                        "Test 2: Socket timeout while receiving the response. Maximum retry limit exceed. Give up."
                    )
                    return if (!nodeNatted) {
                        di!!.setSymmetricUDPFirewall()
                        Log.i("test", "Node is behind a symmetric UDP firewall.")
                        false
                    } else { // not is natted
                        // redo test 1 with address and port as offered in the changed-address message attribute
                        true
                    }
                }
            }
        }
    }

    private fun test1Redo(): Boolean {
        var timeSinceFirstTransmission = 0
        var timeout = timeoutInitValue
        while (true) { // redo test 1 with address and port as offered in the changed-address message attribute
            try { // Test 1 with changed port and address values
                socketTest1!!.connect(ca!!.address.inetAddress, ca!!.port)
                socketTest1!!.soTimeout = timeout
                val sendMH =
                    MessageHeader(MessageHeaderInterface.MessageHeaderType.BindingRequest)
                sendMH.generateTransactionID()
                val changeRequest = ChangeRequest()
                sendMH.addMessageAttribute(changeRequest)
                val data = sendMH.bytes
                val send = DatagramPacket(data, data.size)
                socketTest1!!.send(send)
                Log.i("test", "Test 1 redo with changed address: Binding Request sent.")
                var receiveMH =
                    MessageHeader()
                while (!receiveMH.equalTransactionID(sendMH)) {
                    val receive =
                        DatagramPacket(ByteArray(200), 200)
                    socketTest1!!.receive(receive)
                    receiveMH = MessageHeader.parseHeader(receive.data)
                    receiveMH.parseAttributes(receive.data)
                }
                val ma2 =
                    receiveMH.getMessageAttribute(MessageAttributeInterface.MessageAttributeType.MappedAddress) as MappedAddress
                val ec =
                    receiveMH.getMessageAttribute(MessageAttributeInterface.MessageAttributeType.ErrorCode) as ErrorCode?
                if (ec != null) {
                    di!!.setError(ec.responseCode, ec.reason)
                    Log.i("test", "Message header contains an Errorcode message attribute.")
                    return false
                }
                if (ma2 == null) {
                    di!!.setError(
                        700,
                        "The server is sending an incomplete response (Mapped Address message attribute is missing). The client should not retry."
                    )
                    Log.i("test", "Response does not contain a Mapped Address message attribute.")
                    return false
                } else {
                    if (ma!!.port != ma2.port || ma!!.address.inetAddress != ma2.address.inetAddress) {
                        di!!.setSymmetric()
                        Log.i("test", "Node is behind a symmetric NAT.")
                        return false
                    }
                }
                return true
            } catch (ste2: SocketTimeoutException) {
                if (timeSinceFirstTransmission < 7900) {
                    Log.i(
                        "test",
                        "Test 1 redo with changed address: Socket timeout while receiving the response."
                    )
                    timeSinceFirstTransmission += timeout
                    var timeoutAddValue = timeSinceFirstTransmission * 2
                    if (timeoutAddValue > 1600) timeoutAddValue = 1600
                    timeout = timeoutAddValue
                } else {
                    Log.i(
                        "test",
                        "Test 1 redo with changed address: Socket timeout while receiving the response.  Maximum retry limit exceed. Give up."
                    )
                    return false
                }
            }
        }
    }


    private fun test3() {
        var timeSinceFirstTransmission = 0
        var timeout = timeoutInitValue
        while (true) {
            try { // Test 3 including response
                val sendSocket =
                    DatagramSocket(InetSocketAddress(sourceIaddress, sourcePort))
                sendSocket.connect(InetAddress.getByName(stunServer), stunServerPort)
                sendSocket.soTimeout = timeout
                val sendMH =
                    MessageHeader(MessageHeaderInterface.MessageHeaderType.BindingRequest)
                sendMH.generateTransactionID()
                val changeRequest = ChangeRequest()
                changeRequest.setChangePort()
                sendMH.addMessageAttribute(changeRequest)
                val data = sendMH.bytes
                val send = DatagramPacket(data, data.size)
                sendSocket.send(send)
                Log.i("test", "Test 3: Binding Request sent.")
                val localPort = sendSocket.localPort
                val localAddress = sendSocket.localAddress
                sendSocket.close()
                val receiveSocket =
                    DatagramSocket(localPort, localAddress)
                receiveSocket.connect(InetAddress.getByName(stunServer), ca!!.port)
                receiveSocket.soTimeout = timeout
                var receiveMH =
                    MessageHeader()
                while (!receiveMH.equalTransactionID(sendMH)) {
                    val receive =
                        DatagramPacket(ByteArray(200), 200)
                    receiveSocket.receive(receive)
                    receiveMH = MessageHeader.parseHeader(receive.data)
                    receiveMH.parseAttributes(receive.data)
                }
                val ec =
                    receiveMH.getMessageAttribute(MessageAttributeInterface.MessageAttributeType.ErrorCode) as ErrorCode?
                if (ec != null) {
                    di!!.setError(ec.responseCode, ec.reason)
                    Log.i("test", "Message header contains an Errorcode message attribute.")
                    return
                }
                if (nodeNatted) {
                    di!!.setRestrictedCone()
                    Log.i("test", "Node is behind a restricted NAT.")
                    return
                }
            } catch (ste: SocketTimeoutException) {
                if (timeSinceFirstTransmission < 7900) {
                    Log.i("test", "Test 3: Socket timeout while receiving the response.")
                    timeSinceFirstTransmission += timeout
                    var timeoutAddValue = timeSinceFirstTransmission * 2
                    if (timeoutAddValue > 1600) timeoutAddValue = 1600
                    timeout = timeoutAddValue
                } else {
                    Log.i(
                        "test",
                        "Test 3: Socket timeout while receiving the response. Maximum retry limit exceed. Give up."
                    )
                    di!!.setPortRestrictedCone()
                    Log.i("test", "Node is behind a port restricted NAT.")
                    return
                }
            }
        }
    }
}