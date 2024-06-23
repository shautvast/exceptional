package com.github.shautvast.exceptional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.ClassElement;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.instruction.ReturnInstruction;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
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
            byte[] bytes = instrumentByteCode(classFile, classModel);
            try {
                Files.write(Path.of("MyThrowable.class"), bytes);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return bytes;
        } else {
            return classfileBuffer;
        }
    }

    private static byte[] instrumentByteCode(ClassFile classFile, ClassModel classModel) {
        return classFile.build(classModel.thisClass().asSymbol(), classBuilder -> {
            for (ClassElement ce : classModel) {
                if (ce instanceof MethodModel mm) {
                    if (mm.methodName().toString().equals("<init>")) {
                        classBuilder.withMethod(mm.methodName(), mm.methodType(), mm.flags().flagsMask(),
                                methodBuilder ->
                                        methodBuilder.transformCode(mm.code().get(), ((builder, element) -> {
                                                    if (element instanceof ReturnInstruction) {
                                                        builder.aload(0);
                                                        builder.invokestatic(
                                                                ClassDesc.of("com.github.shautvast.exceptional.ThrowableHandler"), "handle",
                                                                MethodTypeDesc.ofDescriptor("(Ljava/lang/Throwable;)V"));
                                                        builder.return_();
                                                    } else {
                                                        builder.with(element);
                                                    }
                                                })
                                        ));
                        continue;
                    }
                }
                classBuilder.with(ce);
            }
        });
    }
}