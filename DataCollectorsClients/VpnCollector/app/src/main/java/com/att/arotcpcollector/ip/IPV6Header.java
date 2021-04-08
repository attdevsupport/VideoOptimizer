package com.att.arotcpcollector.ip;

import android.util.Log;

import com.att.arotcpcollector.tcp.PacketHeaderException;
import com.att.arotcpcollector.util.PacketUtil;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * Data structure for IPv6 header
 * @author arpitbansal (ab090c)
 */
public class IPV6Header implements IPHeader {
    public static final String TAG = "IPV6Header";

    // 40 bytes is the fixed IP v6 header length excluding Extension and Upper Protocol (TC/UDP etc) headers
    public static final byte INITIAL_FIXED_HEADER_LENGTH = 40;

    // 4-bit field
    private byte ipVersion;

    private int identification;

    // Identifies the upper layer protocol if present, or the last next header
    private byte protocol;

    // 8-bit field. This is similar to the IPv4 Type of Service field.
    private byte trafficClass;

    // 20-bit field. Indicates that this packet belongs to a specific sequence of packets between a source and a destination.
    // For default router handling, the value is 0.
    private int flowLabel;

    // 16-bit field. This length includes the extension headers and the upper layer's protocol length.
    // For payload greater than 65,535 bytes, the field is set to 0 and Jumbo Payload option is used in the Hop-by-Hop Options extension header.
    private short payloadLength;

    // Total length of packet including extension headers but excluding TCP header + TCP data length
    private int internetHeaderLength;

    // Total length of the packet including TCP header + TCP data length
    private int totalLength;

    // 8-bit field. Indicates the first extension header or the upper layer's protocol.
    // In IPv6, this field represents a chain of extension headers (if present), until an upper layer protocol is specified.
    // Currently, we only care about TCP(6) and UDP(17) for the upper later protocols.
    private byte nextHeader;

    // 8-bit field. Similar to TTL field in IPv4.
    // This field represents the maximum number of links over which this packet can travel before being discarded.
    private byte hopLimit;

    // 128-bit field. Source IP address.
    private InetAddress sourceIP;

    // 128-bit field. Destination IP address.
    private InetAddress destinationIP;

    private byte[] extensionHeadersData = new byte[0];


    public IPV6Header(byte[] packetData, int offset) throws PacketHeaderException {
        if ((packetData.length - offset) < INITIAL_FIXED_HEADER_LENGTH) {
            throw new PacketHeaderException("Minimum IPv6 header is 40 bytes. There are less than 40 bytes"
                    + " from start position to the end of array.");
        }

        ipVersion = (byte) (packetData[offset] >> 4);
        if (ipVersion != 0x06) {
            throw new PacketHeaderException("Invalid IPv6 header. IP version should be 6.");
        }

        trafficClass = (byte) ((packetData[offset] << 4) | (packetData[offset + 1] >> 4));
        flowLabel = PacketUtil.getNetworkInt(packetData, offset + 1, 3) & 0x000fffff;
        payloadLength = PacketUtil.getNetworkShort(packetData, offset + 4);
        protocol = nextHeader = packetData[offset + 6];
        hopLimit = packetData[offset + 7];
        try {
            sourceIP = InetAddress.getByAddress(Arrays.copyOfRange(packetData, offset + 8, offset + 24));
            destinationIP = InetAddress.getByAddress(Arrays.copyOfRange(packetData, offset + 24, offset + 40));
        } catch (UnknownHostException e) {
            Log.e(TAG, "Unable to extract source or destination IP", e);
        }

        int extensionHeadersLength = calculateLengthOfExtensionHeaders(packetData, offset + INITIAL_FIXED_HEADER_LENGTH);
        Log.e("TAG", "Initial Protocol: " + protocol);
        if (extensionHeadersLength % 64 != 0) {
            throw new PacketHeaderException("extension headers are supposed to be an integer multiple of 8 octets long (current length " + extensionHeadersLength +") ");
        }
        if (extensionHeadersLength != 0) {
            extensionHeadersData = new byte[extensionHeadersLength];
            System.arraycopy(packetData, offset + INITIAL_FIXED_HEADER_LENGTH, extensionHeadersData, 0, extensionHeadersLength);
        }

        internetHeaderLength = INITIAL_FIXED_HEADER_LENGTH + extensionHeadersLength;
        totalLength = INITIAL_FIXED_HEADER_LENGTH + payloadLength;
    }

