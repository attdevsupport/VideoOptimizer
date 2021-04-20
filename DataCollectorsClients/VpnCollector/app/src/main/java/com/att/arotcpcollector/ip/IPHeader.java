package com.att.arotcpcollector.ip;

import java.net.InetAddress;

public interface IPHeader {
    byte getProtocol();
    byte getIpVersion();
    int getIPHeaderLength();
    int getTotalLength();
    int getIdentification();
    InetAddress getSourceIP();
    InetAddress getDestinationIP();

    void setDestinationIP(InetAddress destinationIP);
    void setSourceIP(InetAddress sourceIP);
    void setTotalLength(int totalLength);
    void setIdentification(int identification);
}
