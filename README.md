# Skija-jogl-JavaFX Testing


references/copied resources from:

https://github.com/JetBrains/skija

https://github.com/jzy3d/jogl-maven-deployer


JavaSDK path must be changed to your local one

```xml
<javafx16sdk>C:\\Java\\openjfx\\javafx-sdk-16\\lib</javafx16sdk>
```

run with other OS than Winodows x64 please change the jogl and skija platform

```xml
<skija.platform>windows</skija.platform>
<jogl.platform>windows-amd64</jogl.platform>
```

run with jdk16

```console
mvn verify exec:java
```

![Demo](jogl-skia.png "Demo")