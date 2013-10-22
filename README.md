Patched build of http://code.google.com/p/google-guice/ - see PATCHES for the exact differences.

Compatibility with Google-Guice
-------------------------------

The main difference between Sisu-Guice and Google-Guice is that Guava is now exposed as a direct Maven dependency. If you are assembling your application outside of Maven you therefore need to add Guava to the runtime JARs. The build uses Guava 11.0.2 but you can use Maven's `<dependencyManagement>` to select a different version of Guava. (The current source code is compatible up to and including Guava 15.0)

Sisu-Guice retains the same public API as Google-Guice and is binary compatible from a client perspective.
