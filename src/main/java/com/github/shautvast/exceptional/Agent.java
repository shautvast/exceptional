package com.github.shautvast.exceptional;

import java.io.InputStream;
import java.lang.classfile.*;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
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
        byte[] b = new byte[stream.available()];
        stream.read(b);

        instrumentation.redefineClasses(new ClassDefinition(Throwable.class, b));
    }

    private static byte[] instrumentTheThrowable(String className, byte[] classfileBuffer) {
        if (className.equals("java/lang/Throwable")) {

            ClassFile classFile = ClassFile.of();
            ClassModel classModel = classFile.parse(classfileBuffer);

            return classFile.build(classModel.thisClass().asSymbol(), classBuilder -> {
                for (ClassElement ce : classModel) {
                    if (ce instanceof MethodModel mm) {
                        String signature = mm.methodName() + "." + mm.methodType().stringValue();
                        if (signature.equals("<init>.()V")) {
                            classBuilder.withMethod(mm.methodName(), mm.methodType(), 1,
                                    methodBuilder -> methodBuilder.withCode(
                                            codebuilder ->
//                                                codebuilder.invokeInstruction(Opcode.LDC, );
                                                codebuilder.invokeInstruction(Opcode.INVOKESTATIC,
                                                        ClassDesc.of("com.github.shautvast.exceptional.Agent"), "foo",
                                                        MethodTypeDesc.ofDescriptor("()V"),
                                                        false)
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

    public static void foo() {
        System.out.println("foo");
    }

}