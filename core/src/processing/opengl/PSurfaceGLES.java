package processing.opengl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ConfigurationInfo;
import android.content.res.AssetManager;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.EGLConfigChooser;
import android.opengl.GLSurfaceView.Renderer;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import processing.app.PContainer;
import processing.app.PFragment;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PSurface;

public class PSurfaceGLES implements PSurface, PConstants {

  private PContainer container;
  private PGraphics graphics;

  private PApplet sketch;

  private Activity activity;
  private WallpaperService wallpaper;

  private View view;
  private SurfaceView surface;

  public PGLES pgl;


  /** The renderer object driving the rendering loop, analogous to the
   * GLEventListener in JOGL */
  protected static AndroidRenderer renderer;
  protected static AndroidConfigChooser configChooser;


  public PSurfaceGLES(PGraphics graphics, PContainer container, SurfaceView view) {
    this.sketch = graphics.parent;
    this.graphics = graphics;
    this.container = container;
    this.pgl = (PGLES)((PGraphicsOpenGL)graphics).pgl;

    if (container.getKind() == PContainer.FRAGMENT) {
      PFragment frag = (PFragment)container;
      activity = frag.getActivity();
      if (view == null) surface = new SketchSurfaceViewGL(activity);
    } else if (container.getKind() == PContainer.WALLPAPER) {
      wallpaper = (WallpaperService)container;
      if (view == null) surface = new SketchSurfaceViewGL(wallpaper);
    }
    if (view != null) surface = view;
  }

//  public PSurfaceGLES(PGraphicsOpenGL graphics) {
//    this.graphics = graphics;
//  }
//
//  public PSurfaceGLES(PApplet sketch, Activity activity, Class<?> rendererClass, int sw, int sh) {
//    this.sketch = sketch;
//    this.activity = activity;
//    surface = new SketchSurfaceViewGL(activity, sw, sh,
//      (Class<? extends PGraphicsOpenGL>)rendererClass);
//  }

  public PContainer getContainer() {
    return container;
  }

  @Override
  public Activity getActivity() {
    return activity;
  }

  @Override
  public View getRootView() {
    return view;
  }

  @Override
  public void setRootView(View view) {
    this.view = view;
  }

  @Override
  public SurfaceView getSurfaceView() {
    return surface;
  }

  public class SketchSurfaceViewGL extends GLSurfaceView {
    PGraphicsOpenGL g3;
    SurfaceHolder surfaceHolder;


    @SuppressWarnings("deprecation")
    public SketchSurfaceViewGL(Context context) {
      super(context);
      g3 = (PGraphicsOpenGL)graphics;

      // Check if the system supports OpenGL ES 2.0.
      final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
      final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
      final boolean supportsGLES2 = configurationInfo.reqGlEsVersion >= 0x20000;

      if (!supportsGLES2) {
        throw new RuntimeException("OpenGL ES 2.0 is not supported by this device.");
      }

      surfaceHolder = getHolder();
      // are these two needed?
      surfaceHolder.addCallback(this);
      surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_GPU);

      // Tells the default EGLContextFactory and EGLConfigChooser to create an GLES2 context.
      setEGLContextClientVersion(2);

      int quality = sketch.sketchQuality();
      if (1 < quality) {
        setEGLConfigChooser(getConfigChooser(quality));
      }

      // The renderer can be set only once.
      setRenderer(getRenderer());
      setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
      setPreserveEGLContextOnPause(true);

      setFocusable(true);
      setFocusableInTouchMode(true);
      requestFocus();
    }


    public void onDestroy() {
      super.onDetachedFromWindow();
    }

