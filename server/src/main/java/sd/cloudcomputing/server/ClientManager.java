package sd.cloudcomputing.server;

import org.jetbrains.annotations.Nullable;
import sd.cloudcomputing.common.util.AuthenticateResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ClientManager {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<String, Client> clientByNameMap;

    public ClientManager() {
        this.clientByNameMap = new HashMap<>();
    }

    public @Nullable Client getClient(String name) {
        try {
            lock.readLock().lock();
            return clientByNameMap.get(name);
        } finally {
            lock.readLock().unlock();
        }
    }

    public AuthenticateResult authenticateClient(String name, String password) {
        lock.writeLock().lock();
        try {
            Client client = clientByNameMap.get(name);

            if (client == null) {
                clientByNameMap.put(name, new Client(name, password));
                return AuthenticateResult.REGISTERED;
            }

            if (!Objects.equals(client.password(), password)) {
                return AuthenticateResult.WRONG_PASSWORD;
            }

            return AuthenticateResult.LOGGED_IN;
        } finally {
            lock.writeLock().unlock();
        }
    }

}
