package com.github.shautvast.exceptional;

import java.lang.classfile.ClassBuilder;
import java.lang.classfile.ClassElement;
import java.lang.classfile.ClassFile;
import java.lang.classfile.MethodModel;
import java.lang.classfile.instruction.ThrowInstruction;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.AccessFlag;
import java.security.ProtectionDomain;

public class Agent {

    private static final String EXCEPTIONLOGGER = ExceptionLogger.class.getName();

    public static void premain(String agentArgs, Instrumentation instrumentation) throws Exception {
        System.err.println("--->Exceptional agent active");
        // add transformer
        instrumentation.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
                return injectExceptionLoggerBeforeThrow(className, classfileBuffer);
            }

            @Override
            public byte[] transform(Module module, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
                return injectExceptionLoggerBeforeThrow(className, classfileBuffer);
            }
        }, true);
    }

    /*
     * Every throw opcode will be preceded by a call to our ExceptionLogger
     */
    private static byte[] injectExceptionLoggerBeforeThrow(String className, byte[] classfileBuffer) {
        var classFile = ClassFile.of();
        var classModel = classFile.parse(classfileBuffer);
        return classFile.build(classModel.thisClass().asSymbol(), classBuilder -> {
            for (ClassElement ce : classModel) {
                // not all methods, TODO include synthetic?
                if (ce instanceof MethodModel methodModel && !methodModel.flags().has(AccessFlag.ABSTRACT)
                        && !methodModel.flags().has(AccessFlag.NATIVE)
                        && !methodModel.flags().has(AccessFlag.BRIDGE)) {
                    transform(classBuilder, methodModel);
                } else {
                    // keep all other class elements
                    classBuilder.with(ce);
                }
            }
        });
    }

    private static void transform(ClassBuilder classBuilder, MethodModel methodModel) {
        methodModel.code().ifPresent(code -> classBuilder.withMethod(
                methodModel.methodName(), // copy name, type and modifiers from the original
                methodModel.methodType(),
                methodModel.flags().flagsMask(),
                methodBuilder -> {
                    // keep the existing annotations (and other method elements) in place
                    methodModel.forEachElement(methodBuilder::with);
                    // change the code
                    methodBuilder.transformCode(code, (builder, element) -> {
                        // this way of instrumenting may miss the already loaded classes, java.lang.String for example.
                        // May need to circumvent that
                        if (element instanceof ThrowInstruction) {
                            builder.dup(); // on top of the stack is the current exception instance
                            // duplicate it to make sure the `athrow` op has something to throw
                            // after the invoke to ExceptionLogger has popped one off
                            builder.invokestatic( // call my code with the exception as argument
                                    ClassDesc.of(EXCEPTIONLOGGER), "log",
                                    MethodTypeDesc.ofDescriptor("(Ljava/lang/Throwable;)V"));
                        }
                        builder.with(element); // leave every element in place
                    });
                }));
    }
}