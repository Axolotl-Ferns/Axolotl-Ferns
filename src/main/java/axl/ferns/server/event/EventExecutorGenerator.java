package axl.ferns.server.event;

import axl.ferns.server.Priority;
import lombok.SneakyThrows;
import org.objectweb.asm.*;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

public class EventExecutorGenerator {

    private final AtomicInteger handlerCounter = new AtomicInteger(0);

    @SneakyThrows
    public EventExecutor generateEventHandler(EventListener listener, Method method, Priority priority) {
        String listenerClassName = listener.getClass().getName().replace('.', '/');
        String className = "EventExecutor" + handlerCounter.incrementAndGet() + "A";
        String handlerClassName = "axl/ferns/server/event/" + className;
        String handlerClassBinaryName = handlerClassName.replace('/', '.');  // Convert to binary name

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        cw.visit(Opcodes.V9,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                handlerClassName,
                null,
                "java/lang/Object",
                new String[]{Type.getInternalName(EventExecutor.class)});

        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "execute", "(Laxl/server/event/Event;)V", null, null);
        mv.visitCode();

        Class<?> paramType = method.getParameterTypes()[0];

        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(paramType));
        mv.visitVarInsn(Opcodes.ASTORE, 2);

        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, handlerClassName, "listener", "L" + listenerClassName + ";");
        mv.visitVarInsn(Opcodes.ALOAD, 2);

        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, listenerClassName, method.getName(), Type.getMethodDescriptor(method), false);

        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(3, 3);
        mv.visitEnd();

        cw.visitField(Opcodes.ACC_PRIVATE, "listener", "L" + listenerClassName + ";", null, null).visitEnd();
        cw.visitField(Opcodes.ACC_PRIVATE, "argumentClass", "Ljava/lang/Class;", null, null).visitEnd();
        cw.visitField(Opcodes.ACC_PRIVATE, "priority", "Laxl/server/Priority;", null, null).visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(L" + listenerClassName + ";Ljava/lang/Class;Laxl/server/Priority;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitFieldInsn(Opcodes.PUTFIELD, handlerClassName, "listener", "L" + listenerClassName + ";");
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitFieldInsn(Opcodes.PUTFIELD, handlerClassName, "argumentClass", "Ljava/lang/Class;");
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        mv.visitFieldInsn(Opcodes.PUTFIELD, handlerClassName, "priority", "Laxl/server/Priority;");
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(4, 3);
        mv.visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "getArgumentClass", "()Ljava/lang/Class;", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, handlerClassName, "argumentClass", "Ljava/lang/Class;");
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "getPriority", "()Laxl/server/Priority;", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, handlerClassName, "priority", "Laxl/server/Priority;");
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        cw.visitEnd();
        Class<?> eventHandler = loader.defineClass(handlerClassBinaryName, cw.toByteArray());  // Use binary name here
        return (EventExecutor) eventHandler.getConstructor(listener.getClass(), Class.class, Priority.class)
                .newInstance(listener, paramType, priority);
    }

    private static final EventHandlerClassLoader loader = new EventHandlerClassLoader();

    private static class EventHandlerClassLoader extends ClassLoader {
        public Class<?> defineClass(String name, byte[] b) {
            return defineClass(name, b, 0, b.length);
        }
    }

}
