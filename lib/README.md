# Why is this a module?

Because ktlint requires certain elements of the kotlin compiler, it includes a
dependency on the embedded kotlin compiler library.

Unfortunately, this clashes and conflicts with classes we use in the JetBrains
Kotlin plugin.

Separating these dependencies out allow us to create a "shadowed" JAR internally
that relocates the conflicting package so that ktlint works, and so does our code!
