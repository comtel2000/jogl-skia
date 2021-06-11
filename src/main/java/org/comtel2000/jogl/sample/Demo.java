package org.comtel2000.jogl.sample;

import java.lang.invoke.MethodHandles;
import java.nio.IntBuffer;
import java.util.concurrent.locks.ReentrantLock;

import org.comtel2000.jogl.sample.scene.HUD;
import org.comtel2000.jogl.sample.scene.SkiaScene;
import org.comtel2000.jogl.sample.scene.SkottieScene;
import org.jetbrains.skija.BackendRenderTarget;
import org.jetbrains.skija.Canvas;
import org.jetbrains.skija.ColorSpace;
import org.jetbrains.skija.DirectContext;
import org.jetbrains.skija.FramebufferFormat;
import org.jetbrains.skija.Surface;
import org.jetbrains.skija.SurfaceColorFormat;
import org.jetbrains.skija.SurfaceOrigin;
import org.jetbrains.skija.impl.Stats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.javafx.NewtCanvasJFX;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.Animator;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;

/**
 * --module-path C:\\Java\\openjfx\\javafx-sdk-16\\lib --add-modules javafx.controls --add-opens
 * javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED --add-opens
 * javafx.graphics/javafx.stage=ALL-UNNAMED --add-opens
 * javafx.graphics/com.sun.javafx.tk.quantum=ALL-UNNAMED --add-opens
 * javafx.graphics/com.sun.glass.ui=ALL-UNNAMED
 * 
 * @author comtel
 *
 */
public class Demo extends Application {

  private static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private Animator animator;
  private DirectContext context;
  private Surface surface;
  private BackendRenderTarget renderTarget;
  private Canvas canvas;

  private int textureId;
  private HUD hud;

  private volatile boolean vsync = true;
  private final ReentrantLock reshapeLock = new ReentrantLock();

  private SkiaScene skiaScene;

  @Override
  public void start(Stage stage) {
    Platform.setImplicitExit(true);
    Stats.enabled = true;
    
    var root = new FlowPane();
    var scene = new Scene(root, 800, 650);
    stage.setScene(scene);
    stage.show();

    hud = new HUD();
    skiaScene = new SkottieScene();// new ShadersScene();

    var jfxNewtDisplay = NewtFactory.createDisplay(null, false);
    var screen = NewtFactory.createScreen(jfxNewtDisplay, 0);

    var caps = new GLCapabilities(GLProfile.getGL2GL3());
    caps.setDoubleBuffered(true);
    var glWindow = GLWindow.create(screen, caps);
    glWindow.setAutoSwapBufferMode(true);

    glWindow.addGLEventListener(new GLEventListener() {

      private final IntBuffer intBuf = IntBuffer.allocate(1);

      public void init(final GLAutoDrawable drawable) {
        logger.info("init");
        initSkija(drawable, true);
        drawable.getGL().setSwapInterval(vsync ? 1 : 0);
      }

      public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
        logger.info("reshape {},{},{},{}", x, y, width, height);
        if (reshapeLock.tryLock()) {
          try {
            if (surface == null || surface.getWidth() != width || surface.getHeight() != height) {
              initSkija(drawable, false);
            }
          } finally {
            reshapeLock.unlock();
          }
        }
      }

      public void display(final GLAutoDrawable drawable) {
        drawable.getGL().setSwapInterval(vsync ? 1 : 0);
        drawable.getGL().glBindTexture(GL.GL_TEXTURE_2D, textureId);
        
        canvas.clear(0xFFFFFFFF);
        draw(canvas, drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
        context.flush();
        drawable.getGL().glGetIntegerv(GL.GL_TEXTURE_BINDING_2D, intBuf);
        textureId = intBuf.get(0);

      }

      public void dispose(final GLAutoDrawable drawable) {
        logger.info("dispose");
        if (surface != null) {
          surface.close();
        }
        if (renderTarget != null) {
          renderTarget.close();
        }
      }
    });
    var glCanvas = new NewtCanvasJFX(glWindow);
    glCanvas.setWidth(scene.getWidth());
    glCanvas.setHeight(scene.getHeight() - 50);
    
    root.getChildren().addAll(glCanvas, new Button("JavaFX " + System.getProperty("javafx.version")));
    
    glWindow.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent ev) {
        switch (ev.getKeyCode()) {
          case KeyEvent.VK_V:
            vsync = !vsync;
            break;
          case KeyEvent.VK_S:
            Stats.enabled = !Stats.enabled;
            break;
          case KeyEvent.VK_G:
            logger.info("Before GC {}", Stats.allocated);
            System.gc();
            break;
          case KeyEvent.VK_UP:
            skiaScene.changeVariant(1);
            break;
          case KeyEvent.VK_DOWN:
            skiaScene.changeVariant(-1);
            break;
          default:
            break;
        }
      }
    });


    // fps = new FPSAnimator(glWindow, 60, false);

    // glCanvas.widthProperty().bind(stage.widthProperty());
    // glCanvas.heightProperty().bind(stage.heightProperty());

    animator = new Animator(glWindow);
    animator.start();


  }


  private final void initSkija(GLAutoDrawable glCanvas, boolean reinitTexture) {

    var width = glCanvas.getSurfaceWidth();
    var height = glCanvas.getSurfaceHeight();
    var dpi = 1.0f;
    var intBuf = IntBuffer.allocate(1);
    glCanvas.getGL().glGetIntegerv(GL.GL_DRAW_FRAMEBUFFER_BINDING, intBuf);

    var fbId = intBuf.get(0);

    renderTarget = BackendRenderTarget.makeGL((int) ((float) width * dpi), (int) ((float) height * dpi), 0, 8, fbId, FramebufferFormat.GR_GL_RGBA8);
    context = DirectContext.makeGL();
    surface = Surface.makeFromBackendRenderTarget(context, renderTarget, SurfaceOrigin.BOTTOM_LEFT, SurfaceColorFormat.RGBA_8888, ColorSpace.getSRGB());
    canvas = surface.getCanvas();
    if (reinitTexture) {
      glCanvas.getGL().glGetIntegerv(GL.GL_TEXTURE_BINDING_2D, intBuf);
      textureId = intBuf.get(0);
    }

  }

  @Override
  public void stop() throws Exception {
    if (animator != null) {
      animator.stop();
    }
  }

  void draw(Canvas canvas, int width, int height) {
    canvas.save();
    skiaScene.draw(canvas, width, height, 1.0f, 0, 0);
    canvas.restore();

    if (Stats.enabled) {
      hud.setScene(skiaScene);
      hud.tick();
      hud.setVsync(vsync);
      canvas.save();
      hud.draw(canvas, width, height);
      canvas.restore();
    }
  }

  public static void main(String[] args) {
    launch();
  }
}
