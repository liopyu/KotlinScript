# KotlinScript

KotlinScript is a Sponge plugin that acts as a scripting-host.

This means that Kotlin style scripts can make use of the Spongepowered API.

### [Wanting to get started? Read our wiki!](https://github.com/DrZoddiak/KotlinScript/wiki)

## What does a script look like?

A script simply needs the file extension of `.plugin.kts`

[You can find many example scripts here](https://github.com/DrZoddiak/KotlinScript/tree/master/host/run/config/scripting-host/scripts);
however, a simple example:
```kotlin 
import me.zodd.*
import net.kyori.adventure.text.Component

//Without context scripts will execute during construction
Logger.info("Hello Plugin Construction Event")

//Wrapper functions are provided to register events
onServerStarted {
    Logger.info("Hello Server Started Event")
}

//Sample first join message
onPlayerJoin {
    val player = player()
    if (!player.hasPlayedBefore()) {
        player.sendMessage(Component.text("Hello ${player.displayName()}"))
        /*
        Multi-line comments are supported
        
        Returns for events (if used) should use return flags as below
        */
        return@onPlayerJoin
    }
    Logger.info("${player.name()} - ${player.firstJoined()}")
}
```

## How could I possibly script without any auto-completion!?
No need fear! While you can script with a simple text editor
you may find that text completion and code suggestions is much easier!

All you need to do is [copy the KotlinScriptingExampleRepo](https://github.com/DrZoddiak/KotlinScriptingExampleRepo), 
create files with the proper file extension, and start scripting!
