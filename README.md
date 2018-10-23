Patched build of https://github.com/google/guice/ - see PATCHES for the exact differences.

Compatibility with Google-Guice
-------------------------------

The main difference between Sisu-Guice and Google-Guice is that Guava is now exposed as a direct Maven dependency. If you are assembling your application outside of Maven you therefore need to add Guava to the runtime JARs. The build uses Guava 18.0 but you can use Maven's `<dependencyManagement>` to select a different version of Guava.

Because of this dependency difference you should avoid mixing the official Google-Guice library with internal extensions provided by Sisu-Guice and vice-versa. Third-party Guice extensions should be compatible with either library.

Sisu-Guice retains the same public API as Google-Guice and is binary compatible from a client perspective.
