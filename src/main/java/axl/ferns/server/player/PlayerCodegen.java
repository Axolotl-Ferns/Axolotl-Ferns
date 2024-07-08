package axl.ferns.server.player;

import lombok.Getter;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.objectweb.asm.Type.getType;

public class PlayerCodegen {

    @Getter
    private final PlayerClassLoader loader = new PlayerClassLoader();

    private final String PLAYER_INTERNAL = "axl/ferns/server/player/Player";
    private final String PLAYER_GENERATED_INTERNAL = "axl/ferns/server/player/GeneratedPlayer";

    private final String PLAYER_CONSTRUCTOR_INTERNAL = "axl/ferns/server/player/PlayerConstructor";
    private final String GENERATED_PLAYER_CONSTRUCTOR_INTERNAL = "axl/ferns/server/player/GeneratedPlayerConstructor";

    public PlayerConstructor codegenAdditions(List<Class<? extends PlayerInterface>> additions) {

        Class<? extends Player> player = generatePlayer(additions);
        if (player == null)
            return null;

        try {
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            String newClassName = GENERATED_PLAYER_CONSTRUCTOR_INTERNAL;
            String[] interfaces = new String[]{PLAYER_CONSTRUCTOR_INTERNAL};
            classWriter.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, newClassName, null, "java/lang/Object", interfaces);

            MethodVisitor mv = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();

            mv = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "newInstance", "()L" + PLAYER_INTERNAL + ";", null, null);
            mv.visitCode();
            mv.visitTypeInsn(Opcodes.NEW, PLAYER_GENERATED_INTERNAL);
            mv.visitInsn(Opcodes.DUP);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, PLAYER_GENERATED_INTERNAL, "<init>", "()V", false);
            mv.visitTypeInsn(Opcodes.CHECKCAST, PLAYER_INTERNAL);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(2, 1);
            mv.visitEnd();

            classWriter.visitEnd();
            Class<? extends PlayerConstructor> playerConstructor = (Class<? extends PlayerConstructor>) loader.defineClass(GENERATED_PLAYER_CONSTRUCTOR_INTERNAL.replace("/", "."), classWriter.toByteArray());
            if (playerConstructor == null)
                return null;

            return playerConstructor.getConstructor().newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Class<? extends Player> generatePlayer(List<Class<? extends PlayerInterface>> additions) {
        try {
            Set<Class<?>> interfacesToImplement = new HashSet<>(additions);

            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            String newClassName = PLAYER_GENERATED_INTERNAL;
            String[] interfaces = interfacesToImplement.stream().map(aInterface -> aInterface.getName().replace('.', '/')).toArray(String[]::new);
            classWriter.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, newClassName, null, PLAYER_INTERNAL, interfaces);

            for (Class<? extends PlayerInterface> additionClass : additions) {
                if (additionClass.isAnnotationPresent(PlayerAdditions.class)) {
                    PlayerAdditions playerAdditions = additionClass.getAnnotation(PlayerAdditions.class);
                    for (PlayerField field : playerAdditions.fields()) {
                        addField(classWriter, field);
                    }
                    for (Method method : additionClass.getDeclaredMethods()) {
                        if (method.isAnnotationPresent(PlayerGetter.class)) {
                            addGetter(classWriter, method, newClassName);
                        } else if (method.isAnnotationPresent(PlayerSetter.class)) {
                            addSetter(classWriter, method, newClassName);
                        }
                    }
                }
            }

            MethodVisitor mv = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, PLAYER_INTERNAL, "<init>", "()V", false);
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();

            classWriter.visitEnd();
            return (Class<? extends Player>) loader.defineClass(PLAYER_GENERATED_INTERNAL.replace("/", "."), classWriter.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void addField(ClassWriter cw, PlayerField field) {
        String fieldTypeDescriptor = getTypeDescriptor(field.type());
        cw.visitField(Opcodes.ACC_PRIVATE, field.name(), fieldTypeDescriptor, null, null).visitEnd();
    }

    private void addGetter(ClassWriter cw, Method method, String className) {
        PlayerGetter getter = method.getAnnotation(PlayerGetter.class);
        String fieldName = getter.name();
        String fieldDescriptor = getTypeDescriptor(method.getReturnType());
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, method.getName(), "()" + fieldDescriptor, null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, className, fieldName, fieldDescriptor);
        mv.visitInsn(getType(fieldDescriptor).getOpcode(Opcodes.IRETURN));
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void addSetter(ClassWriter cw, Method method, String className) {
        PlayerSetter setter = method.getAnnotation(PlayerSetter.class);
        String fieldName = setter.name();
        String fieldDescriptor = getTypeDescriptor(method.getParameterTypes()[0]);

        String returnDescriptor = getTypeDescriptor(method.getReturnType());
        if (Player.class.isAssignableFrom(method.getReturnType()) || PlayerInterface.class.isAssignableFrom(method.getReturnType())) {
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, method.getName(), "(" + fieldDescriptor + ")" + returnDescriptor, null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(getType(fieldDescriptor).getOpcode(Opcodes.ILOAD), 1);
            mv.visitFieldInsn(Opcodes.PUTFIELD, className, fieldName, fieldDescriptor);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(method.getReturnType()));
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
            return;
        }

        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, method.getName(), "(" + fieldDescriptor + ")V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(getType(fieldDescriptor).getOpcode(Opcodes.ILOAD), 1);
        mv.visitFieldInsn(Opcodes.PUTFIELD, className, fieldName, fieldDescriptor);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private String getTypeDescriptor(Class<?> clazz) {
        return org.objectweb.asm.Type.getDescriptor(clazz);
    }

    static class PlayerClassLoader extends ClassLoader {

        public Class<?> defineClass(String name, byte[] b) {
            return defineClass(name, b, 0, b.length);
        }

    }

}