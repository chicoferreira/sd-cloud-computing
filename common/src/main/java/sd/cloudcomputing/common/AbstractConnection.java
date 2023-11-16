package sd.cloudcomputing.common;

import sd.cloudcomputing.common.concurrent.BoundedBuffer;
import sd.cloudcomputing.common.logging.Logger;
import sd.cloudcomputing.common.serialization.*;

import java.io.IOException;
import java.net.Socket;

public abstract class AbstractConnection<W, R> {

    private final Logger logger;
    private final BoundedBuffer<W> writeQueue;
    private final Frost frost;
    private final Class<W> writePacketClass;
    private final Class<R> readPacketClass;
    private FrostSocket socket;
    private boolean running;
    private Thread writeThread;
    private Thread readThread;

    public AbstractConnection(Class<W> writePacketClass, Class<R> readPacketClass, Logger logger, Frost frost) {
        this.writePacketClass = writePacketClass;
        this.readPacketClass = readPacketClass;
        this.logger = logger;
        this.frost = frost;
        this.writeQueue = new BoundedBuffer<>(100);
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

    public void hookSocket(Socket socket) {
        this.socket = new FrostSocket(socket);
    }

    public void startReadWrite() {
        this.running = true;

        this.readThread = new Thread(this::runRead, Thread.currentThread().getName() + "-Read-Thread");
        this.writeThread = new Thread(this::runWrite, Thread.currentThread().getName() + "-Write-Thread");

        this.readThread.start();
        this.writeThread.start();
    }

    public void runRead() {
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
        } catch (IOException e) {
            this.logger.error("Error reading: " + e.getMessage());
            onDisconnect();
        }
    }

    public void enqueuePacket(W packet) {
        try {
            writeQueue.put(packet);
        } catch (InterruptedException e) {
            this.logger.warn("Error enqueuing packet: " + e.getMessage());
        }
    }

    public abstract void handlePacket(R packet);

    public abstract void onDisconnect();

    public boolean isConnected() {
        return running && socket != null && socket.isConnected();
    }

    public W getNextPacketToWrite() throws InterruptedException {
        return writeQueue.take();
    }

    public void runWrite() {
        try {
            while (running) {
                W packet = getNextPacketToWrite();
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
            onDisconnect();
            close();
        } catch (InterruptedException ignored) {
        }
    }

    public void close() {
        try {
            this.running = false;

            socket.close();
            if (readThread != null)
                readThread.join();

            if (writeThread != null)
                writeThread.interrupt();
        } catch (IOException e) {
            this.logger.info("Error closing socket: " + e.getMessage());
        } catch (InterruptedException e) {
            this.logger.info("Error joining threads: " + e.getMessage());
        }
    }
}
