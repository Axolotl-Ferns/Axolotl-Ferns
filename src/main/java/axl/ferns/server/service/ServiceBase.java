package axl.ferns.server.service;

import axl.ferns.server.Priority;

public abstract class ServiceBase {

    public abstract void onEnable();

    public abstract void onDisable();

    public final Priority priority() {
        Service service = this.getClass().getAnnotation(Service.class);
        if (service == null)
            return null;

        return service.priority();
    }

}
