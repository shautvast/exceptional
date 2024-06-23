package com.github.shautvast.exceptional;

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
                return instrumentThrowable(className, classfileBuffer);
            }

            @Override
            public byte[] transform(Module module, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                return instrumentThrowable(className, classfileBuffer);
            }
        }, true);

        byte[] bytecode = Files.readAllBytes(
                Path.of(Agent.class.getResource("/java/lang/Throwable.class").toURI()));

        instrumentation.redefineClasses(new ClassDefinition(Throwable.class, bytecode));
    }

    private static byte[] instrumentThrowable(String className, byte[] classfileBuffer) {
        if (className.equals("java/lang/Throwable")) {
            ClassFile classFile = ClassFile.of();
            ClassModel classModel = classFile.parse(classfileBuffer);
            return instrumentByteCode(classFile, classModel);
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