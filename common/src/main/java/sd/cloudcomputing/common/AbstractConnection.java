package sd.cloudcomputing.common;

import sd.cloudcomputing.common.concurrent.BoundedBuffer;
import sd.cloudcomputing.common.logging.Logger;
import sd.cloudcomputing.common.serialization.*;

import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

public abstract class AbstractConnection<W, R> implements AutoCloseable {

    private final Logger logger;
    private final BoundedBuffer<W> writeQueue;
    private final Frost frost;
    private final Class<W> writePacketClass;
    private final Class<R> readPacketClass;
    private final FrostSocket socket;
    private boolean running;
    private Thread writeThread;
    private Thread readThread;

    public AbstractConnection(Class<W> writePacketClass, Class<R> readPacketClass, Logger logger, Frost frost, Socket socket) {
        this(writePacketClass, readPacketClass, logger, frost, new FrostSocket(socket));
    }

    public AbstractConnection(Class<W> writePacketClass, Class<R> readPacketClass, Logger logger, Frost frost, FrostSocket socket) {
        this.writePacketClass = writePacketClass;
        this.readPacketClass = readPacketClass;
        this.logger = logger;
        this.frost = frost;
        this.writeQueue = new BoundedBuffer<>(100);
        this.socket = socket;
    }

    protected Frost getFrost() {
        return frost;
    }

    protected Logger getLogger() {
        return logger;
    }

    public FrostSocket getSocket() {
        return socket;
    }

    public SerializeOutput writeEnd() throws IOException {
        return this.socket.writeEnd();
    }

    public SerializeInput readEnd() throws IOException {
        return this.socket.readEnd();
    }

    protected void startReadWrite() {
        this.running = true;

        this.readThread = new Thread(this::runRead, Thread.currentThread().getName() + "-Read-Thread");
        this.writeThread = new Thread(this::runWrite, Thread.currentThread().getName() + "-Write-Thread");

        this.readThread.start();
        this.writeThread.start();
    }

    private void runRead() {
        try {
            while (running) {
                SerializeInput serializeInput = socket.readEnd();
                try {
                    R packet = frost.readSerializable(readPacketClass, serializeInput);
                    handlePacket(packet);
                } catch (SerializationException e) {
                    this.logger.warn("Error deserializing generic packet: " + e.getMessage());
                }
            }
        } catch (EOFException | SocketException e) {
            disconnect();
        } catch (IOException e) {
            this.logger.error("Error reading: " + e.getMessage());
            disconnect();
        }
    }

    public void enqueuePacket(W packet) {
        try {
            writeQueue.put(packet);
        } catch (InterruptedException e) {
            this.logger.warn("Error enqueuing packet: " + e.getMessage());
        }
    }

    protected abstract void handlePacket(R packet);

    protected abstract void onDisconnect();

    public boolean isConnected() {
        return running && socket.isConnected();
    }

    /**
     * Used when a connection may want to do something with the packet before sending it
     *
     * @param packet Packet to map
     * @return The mapped packet
     */
    protected W mapPacketBeforeSend(W packet) {
        return packet;
    }

    private void runWrite() {
        try {
            while (running) {
                W packet = mapPacketBeforeSend(writeQueue.take());
                SerializeOutput serializeOutput = socket.writeEnd();
                try {
                    frost.writeSerializable(packet, writePacketClass, serializeOutput);
                    frost.flush(serializeOutput);
                } catch (SerializationException e) {
                    this.logger.warn("Error serializing packet: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            this.logger.error("Error writing: " + e.getMessage());
            disconnect();
        } catch (InterruptedException ignored) {
        }
    }

    public void disconnect() {
        if (!isConnected())
            return;

        this.running = false;

        if (readThread != null) {
            readThread.interrupt();
        }

        if (writeThread != null)
            writeThread.interrupt();

        try {
            socket.close();
        } catch (IOException e) {
            this.logger.info("Error closing socket: " + e.getMessage());
        }

        onDisconnect();
    }

    @Override
    public void close() {
        disconnect();
    }

    public void join() {
        try {
            this.readThread.join();
            this.writeThread.join();
        } catch (InterruptedException e) {
            this.logger.warn("Error joining threads: " + e.getMessage());
        }
    }

    public String getAddressWithPort() {
        return socket.getAddressWithPort();
    }
}
