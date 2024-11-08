
# KotlinScript

**KotlinScript** is a mod for Minecraft that enables you to write and execute Kotlin `.kts` scripts directly from the `config/scripts` folder. This mod makes it easy to add custom behavior to Minecraft using Kotlin scripting.

## Features

- **Script Execution**: Automatically parses and executes `.kts` scripts placed in `config/scripts`.
- **Embedded Kotlin Libraries**: Includes all necessary Kotlin dependencies for scripting, so no extra setup is required.
- **Fabric Integration**: Fully compatible with Fabric, supporting easy installation and usage.

## Usage

Place your `.kts` files in the `config/scripts` folder. Scripts will automatically run at startup.

Example (`config/scripts/example.kts`):

```kotlin
println("Hello from KotlinScript!")
```

## Dependencies

| Dependency                  | Description                                  |
|-----------------------------|----------------------------------------------|
| `kotlin-scripting-common`   | version 2.0.21                               |
| `kotlin-scripting-jvm`      | version 2.0.21                               |
| `kotlin-scripting-jvm-host` | version 2.0.21                               |
| `fabric-language-kotlin`    | Ensures Fabric compatibility for Kotlin code |

## Compatibility

| Requirement        | Version           |
|--------------------|-------------------|
| Minecraft          | Compatible with Fabric-supported versions |
| Fabric Loader      | Latest version recommended                |
| Kotlin Compatibility | Bundled with required Kotlin libraries  |

### Contributing

Feel free to open issues or submit pull requests for improvements.

---

