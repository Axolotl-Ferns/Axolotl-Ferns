package axl.ferns.server;

import axl.ferns.network.handler.PacketHandler;
import axl.ferns.network.packet.DataPacket;
import axl.ferns.network.secure.Secure;
import axl.ferns.network.secure.XORSecure;
import axl.ferns.server.event.*;
import axl.ferns.server.event.EventListener;
import axl.ferns.server.event.player.PlayerDisconnectEvent;
import axl.ferns.server.event.server.ServerTickEvent;
import axl.ferns.server.player.Player;
import axl.ferns.server.player.PlayerCodegen;
import axl.ferns.server.player.PlayerInterface;
import axl.ferns.server.service.Service;
import axl.ferns.server.service.ServiceBase;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.logging.Handler;

public final class Server {

    @Getter
    private final int port;

    @Getter
    private final int threadPoolSize;

    public final int ticksPerSecond;

    private static boolean close = false;

    private static Server instance;

    @SneakyThrows
    public Server(List<ServiceBase> services, int port, int threadPoolSize, int ticksPerSecond) throws SocketException {
        Server.instance = this;
        this.port = port;
        this.threadPoolSize = threadPoolSize;
        this.ticksPerSecond = ticksPerSecond;
        this.socket = new DatagramSocket(port);
        this.executor = Executors.newFixedThreadPool(this.threadPoolSize);

        this.loadServices(services);
        this.generatePlayer();
        this.loadPlayers();

        new Thread(this::loadNetwork).start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            close = true;
        }));

        new Thread(() -> {
            while (!close)
                this.tick();
        }).start();
    }

    private final DatagramSocket socket;

    private final ExecutorService executor;

    @Getter
    private final List<Player> players = new ArrayList<>();

    private final List<Class<? extends PlayerInterface>> playerInterfaces = new ArrayList<>();

    public Server registerPlayerAddition(Class<? extends PlayerInterface> playerInterface) {
        this.playerInterfaces.add(playerInterface);
        return this;
    }

    private Constructor<? extends Player> playerConstructor;

    @SneakyThrows
    private Player newPlayer() {
        return playerConstructor.newInstance();
    }

    @SneakyThrows
    private void generatePlayer() {
        this.playerConstructor = new PlayerCodegen().codegenAdditions(playerInterfaces).getConstructor();
    }

    @Getter
    @Setter
    private Secure secure = new XORSecure();

    @Getter
    private final List<PacketHandler> packetHandlers = new ArrayList<>();

    @Getter
    private final List<ServiceBase> services = new ArrayList<>();

    private void loadPlayers() {
        {
            Player player = newPlayer();
            player.setToken("[test-token-1]");
            player.setId(0);
            player.setKey(new byte[]{0x20, -0x34, 0x23, 0x54, -0x23, 0x54, 0x00, -0x37});
            player.setNickname("Tester 1");
            this.players.add(player);
        }
        {
            Player player = newPlayer();
            player.setToken("[test-token-2]");
            player.setId(1);
            player.setKey(new byte[]{0x20, -0x34, 0x23, 0x54, -0x23, 0x54, 0x00, -0x37});
            player.setNickname("Tester 2");
            this.players.add(player);
        }

        // TODO

        Runtime.getRuntime().addShutdownHook(new Thread(this::savePlayers));
    }

    private void savePlayers() {
        // TODO
    }

    private void loadServices(List<ServiceBase> services) {
        services.forEach((service) -> {
            Service serviceAnnotation = service.getClass().getAnnotation(Service.class);
            if (serviceAnnotation == null)
                return;

            System.out.println("[SERVICES] Load service " + serviceAnnotation.name() + " " + serviceAnnotation.version());
            this.services.add(service);
        });

        services.sort(Comparator.comparing(ServiceBase::priority));

        this.services.forEach(ServiceBase::onEnable);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> this.services.forEach(ServiceBase::onDisable)));
    }

    private HashMap<Short, Supplier<DataPacket>> packets = new HashMap<>();

    public Server registerPacket(Short PID, Supplier<DataPacket> constructor) {
        if (packets.containsKey(PID))
            throw new IllegalArgumentException("The PID turned out to be not unique");

        packets.put(PID, constructor);
        return this;
    }

    private void registerHandler(Handler handler) {
        if (handler instanceof PacketHandler)
            this.packetHandlers.add((PacketHandler) handler);

        // TODO other handlers
    }

    private void loadNetwork() {
        try {
            while (!close) {
                DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
                this.socket.receive(packet);


                executor.execute(() -> {
                    for (PacketHandler packetHandler : getPacketHandlers())
                        packetHandler.accept(packet);
                });
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        socket.close();
    }

    private final HashMap<Class<?>, List<EventExecutor>> eventHandlers = new HashMap<>();

    private final EventExecutorGenerator eventExecutorGenerator = new EventExecutorGenerator();

    public Server registerListener(EventListener listener) {
        Arrays.stream(listener.getClass().getMethods()).forEach((method) -> {
            EventHandler eventHandler = method.getAnnotation(EventHandler.class);
            if (eventHandler == null)
                return;

            if (method.getParameterCount() != 1 ||
                    !Modifier.isPublic(method.getModifiers()) ||
                    !Event.class.isAssignableFrom(method.getParameterTypes()[0]))
                throw new IllegalArgumentException("EventHandler must have 1 parameter, which is inherited from " +
                        "'axl.server.event.Event' and must be public: " + listener.getClass().getName() +
                        "::" + method.getName());

            EventExecutor eventExecutor = eventExecutorGenerator.generateEventHandler(listener, method, eventHandler.priority());
            if (!eventHandlers.containsKey(eventExecutor.getArgumentClass()))
                eventHandlers.put(eventExecutor.getArgumentClass(), new ArrayList<>());

            eventHandlers.get(eventExecutor.getArgumentClass()).add(eventExecutor);
        });

        eventHandlers.forEach(((aClass, eventExecutors) -> {
            eventExecutors.sort(Comparator.comparing(EventExecutor::getPriority));
        }));
        return this;
    }

    @Getter
    private long tick = 0;

    @SneakyThrows
    private void tick() {
        ServerTickEvent serverTickEvent = new ServerTickEvent(++tick);
        this.callEvent(serverTickEvent);
        Thread.sleep(1000 / ticksPerSecond);
    }

    public void callEvent(Event event) {
        eventHandlers.forEach(((aClass, eventExecutors) -> {
            if (event.getClass().isAssignableFrom(aClass))
                eventExecutors.forEach(eventExecutor -> eventExecutor.execute(event));
        }));
    }

    @NonNull
    public static Server getInstance() {
        if (instance == null)
            throw new IllegalStateException("The server was not initialized");

        return instance;
    }

    public void disconnect(Player player, String reason) {
        if (player.isOnline()) {
            PlayerDisconnectEvent playerDisconnectEvent = new PlayerDisconnectEvent(player, reason);
            this.callEvent(playerDisconnectEvent);
            player.disconnect(playerDisconnectEvent.getReason());
        }

        players.remove(player);
    }

    public Player getPlayerById(int playerId) {
        for (Player player: this.players)
            if (player.getId() == playerId)
                return player;

        return null;
    }

    public Player getPlayerByToken(String token) {
        for (Player player: players)
            if (player.getToken().equals(token))
                return player;

        return null;
    }

}