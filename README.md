# Static Requirements

## Introduction

Oliver Gierke tweeted: [https://twitter.com/olivergierke/status/968542913662803968](https://twitter.com/olivergierke/status/968542913662803968)

Wishing there was a way to specify in an interface that the implementation must have certain static factory methods.

This is an implementation using Annotation Processor to provide a solution

## Example

### Template Class
```java
public class StaticTemplate {
    public static InterfaceRequiresStatic create(String arg) {
        throw new NoSuchMethodError("create");
    }
}
```

### Interface 

```java
@RequiresStatic(StaticTemplate.class)
public interface InterfaceRequiresStatic {
    String someMethod();
    void otherMethod(String arg);
}
```

### Implementation
```java

public class MyClass implements InterfaceRequiresStatic {
    @Override
    public String someMethod() {
        return null;
    }

    @Override
    public void otherMethod(String arg) {
    }

    public static InterfaceRequiresStatic create(String arg) {
        InterfaceRequiresStatic result = new MyClass();
        result.otherMethod(arg);
        return result;
    }
}
```

### Performing Check
Until the Annotation processor is implemented use the following to check:
```java
if(RequiresStaticSupport.checkClass(MyClass.class)) {
	// use as needed.
	// an exception will be thrown if the requirement isn't met.
}
```