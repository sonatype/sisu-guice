Patched build of http://code.google.com/p/google-guice/ - see PATCHES for the exact differences.

Compatibility with Google-Guice
-------------------------------

The main difference between Sisu-Guice and Google-Guice is that Guava is now exposed as a direct dependency.
If you are assembling your application outside of Maven you therefore need to add Guava to the runtime JARs.

Sisu-Guice retains the same public API as Google-Guice and is binary compatible from a client perspective.
