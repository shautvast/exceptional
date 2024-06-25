Experiment with java22 and Rust to monitor exceptions in a JVM

It's a mini APM that leverages the java22 ClassFile API and java22 Foreign Function Interface
It's a java agent that instruments all `throw` opcodes in your bytecode with a call to a logger written in Rust.

Work in Progress. Proof of concept works, but needs optimization. Try this at home, not at work :)

Running:
* Update the path to the rust lib (temp fix) in ExceptionLogger for your setup
* mvn clean install
* cd rustlib; cargo build
* create a minimal class in a separate project
```java
public class Main {
    public static void main(String[] args) throws Throwable {
        throw new Throwable();
    }
}
```
* run it with (adjust paths): 
``` bash
java22 -javaagent:$EXCEPTIONAL_PROJECT/exceptional/agent/target/exceptional-agent-1.0-SNAPSHOT.jar --enable-preview -classpath $YOUR_CLASSPATH Main
```