    public IPV6Header(IPV6Header header) {
        ipVersion = header.getIpVersion();
        identification = header.getIdentification();
        protocol = header.getProtocol();
        trafficClass = header.getTrafficClass();
        flowLabel = header.getFlowLabel();
        payloadLength = header.getPayloadLength();
        nextHeader = header.getNextHeader();
        hopLimit = header.getHopLimit();
        sourceIP = header.getSourceIP();
        destinationIP = header.getDestinationIP();
        extensionHeadersData = Arrays.copyOf(header.getExtensionHeadersData(), header.getExtensionHeadersData().length);
        internetHeaderLength = header.getInternetHeaderLength();
        totalLength = header.getTotalLength();
    }

    public byte getIpVersion() {
        return ipVersion;
    }

    public byte getTrafficClass() {
        return trafficClass;
    }

    public int getFlowLabel() {
        return flowLabel;
    }

    public short getPayloadLength() {
        return payloadLength;
    }

    public byte getNextHeader() {
        return nextHeader;
    }

    public byte getHopLimit() {
        return hopLimit;
    }

    public void setHopLimit(byte hopLimit) {
        this.hopLimit = hopLimit;
    }

    @Override
    public InetAddress getSourceIP() {
        return sourceIP;
    }

    @Override
    public void setSourceIP(InetAddress sourceIP) {
        this.sourceIP = sourceIP;
    }

    @Override
    public InetAddress getDestinationIP() {
        return destinationIP;
    }

    @Override
    public void setDestinationIP(InetAddress destinationIP) {
        this.destinationIP = destinationIP;
    }

    @Override
    public int getIPHeaderLength() {
        return internetHeaderLength;
    }

    public void setPayloadLength(short payloadLength) {
        this.payloadLength = payloadLength;
    }

    @Override
    public byte getProtocol() {
        return protocol;
    }

    @Override
    public int getTotalLength() {
        return totalLength;
    }

    @Override
    public void setTotalLength(int totalLength) {
        this.totalLength = totalLength;
    }

    @Override
    public int getIdentification() {
        return identification;
    }

    @Override
    public void setIdentification(int identification) {
        this.identification = identification;
    }

    public byte[] getExtensionHeadersData() {
        return extensionHeadersData;
    }

    public int getInternetHeaderLength() {
        return internetHeaderLength;
    }


    /**
     * Calculate total length of extension headers until an Upper Layer Protocol i.e. TCP(6) or UDP(17) is encountered
     * OR No Next Header (59) is found
     *
     * Currently Hop By Hop (0), Routing (43) and Destination Options (60) headers are supported.
     * TODO: Implement support for Fragment (44) and Encapsulating Security Payload (50) headers
     * @param buffer
     * @param start
     * @return Total length of extension headers present
     * @throws PacketHeaderException
     */
    private int calculateLengthOfExtensionHeaders(byte[] buffer, int start) throws PacketHeaderException {
        int headerLength = 0;
        switch (protocol) {
            case 0: // Hop by Hop Options Header
                // TODO: Use Routing Header's destination address in checksum calculation if it exists
            case 43: // Routing Header
            case 51: // Authentication Header
            case 60: // Destination Options Header
                if (start >= buffer.length - 1) {
                    throw new PacketHeaderException("Insufficient data for header " + protocol);
                }

                headerLength += (protocol == 51 ? (buffer[start + 1] + 2) * 4 : (buffer[start + 1] * 8) + 8);
                protocol = buffer[start];
                headerLength += calculateLengthOfExtensionHeaders(buffer, start + headerLength);
                break;
            case 59: // No Next header. This signifies that there exists nothing after the corresponding header
            case 6: // TCP
            case 17: // UDP
                break;
            default:
                throw new PacketHeaderException("Protocol " + protocol + " not supported");
        }

        return headerLength;
    }
}
