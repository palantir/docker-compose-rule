package com.palantir.docker.compose.connection;

import javax.net.ssl.SSLHandshakeException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.util.Objects;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DockerPort {

    private static final Logger log = LoggerFactory.getLogger(DockerPort.class);

    private final String ip;
    private final PortMapping portMapping;

    public DockerPort(String ip, int externalPort, int internalPort) {
        this(ip, new PortMapping(externalPort, internalPort));
    }

    public DockerPort(String ip, PortMapping portMapping) {
        this.ip = ip;
        this.portMapping = portMapping;
    }

    public String getIp() {
        return ip;
    }

    public int getExternalPort() {
        return portMapping.getExternalPort();
    }

    public int getInternalPort() {
        return portMapping.getInternalPort();
    }

    public boolean isListeningNow() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, getExternalPort()), 500);
            log.trace("External Port '{}' on ip '{}' was open", getExternalPort(), ip);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean isHttpResponding(Function<DockerPort, String> urlFunction) {
        URL url;
        try {
            String urlString = urlFunction.apply(this);
            log.trace("Trying to connect to {}", urlString);
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not create URL for connecting to localhost", e);
        }
        try {
            url.openConnection().connect();
            url.openStream().read();
            log.debug("Http connection acquired, assuming port active");
            return true;
        } catch (SocketException e) {
            log.trace("Failed to acquire http connection, assuming port inactive", e);
            return false;
        } catch (FileNotFoundException e) {
            log.debug("Received 404, assuming port active");
            return true;
        } catch (SSLHandshakeException e) {
            log.debug("Received SSL response, assuming port active");
            return true;
        } catch (IOException e) {
            log.trace("Error acquiring http connection, assuming port open but inactive", e);
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, portMapping);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        DockerPort other = (DockerPort) obj;
        return Objects.equals(ip, other.ip) &&
               Objects.equals(portMapping, other.portMapping);
    }

    @Override
    public String toString() {
        return "DockerPort [ip=" + ip + ", portMapping=" + portMapping + "]";
    }

}
