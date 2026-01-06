# Java 25 Compatibility Issue

## Problem
Lombok 1.18.36 (latest stable) has compatibility issues with Java 25. The error:
```
java.lang.ExceptionInInitializerError: com.sun.tools.javac.code.TypeTag :: UNKNOWN
```

This occurs because Lombok accesses internal Java compiler APIs that have changed in Java 25.

## Current Status
- ✅ **Java 25**: Configured in pom.xml
- ✅ **Maven Compiler Plugin**: Updated to 3.13.0
- ❌ **Lombok**: Not fully compatible with Java 25 yet

## Options

### Option 1: Stay on Java 21 (Recommended - LTS)
**Pros**:
- ✅ Fully supported by all tools
- ✅ Long-term support (LTS)
- ✅ Stable and production-ready
- ✅ All dependencies work correctly

**Cons**:
- ❌ Not using latest Java features

**Action**: Revert `java.version` to `21` in pom.xml

### Option 2: Wait for Lombok Java 25 Support
**Status**: Lombok team is working on Java 25 support
**Timeline**: Unknown (check Lombok GitHub issues)

**Action**: Monitor Lombok releases for Java 25 support

### Option 3: Remove Lombok (Not Recommended)
**Pros**:
- ✅ Can use Java 25 immediately

**Cons**:
- ❌ Need to manually add getters/setters to all entities
- ❌ More boilerplate code
- ❌ Higher maintenance

**Action**: Remove Lombok and add getters/setters manually

### Option 4: Use Java 25 Without Lombok Annotation Processing
**Workaround**: Compile with Java 25 but disable Lombok during compilation, then manually add getters/setters.

**Not Recommended**: Defeats the purpose of using Lombok.

## Recommendation

**Use Java 21 (LTS)** for now because:
1. It's the current LTS version
2. All tools (Lombok, Spring Boot, etc.) fully support it
3. It's stable and production-ready
4. You can upgrade to Java 25 later when Lombok supports it

## Current Configuration

The pom.xml is currently set to:
- `java.version=25`
- Lombok `1.18.36`
- Maven compiler plugin `3.13.0`

## To Revert to Java 21

Change in `pom.xml`:
```xml
<properties>
    <java.version>21</java.version>
</properties>
```

And update compiler plugin:
```xml
<configuration>
    <release>21</release>
    <!-- Remove Java 25 specific compiler args -->
</configuration>
```

## Monitoring Lombok Java 25 Support

Check these resources:
- Lombok GitHub: https://github.com/projectlombok/lombok/issues
- Search for: "Java 25" or "JDK 25" compatibility
- Lombok releases: https://projectlombok.org/changelog

---

**Recommendation**: Use Java 21 for now, upgrade to Java 25 when Lombok supports it.
