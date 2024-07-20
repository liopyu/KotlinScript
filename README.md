# Kotlin Script

Welcome to the Kotlin Script, a kotlin script to Java interpreter!
This project enables you to write scripts in Kotlin (.kts files) and have them translated to Java, soon to be personalized for use in minecraft.

## Features

| **Write in Kotlin**                                                                                                    | **Translate to Java**                                                                                                    | **Scope Management**                                                                                                | **Error Reporting**                                                                                                  | **Logging**                                                                                                          |
|------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------|
| Write your scripts using the powerful Kotlin language.                                                                 | Automatically translates Kotlin scripts to Java for seamless integration with Minecraft Forge.                            | Advanced scope management to handle variable declarations and more.                                                  | Detailed error messages to help you debug your scripts.                                                              | Provides feedback on script execution through the logger.                                                            |


## Getting Started

### Prerequisites

- Minecraft Forge 1.20.1
- Java Development Kit (JDK) 8 or higher

### Usage

1. Create a `.kts` file in the `run/scripts` folder of your Minecraft directory. For example, `example.kts`:
    ```kotlin
   import net.liopyu.kotlinscript.TestClass as TClass // Optionally import classes as an alias
   TClass().instancedExecute() // Instantiated method calls
   var variable = TClass.execute() // TClass.execute() is a void method which logs a message to console
   variable // Call/execute the method
   var variable2 = "first assigned variable"
   {
      // Re-assign in a scope
      variable2 = "new variable"
      {
         variable2 = "newest variable"
         print(variable2) // Console logs "newest variable"
      }
   }
    ```

2. Start Minecraft. The interpreter will automatically load and execute the scripts from the `scripts` folder.

### Classes Overview

| **Class**                  | **Description**                                                                                                                                                                                                                   |
|----------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `KotlinScript`             | Initializes a script interpreter to load and execute Kotlin scripts from a folder.                                                                                                                                                |
| `KotlinScriptInterpreter`  | Loads `.kts` files, interprets the scripts line by line, and handles commands such as variable declarations, method invocations, and imports. Manages variable scopes using `ScopeChain` and processes keywords using `KeywordHandler`. |
| `Scope`                    | Manages variable declarations, immutability, and type storage within a particular scope. Handles variable inheritance from parent scopes and checks against modifications of immutable variables.                                     |
| `ScopeChain`               | Manages a stack of `Scope` objects, allowing for entering and exiting of scopes. Maintains a global scope and manages nested scopes.                                                                                                 |
| `KotlinScriptHelperClass`  | Manages keywords used in Kotlin script files, defining and recognizing keywords such as `val`, `var`, `print`, and `import`.                                                                                                       |
| `ContextUtils`             | Handles storing information about classes, such as their Java class object, simple name, alias, and full class name. Facilitates dynamic interaction with classes within the scripting environment.                                 |


## License

This project is licensed under the GNU General Public License v3.0. See the `LICENSE` file for more details.
