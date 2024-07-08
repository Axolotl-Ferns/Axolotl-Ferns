package axl.ferns.server;

import axl.ferns.network.handler.PacketHandler;
import axl.ferns.network.packet.DataPacket;
import axl.ferns.network.secure.Secure;
import axl.ferns.network.secure.XORSecure;
import axl.ferns.server.event.EventListener;
import axl.ferns.server.event.*;
import axl.ferns.server.event.player.PlayersLoadEvent;
import axl.ferns.server.event.player.PlayersSaveEvent;
import axl.ferns.server.event.server.ServerTickEvent;
import axl.ferns.server.player.Player;
import axl.ferns.server.player.PlayerCodegen;
import axl.ferns.server.player.PlayerConstructor;
import axl.ferns.server.player.PlayerInterface;
import axl.ferns.server.service.Service;
import axl.ferns.server.service.ServiceBase;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public final class Server {

    @Getter
    private final int port;

    @Getter
    private final int threadPoolSize;

    public final int ticksPerSecond;

    private static boolean close = false;

    private static Server instance;

    @SneakyThrows
    public Server(List<ServiceBase> services, int port, int threadPoolSize, int ticksPerSecond) {
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
            savePlayers();
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

    private PlayerConstructor playerConstructor;

    @SneakyThrows
    public Player newPlayer() {
        return playerConstructor.newInstance();
    }

    @SneakyThrows
    private void generatePlayer() {
        this.playerConstructor = new PlayerCodegen().codegenAdditions(playerInterfaces);
    }

    @Getter
    @Setter
    private Secure secure = new XORSecure();

    private void loadPlayers() {
        PlayersLoadEvent event = new PlayersLoadEvent();
        callEvent(event);
        this.players.addAll(event.getPlayers());
        Runtime.getRuntime().addShutdownHook(new Thread(this::savePlayers));
    }

    private void savePlayers() {
        callEvent(new PlayersSaveEvent());
    }

    @Getter
    private final List<ServiceBase> services = new ArrayList<>();

    private void loadServices(List<ServiceBase> services) {
        if (services == null || services.isEmpty())
            return;

        services.forEach((service) -> {
            Service serviceAnnotation = service.getClass().getAnnotation(Service.class);
            if (serviceAnnotation == null)
                throw new IllegalArgumentException("Service \"" + service.getClass().getName() + "\" does not have an @axl.ferns.server.service.Service annotation");

            this.services.add(service);
        });

        services.sort(Comparator.comparing(ServiceBase::priority));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> this.services.forEach(ServiceBase::onDisable)));
        this.services.forEach(ServiceBase::onEnable);
    }

    public ServiceBase getService(Class<? extends ServiceBase> service) {
        for (ServiceBase base: services)
            if (base.getClass().isAssignableFrom(service))
                return base;

        return null;
    }

    @Getter
    private final HashMap<Short, Supplier<DataPacket>> packets = new HashMap<>();

    public Server registerPacket(Short PID, Supplier<DataPacket> constructor) {
        if (packets.containsKey(PID))
            throw new IllegalArgumentException("The PID turned out to be not unique");

        packets.put(PID, constructor);
        return this;
    }

    @Getter
    private final List<PacketHandler> packetHandlers = new ArrayList<>();

    public Server registerPacketHandler(PacketHandler handler) {
        this.packetHandlers.add(handler);
        return this;
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
