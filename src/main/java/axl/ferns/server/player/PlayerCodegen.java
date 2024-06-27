package axl.ferns.server.player;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlayerCodegen {

    public Class<? extends Player> codegenAdditions(List<Class<? extends PlayerInterface>> additions) {
        try {
            Set<Class<?>> interfacesToImplement = new HashSet<>(additions);

            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            String newClassName = "axl/ferns/server/player/GeneratedPlayer";
            String[] interfaces = interfacesToImplement.stream().map(aInterface -> aInterface.getName().replace('.', '/')).toArray(String[]::new);
            classWriter.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, newClassName, null, "axl/ferns/server/player/Player", interfaces);

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
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "axl/ferns/server/player/Player", "<init>", "()V", false);
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();

            classWriter.visitEnd();
            return (Class<? extends Player>) new PlayerClassLoader().defineClass("axl.server.player.GeneratedPlayer", classWriter.toByteArray());
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
        mv.visitInsn(org.objectweb.asm.Type.getType(fieldDescriptor).getOpcode(Opcodes.IRETURN));
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void addSetter(ClassWriter cw, Method method, String className) {
        PlayerSetter setter = method.getAnnotation(PlayerSetter.class);
        String fieldName = setter.name();
        String fieldDescriptor = getTypeDescriptor(method.getParameterTypes()[0]);
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, method.getName(), "(" + fieldDescriptor + ")V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(org.objectweb.asm.Type.getType(fieldDescriptor).getOpcode(Opcodes.ILOAD), 1);
        mv.visitFieldInsn(Opcodes.PUTFIELD, className, fieldName, fieldDescriptor);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private String getTypeDescriptor(Class<?> clazz) {
        if (clazz == Integer.class) return "I";
        if (clazz == Boolean.class) return "Z";
        if (clazz == Short.class) return "S";
        if (clazz == Long.class) return "J";
        if (clazz == Double.class) return "D";
        if (clazz == Float.class) return "F";
        if (clazz == Byte.class) return "B";
        if (clazz == Character.class) return "C";
        return org.objectweb.asm.Type.getDescriptor(clazz);
    }

    static class PlayerClassLoader extends ClassLoader {

        public Class<?> defineClass(String name, byte[] b) {
            return defineClass(name, b, 0, b.length);
        }

    }

}