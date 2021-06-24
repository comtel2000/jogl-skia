# Skija-jogl-JavaFX Testing


references/copied resources from:

https://github.com/JetBrains/skija

https://github.com/jzy3d/jogl-maven-deployer


JavaSDK path must be changed to your local one

```xml
<javafx16sdk>/Library/Java/JavaFX/javafx-sdk-16/lib</javafx16sdk>
```

run with other OS than MacOS x64 please change the jogl and skija platform

```xml
<skija.platform>macos-x64</skija.platform>
<jogl.platform>macosx-universal</jogl.platform>
```


![Demo](jogl-skia-mac.png "Demo")