    // part of SurfaceHolder.Callback
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
      super.surfaceCreated(holder);
      if (PApplet.DEBUG) {
        System.out.println("surfaceCreated()");
      }
    }


    // part of SurfaceHolder.Callback
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
      super.surfaceDestroyed(holder);
      if (PApplet.DEBUG) {
        System.out.println("surfaceDestroyed()");
      }

      /*
      // TODO: Check how to make sure of calling g3.dispose() when this call to
      // surfaceDestoryed corresponds to the sketch being shut down instead of just
      // taken to the background.

      // For instance, something like this would be ok?
      // The sketch is being stopped, so we dispose the resources.
      if (!paused) {
        g3.dispose();
      }
      */
    }


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
      super.surfaceChanged(holder, format, w, h);

      if (PApplet.DEBUG) {
        System.out.println("SketchSurfaceView3D.surfaceChanged() " + w + " " + h);
      }
      sketch.surfaceChanged();
//      width = w;
//      height = h;
//      g.setSize(w, h);

      // No need to call g.setSize(width, height) b/c super.surfaceChanged()
      // will trigger onSurfaceChanged in the renderer, which calls setSize().
      // -- apparently not true? (100110)
    }


    /**
     * Inform the view that the window focus has changed.
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
      super.onWindowFocusChanged(hasFocus);
      sketch.surfaceWindowFocusChanged(hasFocus);
//      super.onWindowFocusChanged(hasFocus);
//      focused = hasFocus;
//      if (focused) {
////        println("got focus");
//        focusGained();
//      } else {
////        println("lost focus");
//        focusLost();
//      }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
      return sketch.surfaceTouchEvent(event);
    }


    @Override
    public boolean onKeyDown(int code, android.view.KeyEvent event) {
      sketch.surfaceKeyDown(code, event);
      return super.onKeyDown(code, event);
    }


    @Override
    public boolean onKeyUp(int code, android.view.KeyEvent event) {
      sketch.surfaceKeyUp(code, event);
      return super.onKeyUp(code, event);
    }


    // don't think i want to call stop() from here, since it might be swapping renderers
//    @Override
//    protected void onDetachedFromWindow() {
//      super.onDetachedFromWindow();
//      stop();
//    }
  }

  public AssetManager getAssets() {
    if (container.getKind() == PContainer.FRAGMENT) {
      return activity.getAssets();
    } else if (container.getKind() == PContainer.WALLPAPER) {
      return wallpaper.getBaseContext().getAssets();
    }
    return null;
  }

  public void startActivity(Intent intent) {
    if (container.getKind() == PContainer.FRAGMENT) {
      container.startActivity(intent);
    }
  }

  public void initView(int sketchWidth, int sketchHeight) {
    if (container.getKind() == PContainer.FRAGMENT) {
      int displayWidth = container.getWidth();
      int displayHeight = container.getHeight();
      View rootView;
      if (sketchWidth == displayWidth && sketchHeight == displayHeight) {
        // If using the full screen, don't embed inside other layouts
//        window.setContentView(surfaceView);
        rootView = getSurfaceView();
      } else {
        // If not using full screen, setup awkward view-inside-a-view so that
        // the sketch can be centered on screen. (If anyone has a more efficient
        // way to do this, please file an issue on Google Code, otherwise you
        // can keep your "talentless hack" comments to yourself. Ahem.)
        RelativeLayout overallLayout = new RelativeLayout(activity);
        RelativeLayout.LayoutParams lp =
          new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                                          LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.CENTER_IN_PARENT);

        LinearLayout layout = new LinearLayout(activity);
        layout.addView(getSurfaceView(), sketchWidth, sketchHeight);
        overallLayout.addView(layout, lp);
//        window.setContentView(overallLayout);
        rootView = overallLayout;
      }
      setRootView(rootView);
    } else if (container.getKind() == PContainer.WALLPAPER) {
      int displayWidth = container.getWidth();
      int displayHeight = container.getHeight();
      View rootView;
      if (sketchWidth == displayWidth && sketchHeight == displayHeight) {
        // If using the full screen, don't embed inside other layouts
//        window.setContentView(surfaceView);
        rootView = getSurfaceView();
      } else {
        // If not using full screen, setup awkward view-inside-a-view so that
        // the sketch can be centered on screen. (If anyone has a more efficient
        // way to do this, please file an issue on Google Code, otherwise you
        // can keep your "talentless hack" comments to yourself. Ahem.)
        RelativeLayout overallLayout = new RelativeLayout(wallpaper);
        RelativeLayout.LayoutParams lp =
          new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                                          LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.CENTER_IN_PARENT);

        LinearLayout layout = new LinearLayout(wallpaper);
        layout.addView(getSurfaceView(), sketchWidth, sketchHeight);
        overallLayout.addView(layout, lp);
//        window.setContentView(overallLayout);
        rootView = overallLayout;
      }
      setRootView(rootView);
    }
  }

  public String getName() {
    if (container.getKind() == PContainer.FRAGMENT) {
      return activity.getComponentName().getPackageName();
    } else if (container.getKind() == PContainer.WALLPAPER) {
      return wallpaper.getPackageName();
    }
    return "";
  }

  public void setOrientation(int which) {
    if (container.getKind() == PContainer.FRAGMENT) {
      if (which == PORTRAIT) {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
      } else if (which == LANDSCAPE) {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
      }
    }
  }

  public File getFilesDir() {
    if (container.getKind() == PContainer.FRAGMENT) {
      return activity.getFilesDir();
    } else if (container.getKind() == PContainer.WALLPAPER) {
      return wallpaper.getFilesDir();
    }
    return null;
  }

  public InputStream openFileInput(String filename) {
    if (container.getKind() == PContainer.FRAGMENT) {
      try {
        return activity.openFileInput(filename);
      } catch (FileNotFoundException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    return null;
  }

  public File getFileStreamPath(String path) {
    if (container.getKind() == PContainer.FRAGMENT) {
      return activity.getFileStreamPath(path);
    } else if (container.getKind() == PContainer.WALLPAPER) {
      return wallpaper.getFileStreamPath(path);
    }
    return null;
  }

  public void dispose() {
    // TODO Auto-generated method stub
//    surface.onDestroy();
  }


  ///////////////////////////////////////////////////////////

  // Android specific classes (Renderer, ConfigChooser)


  public AndroidRenderer getRenderer() {
    renderer = new AndroidRenderer();
    return renderer;
  }


  public AndroidContextFactory getContextFactory() {
    return new AndroidContextFactory();
  }


  public AndroidConfigChooser getConfigChooser(int samples) {
    configChooser = new AndroidConfigChooser(5, 6, 5, 4, 16, 1, samples);
    return configChooser;
  }


  public AndroidConfigChooser getConfigChooser(int r, int g, int b, int a,
                                               int d, int s, int samples) {
    configChooser = new AndroidConfigChooser(r, g, b, a, d, s, samples);
    return configChooser;
  }


  protected class AndroidRenderer implements Renderer {
    public AndroidRenderer() {
    }

    public void onDrawFrame(GL10 igl) {
//      System.out.println("drawing frame " + sketch.frameCount);
      pgl.getGL(igl);
      sketch.handleDraw();
//      gl.glClearColor(sketch.random(0, 1), 0, 0, 1);
//      gl.glClear(GLES20.GL_COLOR_BUFFER_BIT);

    }

    public void onSurfaceChanged(GL10 igl, int iwidth, int iheight) {
      pgl.getGL(igl);

      // Here is where we should initialize native libs...
      // lib.init(iwidth, iheight);

      graphics.setSize(iwidth, iheight);
    }

    public void onSurfaceCreated(GL10 igl, EGLConfig config) {
      pgl.init(igl);
    }
  }


  protected class AndroidContextFactory implements
    GLSurfaceView.EGLContextFactory {
    public EGLContext createContext(EGL10 egl, EGLDisplay display,
        EGLConfig eglConfig) {
      int[] attrib_list = { PGLES.EGL_CONTEXT_CLIENT_VERSION, 2,
                            EGL10.EGL_NONE };
      EGLContext context = egl.eglCreateContext(display, eglConfig,
                                                EGL10.EGL_NO_CONTEXT,
                                                attrib_list);
      return context;
    }

    public void destroyContext(EGL10 egl, EGLDisplay display,
                               EGLContext context) {
      egl.eglDestroyContext(display, context);
    }
  }


  protected class AndroidConfigChooser implements EGLConfigChooser {
    // Desired size (in bits) for the rgba color, depth and stencil buffers.
    public int redTarget;
    public int greenTarget;
    public int blueTarget;
    public int alphaTarget;
    public int depthTarget;
    public int stencilTarget;

    // Actual rgba color, depth and stencil sizes (in bits) supported by the
    // device.
    public int redBits;
    public int greenBits;
    public int blueBits;
    public int alphaBits;
    public int depthBits;
    public int stencilBits;
    public int[] tempValue = new int[1];

    public int numSamples;

    /*
    The GLES2 extensions supported are:
      GL_OES_rgb8_rgba8 GL_OES_depth24 GL_OES_vertex_half_float
      GL_OES_texture_float GL_OES_texture_half_float
      GL_OES_element_index_uint GL_OES_mapbuffer
      GL_OES_fragment_precision_high GL_OES_compressed_ETC1_RGB8_texture
      GL_OES_EGL_image GL_OES_required_internalformat GL_OES_depth_texture
      GL_OES_get_program_binary GL_OES_packed_depth_stencil
      GL_OES_standard_derivatives GL_OES_vertex_array_object GL_OES_egl_sync
      GL_EXT_multi_draw_arrays GL_EXT_texture_format_BGRA8888
      GL_EXT_discard_framebuffer GL_EXT_shader_texture_lod
      GL_IMG_shader_binary GL_IMG_texture_compression_pvrtc
      GL_IMG_texture_stream2 GL_IMG_texture_npot
      GL_IMG_texture_format_BGRA8888 GL_IMG_read_format
      GL_IMG_program_binary GL_IMG_multisampled_render_to_texture
      */

    /*
    // The attributes we want in the frame buffer configuration for Processing.
    // For more details on other attributes, see:
    // http://www.khronos.org/opengles/documentation/opengles1_0/html/eglChooseConfig.html
    protected int[] configAttribsGL_MSAA = {
      EGL10.EGL_RED_SIZE, 5,
      EGL10.EGL_GREEN_SIZE, 6,
      EGL10.EGL_BLUE_SIZE, 5,
      EGL10.EGL_ALPHA_SIZE, 4,
      EGL10.EGL_DEPTH_SIZE, 16,
      EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
      EGL10.EGL_SAMPLE_BUFFERS, 1,
      EGL10.EGL_SAMPLES, 2,
      EGL10.EGL_NONE };

    protected int[] configAttribsGL_CovMSAA = {
      EGL10.EGL_RED_SIZE, 5,
      EGL10.EGL_GREEN_SIZE, 6,
      EGL10.EGL_BLUE_SIZE, 5,
      EGL10.EGL_ALPHA_SIZE, 4,
      EGL10.EGL_DEPTH_SIZE, 16,
      EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
      EGL_COVERAGE_BUFFERS_NV, 1,
      EGL_COVERAGE_SAMPLES_NV, 2,
      EGL10.EGL_NONE };

    protected int[] configAttribsGL_NoMSAA = {
      EGL10.EGL_RED_SIZE, 5,
      EGL10.EGL_GREEN_SIZE, 6,
      EGL10.EGL_BLUE_SIZE, 5,
      EGL10.EGL_ALPHA_SIZE, 4,
      EGL10.EGL_DEPTH_SIZE, 16,
      EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
      EGL10.EGL_NONE };

    protected int[] configAttribsGL_Good = {
      EGL10.EGL_RED_SIZE, 8,
      EGL10.EGL_GREEN_SIZE, 8,
      EGL10.EGL_BLUE_SIZE, 8,
      EGL10.EGL_ALPHA_SIZE, 8,
      EGL10.EGL_DEPTH_SIZE, 16,
      EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
      EGL10.EGL_NONE };

    protected int[] configAttribsGL_TestMSAA = {
      EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
      EGL10.EGL_SAMPLE_BUFFERS, 1,
      EGL10.EGL_SAMPLES, 2,
      EGL10.EGL_NONE };
    */

    protected int[] attribsNoMSAA = {
      EGL10.EGL_RENDERABLE_TYPE, PGLES.EGL_OPENGL_ES2_BIT,
      EGL10.EGL_SAMPLE_BUFFERS, 0,
      EGL10.EGL_NONE };

    public AndroidConfigChooser(int rbits, int gbits, int bbits, int abits,
                                int dbits, int sbits, int samples) {
      redTarget = rbits;
      greenTarget = gbits;
      blueTarget = bbits;
      alphaTarget = abits;
      depthTarget = dbits;
      stencilTarget = sbits;
      numSamples = samples;
    }

    public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
      EGLConfig[] configs = null;
      if (1 < numSamples) {
        int[] attribs = new int[] {
          EGL10.EGL_RENDERABLE_TYPE, PGLES.EGL_OPENGL_ES2_BIT,
          EGL10.EGL_SAMPLE_BUFFERS, 1,
          EGL10.EGL_SAMPLES, numSamples,
          EGL10.EGL_NONE };
        configs = chooseConfigWithAttribs(egl, display, attribs);
        if (configs == null) {
          // No normal multisampling config was found. Try to create a
          // coverage multisampling configuration, for the nVidia Tegra2.
          // See the EGL_NV_coverage_sample documentation.
          int[] attribsCov = {
            EGL10.EGL_RENDERABLE_TYPE, PGLES.EGL_OPENGL_ES2_BIT,
            PGLES.EGL_COVERAGE_BUFFERS_NV, 1,
            PGLES.EGL_COVERAGE_SAMPLES_NV, numSamples,
            EGL10.EGL_NONE };
          configs = chooseConfigWithAttribs(egl, display, attribsCov);
          if (configs == null) {
            configs = chooseConfigWithAttribs(egl, display, attribsNoMSAA);
          } else {
            PGLES.usingMultisampling = true;
            PGLES.usingCoverageMultisampling = true;
            PGLES.multisampleCount = numSamples;
          }
        } else {
          PGLES.usingMultisampling = true;
          PGLES.usingCoverageMultisampling = false;
          PGLES.multisampleCount = numSamples;
        }
      } else {
        configs = chooseConfigWithAttribs(egl, display, attribsNoMSAA);
      }

      if (configs == null) {
        throw new IllegalArgumentException("No EGL configs match configSpec");
      }

      if (PApplet.DEBUG) {
        for (EGLConfig config : configs) {
          String configStr = "P3D - selected EGL config : "
            + printConfig(egl, display, config);
          System.out.println(configStr);
        }
      }

      // Now return the configuration that best matches the target one.
      return chooseBestConfig(egl, display, configs);
    }

    public EGLConfig chooseBestConfig(EGL10 egl, EGLDisplay display,
                                      EGLConfig[] configs) {
      EGLConfig bestConfig = null;
      float bestScore = Float.MAX_VALUE;

      for (EGLConfig config : configs) {
        int gl = findConfigAttrib(egl, display, config,
                                  EGL10.EGL_RENDERABLE_TYPE, 0);
        boolean isGLES2 = (gl & PGLES.EGL_OPENGL_ES2_BIT) != 0;
        if (isGLES2) {
          int d = findConfigAttrib(egl, display, config,
                                   EGL10.EGL_DEPTH_SIZE, 0);
          int s = findConfigAttrib(egl, display, config,
                                   EGL10.EGL_STENCIL_SIZE, 0);

          int r = findConfigAttrib(egl, display, config,
                                   EGL10.EGL_RED_SIZE, 0);
          int g = findConfigAttrib(egl, display, config,
                                   EGL10.EGL_GREEN_SIZE, 0);
          int b = findConfigAttrib(egl, display, config,
                                   EGL10.EGL_BLUE_SIZE, 0);
          int a = findConfigAttrib(egl, display, config,
                                   EGL10.EGL_ALPHA_SIZE, 0);

          float score = 0.20f * PApplet.abs(r - redTarget) +
                        0.20f * PApplet.abs(g - greenTarget) +
                        0.20f * PApplet.abs(b - blueTarget) +
                        0.15f * PApplet.abs(a - alphaTarget) +
                        0.15f * PApplet.abs(d - depthTarget) +
                        0.10f * PApplet.abs(s - stencilTarget);

          if (score < bestScore) {
            // We look for the config closest to the target config.
            // Closeness is measured by the score function defined above:
            // we give more weight to the RGB components, followed by the
            // alpha, depth and finally stencil bits.
            bestConfig = config;
            bestScore = score;

            redBits = r;
            greenBits = g;
            blueBits = b;
            alphaBits = a;
            depthBits = d;
            stencilBits = s;
          }
        }
      }

      if (PApplet.DEBUG) {
        String configStr = "P3D - selected EGL config : "
          + printConfig(egl, display, bestConfig);
        System.out.println(configStr);
      }
      return bestConfig;
    }

    protected String printConfig(EGL10 egl, EGLDisplay display,
                                 EGLConfig config) {
      int r = findConfigAttrib(egl, display, config,
                               EGL10.EGL_RED_SIZE, 0);
      int g = findConfigAttrib(egl, display, config,
                               EGL10.EGL_GREEN_SIZE, 0);
      int b = findConfigAttrib(egl, display, config,
                               EGL10.EGL_BLUE_SIZE, 0);
      int a = findConfigAttrib(egl, display, config,
                               EGL10.EGL_ALPHA_SIZE, 0);
      int d = findConfigAttrib(egl, display, config,
                               EGL10.EGL_DEPTH_SIZE, 0);
      int s = findConfigAttrib(egl, display, config,
                               EGL10.EGL_STENCIL_SIZE, 0);
      int type = findConfigAttrib(egl, display, config,
                                  EGL10.EGL_RENDERABLE_TYPE, 0);
      int nat = findConfigAttrib(egl, display, config,
                                 EGL10.EGL_NATIVE_RENDERABLE, 0);
      int bufSize = findConfigAttrib(egl, display, config,
                                     EGL10.EGL_BUFFER_SIZE, 0);
      int bufSurf = findConfigAttrib(egl, display, config,
                                     EGL10.EGL_RENDER_BUFFER, 0);

      return String.format("EGLConfig rgba=%d%d%d%d depth=%d stencil=%d",
                           r,g,b,a,d,s)
        + " type=" + type
        + " native=" + nat
        + " buffer size=" + bufSize
        + " buffer surface=" + bufSurf +
        String.format(" caveat=0x%04x",
                      findConfigAttrib(egl, display, config,
                                       EGL10.EGL_CONFIG_CAVEAT, 0));
    }

    protected int findConfigAttrib(EGL10 egl, EGLDisplay display,
      EGLConfig config, int attribute, int defaultValue) {
      if (egl.eglGetConfigAttrib(display, config, attribute, tempValue)) {
        return tempValue[0];
      }
      return defaultValue;
    }

     protected EGLConfig[] chooseConfigWithAttribs(EGL10 egl,
                                                   EGLDisplay display,
                                                   int[] configAttribs) {
       // Get the number of minimally matching EGL configurations
       int[] configCounts = new int[1];
       egl.eglChooseConfig(display, configAttribs, null, 0, configCounts);

       int count = configCounts[0];

       if (count <= 0) {
         //throw new IllegalArgumentException("No EGL configs match configSpec");
         return null;
       }

       // Allocate then read the array of minimally matching EGL configs
       EGLConfig[] configs = new EGLConfig[count];
       egl.eglChooseConfig(display, configAttribs, configs, count, configCounts);
       return configs;

       // Get the number of minimally matching EGL configurations
//     int[] num_config = new int[1];
//     egl.eglChooseConfig(display, configAttribsGL, null, 0, num_config);
//
//     int numConfigs = num_config[0];
//
//     if (numConfigs <= 0) {
//       throw new IllegalArgumentException("No EGL configs match configSpec");
//     }
//
//     // Allocate then read the array of minimally matching EGL configs
//     EGLConfig[] configs = new EGLConfig[numConfigs];
//     egl.eglChooseConfig(display, configAttribsGL, configs, numConfigs,
//         num_config);

     }
  }
}
