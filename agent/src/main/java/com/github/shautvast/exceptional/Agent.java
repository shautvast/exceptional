package com.github.shautvast.exceptional;

import java.lang.classfile.*;
import java.lang.classfile.instruction.ReturnInstruction;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.ProtectionDomain;

public class Agent {

    @SuppressWarnings("DataFlowIssue")
    public static void premain(String agentArgs, Instrumentation instrumentation) throws Exception {
        System.err.println("--->Exceptional agent active");
        // add transformer
        instrumentation.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
                return instrumentThrowable(className, classfileBuffer);
            }

            @Override
            public byte[] transform(Module module, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
                return instrumentThrowable(className, classfileBuffer);
            }
        }, true);

        // we only want to redefine Throwable
        byte[] bytecode = Files.readAllBytes(
                Path.of(Agent.class.getResource("/java/lang/Throwable.class").toURI()));
        instrumentation.redefineClasses(new ClassDefinition(Throwable.class, bytecode));
    }

    private static byte[] instrumentThrowable(String className, byte[] bytecode) {
        // we only want to instrument Throwable
        // or rather,,, is this the right way? This way we also intercept new any Exception that is not thrown
        // But,,, who does that?? (famous last words)
        if (className.equals("java/lang/Throwable")) {
            ClassFile classFile = ClassFile.of();
            ClassModel classModel = classFile.parse(bytecode);
            return instrumentConstructors(classFile, classModel);
        } else {
            return bytecode;
        }
    }

    private static byte[] instrumentConstructors(ClassFile classFile, ClassModel classModel) {
        return classFile.build(classModel.thisClass().asSymbol(), classBuilder -> {
            for (ClassElement ce : classModel) {
                if (ce instanceof MethodModel mm && mm.methodName().toString().equals("<init>")) {
                    instrumentMethodWithMyExceptionLogger(classBuilder, mm);
                    continue;
                }
                classBuilder.with(ce);
            }
        });
    }

    private static void instrumentMethodWithMyExceptionLogger(ClassBuilder classBuilder, MethodModel methodModel) {
        methodModel.code().ifPresent(code ->
                classBuilder.withMethod(methodModel.methodName(), methodModel.methodType(), methodModel.flags().flagsMask(),
                        methodBuilder ->
                                methodBuilder.transformCode(code, ((builder, element) -> {
                                            if (element instanceof ReturnInstruction) {
                                                builder.aload(0); // load `this` on the stack, ie the Throwable instance
                                                builder.invokestatic( //  call my code
                                                        ClassDesc.of("com.github.shautvast.exceptional.ExceptionLogger"), "log",
                                                        MethodTypeDesc.ofDescriptor("(Ljava/lang/Throwable;)V"));
                                                builder.return_();
                                            } else {
                                                builder.with(element); // leave any other bytecode in place
                                            }
                                        })
                                )));
    }
}