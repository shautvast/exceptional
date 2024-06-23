package com.github.shautvast.exceptional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.*;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;

@SuppressWarnings("ALL")
public class Agent {

    private static final String MESSAGE = "--- Exceptional agent active";

    public static void premain(String agentArgs, Instrumentation instrumentation) throws Exception {
        instrumentation.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
                return instrumentTheThrowable(className, classfileBuffer);
            }

            @Override
            public byte[] transform(Module module, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                return instrumentTheThrowable(className, classfileBuffer);
            }
        }, true);

        InputStream stream = Agent.class.getResourceAsStream("/java/lang/Throwable.class");
        byte[] bytecode = readFully(stream);

        instrumentation.redefineClasses(new ClassDefinition(Throwable.class, bytecode));
    }

    private static byte[] readFully(InputStream stream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = stream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();
        return buffer.toByteArray();
    }

    private static byte[] instrumentTheThrowable(String className, byte[] classfileBuffer) {
        if (className.equals("java/lang/Throwable")) {
            ClassFile classFile = ClassFile.of();
            ClassModel classModel = classFile.parse(classfileBuffer);

            return classFile.build(classModel.thisClass().asSymbol(), classBuilder -> {
                for (ClassElement ce : classModel) {
                    if (ce instanceof MethodModel mm) {
                        String signature = mm.methodName() + mm.methodType().stringValue();
                        if (signature.equals("<init>()V")) {
                            classBuilder.withMethod(mm.methodName(), mm.methodType(), Modifier.PUBLIC,
                                    methodBuilder -> methodBuilder.withCode(
                                            cb -> {
                                                // recreate existing code for this method, because... I don't know how to simply add the new code at the end🫣
                                                ClassDesc throwable = ClassDesc.of("java.lang.Throwable");
                                                cb.aload(0);
                                                cb.invokespecial(ConstantDescs.CD_Object, "<init>", MethodTypeDesc.ofDescriptor("()V"));
                                                cb.aload(0);
                                                cb.aload(0);
                                                cb.putfield(throwable, "cause", ClassDesc.ofDescriptor("Ljava/lang/Throwable;"));
                                                cb.aload(0);
                                                cb.getstatic(throwable, "UNASSIGNED_STACK", ClassDesc.ofDescriptor("[Ljava/lang/StackTraceElement;"));
                                                cb.putfield(throwable, "stackTrace", ClassDesc.ofDescriptor("[Ljava/lang/StackTraceElement;"));
                                                cb.aload(0);
                                                cb.invokevirtual(throwable, "fillInStackTrace", MethodTypeDesc.ofDescriptor("()Ljava/lang/Throwable;"));
                                                cb.pop();
//                                                cb.getstatic(throwable, "jfrTracing", ConstantDescs.CD_Boolean);
//                                                Label end = cb.newLabel();
//                                                cb.ifeq(end);
//                                                cb.aload(0);
//                                                cb.invokevirtual(ConstantDescs.CD_Object, "getClass", MethodTypeDesc.ofDescriptor("()Ljava/lang/Class;"));
//                                                cb.aconst_null();
//                                                cb.invokestatic(ClassDesc.of("jdk.internal.event.ThrowableTracer"), "traceThrowable", MethodTypeDesc.ofDescriptor("(Ljava/lang/Class;Ljava/lang/String;)V"));
//                                                cb.labelBinding(end);

                                                cb.aload(0);
                                                cb.invokestatic(
                                                        ClassDesc.of("com.github.shautvast.exceptional.ThrowableHandler"), "handle",
                                                        MethodTypeDesc.ofDescriptor("(Ljava/lang/Throwable;)V"));
                                                cb.return_();
                                            }
                                    ));
                            continue;
                        }
                    }
                    classBuilder.with(ce);
                }
            });
        } else {
            return classfileBuffer;
        }
    }
}

//getstatic     #7                  // Field java/lang/System.out:Ljava/io/PrintStream;
//ldc           #13                 // class com/github/shautvast/exceptional/Foo
//invokevirtual #15                 // Method java/lang/Class.getModule:()Ljava/lang/Module;
//invokevirtual #21                 // Method java/lang/Module.getName:()Ljava/lang/String;
//invokevirtual #27                 // Method java/io/PrintStream.println:(Ljava/lang/String;)V
