WebGL ClojureScript Tutorial
============================

This document will guide you through the basics of setting up a ClojureScript program with thi.ng/geom and Figwheel to [interactively program](https://www.youtube.com/watch?v=KZjFVdU8VLI) WebGL.


[![Live coding!](https://j.gifs.com/RgMoM0.gif)](https://www.youtube.com/watch?v=0ipebuJ8QN0)

<!-- markdown-toc start - Don't edit this section. Run M-x markdown-toc-generate-toc again -->
**Table of Contents**

- [WebGL ClojureScript Tutorial](#webgl-clojurescript-tutorial)
    - [Introduction](#introduction)
    - [File Hierarchy](#file-hierarchy)
    - [An introduction to figwheel](#an-introduction-to-figwheel)
- [Getting started](#getting-started)
    - [The canvas](#the-canvas)
    - [Thi.ng Geom - the helper library](#thing-geom---the-helper-library)
        - [Namespaces](#namespaces)
        - [Let there be darkness](#let-there-be-darkness)
- [Hello Triangle](#hello-triangle)
    - [The shader spec](#the-shader-spec)
    - [The model](#the-model)
    - [The viewport](#the-viewport)
    - [Putting it all together](#putting-it-all-together)
- [Animation loop](#animation-loop)
    - [Quick look into functional programming](#quick-look-into-functional-programming)
    - [Constructing a animation function](#constructing-a-animation-function)
        - [The atom](#the-atom)
        - [Writing the function](#writing-the-function)
        - [A solution](#a-solution)
    - [Morphing the model](#morphing-the-model)
        - [Code checkpoint](#code-checkpoint)
        - [Defining and passing the transform](#defining-and-passing-the-transform)
- [Beyond this tutorial](#beyond-this-tutorial)

<!-- markdown-toc end -->


Introduction
------------

Hi, and welcome!

This guide assumes basic knowledge of OpenGL and [GLSL](https://en.wikipedia.org/wiki/Glsl) and rudimentary understanding of the [Clojure syntax](http://www.tryclj.com/). It's aimed at those who want to leveredge the zero iteration time development environment provided by [figwheel](https://www.youtube.com/watch?v=j-kj2qwJa_E) to make 3D applications.

In order to follow this guide you'll want to have [Leiningen](http://leiningen.org/) and [Git](https://git-scm.com/) along with your favourite text editor ([Emacs](https://www.gnu.org/software/emacs/) in case you haven't decided).

With that done, we're off to the races!



File Hierarchy
------------

In order to get started we'll start off by telling Leiningen that we want a new figwheel project:

    lein new figwheel webgl-clojurescript-tutorial
    cd webgl-clojurescript-tutorial

You'll now be standing in a directory tree looking something like this:

    ├── dev
    │  └── user.clj          -- Helper code for development.
    ├── project.clj          -- Leiningen definition of our project.
    ├── README.md            -- Helpful instructions to other developers.
    ├── resources
    │  └── public            -- Folder directly server by the web server.
    │     ├── css
    │     │  └── style.css   -- Our sole style sheet.
    │     └── index.html     -- The root html document.
    └── src
       └── webgl_clojurescript_tutorial
          └── core.cljs      -- Our ClojureScript code.

The most important files here are `index.html`, `style.css` and `core.cljs`; they're the sum of our code for now. The HTML and CSS are going to be delivered as they are by the web server; `core.cljs` on the other hand is has to be compiled before it can be delivered to the web browser.

In other words it's getting time to start our compiling conductor/web server/magic machine Figwheel! But before we do anything crazy we want to initiate a new git repository so that we'll always be able to land safely in a previous commit.

    git init
    git add src/* resources/public/index.html resources/public/css/style.css README.md project.clj dev/user.clj
    git commit -v

The file `project.clj` contains the Leiningen definition of our project, it tells Leiningen where our source code is and how to compile it; but perhaps most importantly it contains the list of dependencies we're building on. We'll have to edit this file every time we want to build on the shoulders of giants.



An introduction to figwheel
---------------------------

[Figwheel](https://www.youtube.com/watch?v=j-kj2qwJa_E) by Bruce Hauman allows for code to dynamically be injected into your running web application without destroying its state, provided you follow a few simple rules about how you handle your state.

As a fast and hard rule: Mutable state should be declared like:

```clojure
(defonce app-state (atom 0))
```

and to be read with [`@`](https://clojuredocs.org/clojure.core/deref) and modified with [`!swap`](https://clojuredocs.org/clojure.core/swap!) like:

```clojure
(swap! app-state function-which-mutates-state argument-to-function-if-any)
```

But since an interactive programming session says more than a thousand words, go ahead and start up figwheel!

    lein figwheel

Once done chopping its fruits your browser will have opened a new tab displaying your application. If you open your browser's [developer console](https://developer.mozilla.org/en-US/docs/Tools/Web_Console) (right click on the background of your page and press Inspect) you'll see that there's a message printed there for you. If we modify string literal given as the first argument to `println` in `src/webgl_clojurescript_tutorial/core.cljs` and save the file we'll see the modified string printed to the console.

This within itself isn't all that interesting, we'll need a more complex program to illustrate the true prowess of Figwheel. So go ahead and delete the `println` to give room for our WebGL program.

If you want to play a bit more with Figwheel before continuing I recommend trying out the [flappy bird demo by its author](https://github.com/bhauman/flappy-bird-demo/blob/master/project.clj).




Getting started
===============

In order to get started with WebGL there's one very important aspect missing in our `index.html` - a canvas.

## The canvas

Lets clear out the `<div>` in `<body>` and in its stead add a canvas so that we end up with the following `index.html`:

```html
<!DOCTYPE html>
<html>
  <head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link href="css/style.css" rel="stylesheet" type="text/css">
  </head>
  <body>
    <canvas id="main" height="400px" width="400px"></canvas>
    <script src="js/compiled/webgl_clojurescript_tutorial.js" type="text/javascript"></script>
  </body>
</html>
```

Now this is a rare moment, so cherish it: Press `F5` in your browser to reload the page. The index document is about the only thing figwheel can't inject to your page. In case you've forgotten the URL, it's in your figwheel output: http://localhost:3449.

If you want you can right click on the whiteness somewhere 1cm in from the right corner and press inspect in your browser, just to make sure that there's actually a canvas there.


## Thi.ng Geom - the helper library

In order to abstract ourselves away from calling `glVertexAttribPointer` and its cousins, but not above `GLSL` like [Three.js](https://github.com/cassiel/threejs-figwheel) would, I've chosen Karsten Schmidt's library [thi.ng/geom](https://github.com/thi-ng/geom). We need to declare this dependency in our `project.clj`. Your dependencies section should approximate:

```clojure
:dependencies [[org.clojure/clojure "1.8.0"]
               [org.clojure/clojurescript "1.10.339"]
               [org.clojure/core.async "0.2.385"
                :exclusions [org.clojure/tools.reader]]
               [thi.ng/geom "1.0.0-RC3"]]
```

To download the new dependency and restart figwheel, press Ctrl-c in the terminal you ran `lein figwheel` and then start it again.


### Namespaces

At the very top of `src/webgl_clojurescript_tutorial/core.cljs` you'll see the following call:

```clojure
(ns webgl-clojurescript-tutorial.core
  (:require ))
```

That is the declaration of your namespaece, an isolated piece of your program. Unlike JavaScript there is no global namespace. We now want to reference parts of the geom namespace:

```clojure
(ns webgl-clojurescript-tutorial.core
  (:require [thi.ng.geom.gl.core :as gl]))
```

We can now access things defined in `thi.ng.geom.gl.core` through the namespace qualifier `gl`. So let's do just that to create a [GL context](https://www.opengl.org/wiki/OpenGL_Context), our entry point to the OpenGL state machine.


### Let there be darkness

Beneath `(enable-console-print!)` in `core.cljs`, add:

```clojure
(defonce gl-ctx (gl/gl-context "main"))
```

This way we define, once and only once, the symbol `gl-ctx`; this way we'll never end up redefining the whole state machine once the application is up and running. But that doesn't stop us from mutating the state machine, so why not start by clearing its buffers? The below program should result in you having a block box on your screen.

```clojure
(ns webgl-clojurescript-tutorial.core
  (:require [thi.ng.geom.gl.core :as gl]))

(enable-console-print!)

(defonce gl-ctx (gl/gl-context "main"))

(doto gl-ctx
  (gl/clear-color-and-depth-buffer 0 0 0 1 1))
```

If we take a peek at the definition of `gl/clear-color-and-depth-buffer` that the arguments are `red green blue alpha depth`. You can find the definition of `gl/clear-color-and-depth-buffer` in ´resources/public/js/compiled/out/thi/ng/geom/gl/core.cljc´ or in Emacs with CIDER by pressing `M-.`. Play with the arguments a little and you'll probably understand.

Remember to commit your code with `git commit -v` at every point you have a working version.

Since Geom is open source we can just as easily do all the things `gl/gl-context` and`gl/clear-color-and-depth-buffer` does ourselves:

```clojure
(ns webgl-clojurescript-tutorial.core
  (:require [thi.ng.geom.gl.core :as gl]
            [thi.ng.geom.gl.webgl.constants :as glc]))

(enable-console-print!)

(def context-default-attribs
     {:alpha                                true
      :antialias                            true
      :depth                                true
      :fail-if-major-performance-caveat     false
      :prefer-low-power-to-high-performance false
      :premultiplied-alpha                  true
      :preserve-drawing-buffer              false
      :stencil                              false})

(defn gl-context
  ([canvas] (gl-context canvas {}))
  ([canvas attribs]
   (let [canvas  (if (string? canvas) (.getElementById js/document canvas) canvas)
         attribs (clj->js (merge context-default-attribs attribs))
         ctx     (loop [ids ["webgl" "experimental-webgl" "webkit-3d" "moz-webgl"]]
                   (when ids
                     (try
                       (let [ctx (.getContext canvas (first ids) attribs)]
                         (set! (.-onselectstart canvas) (constantly false))
                         (if ctx ctx (recur (next ids))))
                       (catch js/Error e (recur (next ids))))))]
     (or ctx (println "WebGL not available")))))

;;; The below defonce's cannot and will not be reloaded by figwheel.
(defonce gl-ctx (gl/gl-context "main"))

(doto gl-ctx
  (.clearColor 0 0 0 1)
  (.clearDepth 1)
  (.clear (bit-or 0x100 0x4000)))

```

That gets unwieldier and harder to grasp, but it's good to know that we have a fallback.

In case you went with the bloated version of the code, revert by calling `git commit reset --hard` from your bash terminal.




Hello Triangle
==============

Now comes the first big steps. We're going to define rudimentary fragment and vertex shaders along with a model to be drawn by them. In order for Geom to be able manage the upload of uniforms and models by itself, we must declare which they are in what's called a shader spec, a specification of the shader program.

We'll have to complete a whole bunch of steps before we can see our model on the screen. So hold on, you're in for a ride!


## The shader spec

Below is the shader spec we'll use for our first hello triangle program. It's a bit more advanced than it actually has to be, but if you've written GLSL before you'll be accustomed to the use of both the model, view, and projection matrixes as well as a position vector.

The shader spec itself is a map containing a vertex shader under the key `:vs`, a fragment shader under `:fs`, a map of uniforms and their type under `:uniforms` and finally the attribute `position` declared in the `:attribs` map.

```clojure
(def shader-spec
  {:vs "void main() {
          gl_Position = proj * view * vec4(position, 1.0);
       }"
   :fs "void main() {
           gl_FragColor = vec4(0.5, 0.5, 1.0, 1.0);
       }"
   :uniforms {:view       :mat4
              :proj       :mat4}
   :attribs  {:position   :vec3}})
```

So insert the above definition into `core.cljs` below the definition of `gl-ctx`. Do also include the Geom matrix library in your requirements:

```clojure
[thi.ng.geom.matrix :as mat]
```


## The model

In order to construct a model Geom starts out defining the abstract shape. For example a sphere with the radius of 0.6 imaginary units is described by.

```clojure
(ns webgl-clojurescript-tutorial.core (:require [thi.ng.geom.sphere :as sph]))
(sph/sphere 0.6)
```

And a triangle is likewise defined by its corners:

```clojure
(ns webgl-clojurescript-tutorial.core (:require [thi.ng.geom.triangle :as tri]))
(tri/triangle3 [[1 0 0] [0 0 0] [0 1 0]])
```

The geometry is then realised into a mesh of triangles, a buffer with a specific size ready for handover to WebGL. In the below snippet the mesh consists of three vec3's, hence the argument `3` to `glmesh/glmesh`.

```clojure
(def triangle (geom/as-mesh (tri/triangle3 [[1 0 0] [0 0 0] [0 1 0]])
                            {:mesh (glmesh/gl-mesh 3)}))
```

Insert the above definition of a triangle into `core.cljs` below the shader spec and insert the below requirements into your namespace declaration:

```clojure
[thi.ng.geom.core :as geom]
[thi.ng.geom.gl.glmesh :as glmesh]

```


## The viewport

We need a lens through which we can view the world, and for now the default geom camera will do.

```clojure
(ns webgl-clojurescript-tutorial.core (:require [thi.ng.geom.gl.camera :as cam]))
(defonce camera (cam/perspective-camera {}))
```

Now you might be asking yourself: "What exactly constitutes a camera?" Good question! Lets have a look!

If you've entered the above definition of the camera into your `core.cljs`, perhaps below the
definition of the GL context, and the dependency into your list of requirements you can below that enter:

```clojure
(println camera)
```

If you save the file and take a look at the developer console in your web browser you'll see that it's been neatly described for you. Alternatively if you've got Emacs with CIDER set up, and got a CLJS REPL going (technobabble at its best), you can press `C-c C-e` with your cursor at the parenthesis surrounding the call to `cam/perspective-camera` to get the same printout in your editor.

```clojure
{:aspect 1.7777777777777777                     ; Aspect ratio.
 :eye    #vec3 [0 0 2]                          ; Where we look from.
 :fov    45                                     ; The field of view.
 :up     #vec3 [0 1 0]                          ; Where up's at.
 :near   0.1                                    ; Near plane of the frustum.
 :proj   #object[thi.ng.geom.matrix.Matrix44]   ; Projection matrix.
 :target #vec3 [0 0 0]                          ; Where we're looking.
 :far    100                                    ; Far plane of the frustum.
 :view   #object[thi.ng.geom.matrix.Matrix44]}  ; [View matrix](http://www.3dgep.com/understanding-the-view-matrix/).
```

As you can see it's a map containing most of what you'd want out of a camera. As we'll soon see this map will be joined together with the map describing the shader before being passed to the render function of Geom. So hang in there just a little bit more, we're soon there!


## Putting it all together

We'll now introduce the [`->`](https://clojuredocs.org/clojure.core/-%3E) or thread-first macro which takes the first argument, in this case `model`, and places it as the first argument of its second argument and so on. Perhaps best explained through an example:

```clojure
(-> 5 (+ 3) (/ 2) (- 1))
; Returns 3
;; This can be explained by using macroexpand-all from clojure.walk:
(macroexpand-all '(-> 5 (+ 3) (/ 2) (- 1)))
; Returns (- (/ (+ 5 3) 2) 1)')
```

This macro is used to simplify code which would otherwise look pretty shaggy. For example:

```clojure
(defn combine-model-shader-and-camera
  [model spec camera]
  (cam/apply
   (gl/make-buffers-in-spec
    (assoc (gl/as-gl-buffer-spec model {}) :shader
           (shaders/make-shader-from-spec gl-ctx spec))
    gl-ctx glc/static-draw) camera))
```
can be rewritten as

```clojure
(defn combine-model-shader-and-camera
  [model shader-spec camera]
  (-> model
      (gl/as-gl-buffer-spec {})
      (assoc :shader (shaders/make-shader-from-spec gl-ctx shader-spec))
      (gl/make-buffers-in-spec gl-ctx glc/static-draw)
      (cam/apply camera)))
```

which is a great deal more readable once you understand what the macro does. The above function `combine-model-shader-and-camera` takes a model, shader-spec and camera and in order: Makes the model into a buffer-spec (a map), compiles and inserts the shader specified by `shader-spec` into on the field `:shader` of the map, creates the gl buffers specified by the shader-spec, puts the camera into the map.

Add `combine-model-shader-and-camera` to `core.cljs` underneath your model definition and add `[thi.ng.geom.gl.shaders :as shaders]` to your requirements list.

And now for the final step. Modify your `(doto ctx` so that it looks like:

```

(doto gl-ctx
  (gl/clear-color-and-depth-buffer 0 0 0 1 1)
  (gl/draw-with-shader (combine-model-shader-and-camera triangle shader-spec camera)))
```

and once you save `core.cljs` you should now see a fantastic blue triangle against a black background, or something along those lines.




Animation loop
==============

As you might have noticed by observing the script there's only ever one frame drawn for each save-figwheel-inject loop, in order to create a slightly more interactive program we'll have to remedy that. Specifically we'll have to modify the `(doto gl-ctx)` call so that it's run on some sort of a timer.



## Quick look into functional programming

Geom has built in functions to support this in the namespace `thi.ng.geom.gl.webgl.animator` so add this to your requirements and make it available under then name `anim`. If you inspect the namespace `anim` in our editor you'll find that there's really only one function of interest to us `animate`. We'll take some time here for a gentle introduction to higher order functions and closures; both concepts commonly found in modern languages, the latter of which exists entirely separately from the language Clojure.

If we inspect the signature of animate you'll find that it has two: [f] or [f elem]. `f` is by convention a name used for when passing around functions, `fun`, `func` or `function` are also common. Again, how you inspect a function is specific to your editor, if you're unsure you can always look it up by opening the file ´resources/public/js/compiled/out/thi/ng/geom/gl/webgl/animator.cljs´; in Emacs with company-mode you press f1 or C-h when the auto complete appears.

Taking a look at the body of the first definition we find that the single argument version of `animate` simply calls its two-argument version with `nil` as the second argument, ´nil´ which in other languages is called `null` or `None`.

Inside the body of the dual argument version we find a closure defined by the function let. A closure is a [lexical scope](https://blog.rjmetrics.com/2012/01/11/lexical-vs-dynamic-scope-in-clojure/), an anonymous namespace local to a position in code, in which symbols can be looked up. Specifically the symbol `t0` is defined to hold the value of the time at the creation of the lexical scope, `fid`, frame id, is defined as a volatile variable starting at 0. And finally `f'`is given a local name `animate*` and defined as:

If the original function `f` passed to animate returns true given the time in seconds since first frame, increase the `fid` by one and queue another frame to be drawn in the future with the `animate*` function.

This type of function wrapping is quite common in modern languages, in Python for example there's a special syntax for this behaviour called [decorators](http://simeonfranklin.com/blog/2012/jul/1/python-decorators-in-12-steps/).



## Constructing a animation function

Based on the information we've gathered above and your previous knowledge of Clojure, try to write a call to `anim/animate` on your own! But in order to actually see that you're rendering multiple frames you need something to be different between the frames. So let's define some state that we can safely mutate into our program, introducing the [atom](https://clojuredocs.org/clojure.core/atom).



### The atom

An atom can be [atomically](https://en.wikipedia.org/wiki/Atomicity_(programming)) written to and read from, i.e. as if every operation was done synchronously even though they in reality aren't. State which you want to mutate throughout the run of your program is typically well placed in an atom.

It's now time to bring out a [REPL](http://web.clojurerepl.com/) and play around a bit, and I strongly encourage you to try play on your own and not just read what I'm doing.

```clojure
user=> (def a (atom 10))
#'user/a
user=> @a
10
user=> (swap! a inc)
11
user=> (swap! a inc)
12
user=> (swap! a (fn [n] (* 2 n)))
24
user=> (swap! a #(/ % 2))
12
```

In the above text REPL-interaction I define an atom `a` and then swap its content with the result of a whole bunch of different functions with the original value as an argument.

Define an atom for what you want to animate in your program. I'll go with the red clear color:

```clojure
(defonce red (atom 0))
```
And then add a function which mutates your state atom inside what will become your core rendering loop:

```clojure
(doto gl-ctx
  (gl/clear-color-and-depth-buffer (swap! red #(mod (+ % 0.1) 1)) 0 0 1 1)
  (gl/draw-with-shader (combine-model-shader-and-camera triangle shader-spec camera)))
```

Right. Now we're ready to make this one spin right round!


### Writing the function

Okey, throwback: You want to call `animate` from `thi.ng.geom.gl.webgl.animator`. The first argument given to `animate` should be a function. This function should take one argument `t` and return `true`.



### A solution

In order to continue reading you got to promise me that you've either solved the above task, or had a honest try at it; promise? Good let me share my solution:

```clojure
(anim/animate
 (fn [t]
   (doto gl-ctx
     (gl/clear-color-and-depth-buffer (swap! red #(mod (+ % 0.001) 1)) 0 0 1 1)
     (gl/draw-with-shader (combine-model-shader-and-camera triangle shader-spec camera))) true)))
```

Now: There's one problem with this solution which you'll find with this program. If you modify the literal 0.001 to perhaps 0.1 and then back again you'll find that the animation speed doesn't really drop back. This is because the `(anim/animate f)` passes `f`, or rather it's modified `f'`, into a browser internal function queue, and for every time we modify our anonymous function defined by `(fn [t]` we add another copy to that queue without a way to remove it.

So there are two ways around this: Either we implement a way for our functions to return false, or we make sure to only queue our function once. The latter is rather simple to implement while still retaining the ability to modify `f` if we give `f` a name in our scope, so we'll go with that.

The first step is to make sure that we only register our animation function once, so wrap the `anim/animate` call in a `defonce`:

```clojure
(defonce running
  (anim/animate
   (fn [t]
     (doto gl-ctx
       (gl/clear-color-and-depth-buffer (swap! red #(mod (+ % 0.1) 1)) 0 0 1 1)
       (gl/draw-with-shader (combine-model-shader-and-camera triangle shader-spec camera))) true)))
```

If you modify the anonymous function we defined in `(fn` now you'll see how the behaviour doesn't change in our application, we're not registering our function more than once. But in order to modify the function we do register we have to refactor it out and give it a name:



```clojure
(defn draw-frame! []
  (doto gl-ctx
    (gl/clear-color-and-depth-buffer (swap! red #(mod (+ % 0.001) 1)) 0 0 1 1)
    (gl/draw-with-shader (combine-model-shader-and-camera triangle shader-spec camera))))

(defonce running
  (anim/animate (fn [t] (draw-frame!) true)))
```

And the reason we call our frame-drawing function `draw-frame!` with an exclamation mark at the end is because it's not [pure](https://en.wikipedia.org/wiki/Pure_function), it has [side effects](https://en.wikipedia.org/wiki/Side_effect_(computer_science)) outside of its call stack. And in Clojure the convention is to mark impure functions with a bang at the end.

Short food for thought: Functions without return values are always either impure or pointless.



## Morphing the model

Now, most animation for the most part isn't about changing the clear color but about moving or morphing models; so let's do some rotation!


### Code checkpoint

Let's start by removing the clear color stuff, your whole `core.cljs` should now look like this:

```clojure
(ns webgl-clojurescript-tutorial.core
  (:require [thi.ng.geom.gl.core :as gl]
            [thi.ng.geom.matrix :as mat]
            [thi.ng.geom.core :as geom]
            [thi.ng.geom.triangle :as tri]
            [thi.ng.geom.gl.glmesh :as glmesh]
            [thi.ng.geom.gl.shaders :as shaders]
            [thi.ng.geom.gl.webgl.constants :as glc]
            [thi.ng.geom.gl.camera :as cam]
            [thi.ng.geom.gl.webgl.animator :as anim]))

(enable-console-print!)

;;; The below defonce's cannot and will not be reloaded by figwheel.
(defonce gl-ctx (gl/gl-context "main"))
(defonce camera (cam/perspective-camera {}))

(def shader-spec
  {:vs "void main() {
          gl_Position = proj * view * vec4(position, 1.0);
       }"
   :fs "void main() {
           gl_FragColor = vec4(0, 0.5, 1.0, 1.0);
       }"
   :uniforms {:view       :mat4
              :proj       :mat4}
   :attribs  {:position   :vec3}})


(def triangle (geom/as-mesh
               (tri/triangle3 [[1 0 0] [0 0 0] [0 1 0]])
               {:mesh (glmesh/gl-mesh 3)}))


(defn combine-model-shader-and-camera
  [model shader-spec camera]
  (-> model
      (gl/as-gl-buffer-spec {})
      (assoc :shader (shaders/make-shader-from-spec gl-ctx shader-spec))
      (gl/make-buffers-in-spec gl-ctx glc/static-draw)
      (cam/apply camera)))

(defn draw-frame! [t]
  (doto gl-ctx
    (gl/clear-color-and-depth-buffer 0 0 0 1 1)
    (gl/draw-with-shader (combine-model-shader-and-camera triangle shader-spec camera))))

(defonce running
  (anim/animate (fn [t] (draw-frame! t) true)))
```

If you recall your graphics programming course it's common to keep model local transformations in a 4×4 matrix uploaded as a uniform to be applied to the model on the graphics card. That's exacly what we're going to do now.

### Defining and passing the transform

Let's start start off by creating a function which takes the amount of time passed since the start of the program and returns a 4×4 matrix rotation matrix.

```clojure
(defn spin
  [t]
  (geom/rotate-y  mat/M44 (/ t 10)))
```
What we'll do now is to attach the result of this function to the map which in the end is what's passed to `gl/draw-with-shader`.

So let's inspect that map for a second. In `core.cljs`, temporarily add `(println (combine-model-shader-and-camera triangle shader-spec camera))` to see what it's that we're passing on.


```clojure
{:attribs
 {:position {:data        #object[Float32Array 1, ... ,0],
             :size        3,
             :buffer      #object[WebGLBuffer [object WebGLBuffer]],
             :target      34962,
             :buffer-mode 35044}},
 :num-vertices 3,
 :mode 4,
 :shader {:vs "code ...",
          :fs "code ...",
          :uniforms {:view {:type    :mat4,
                            :default nil,
                            :setter  #object[Function "code ..."],
                            :loc     #object[WebGLUniformLocation]},
                     :proj {:type    :mat4,
                            :default nil,
                            :setter  #object[Function "code ..."],
                            :loc     #object[WebGLUniformLocation]}},
          :attribs  {:position 0},
          :program  #object[WebGLProgram [object WebGLProgram]]},
 :uniforms {:view #object[thi.ng.geom.matrix.Matrix44],
            :proj #object[thi.ng.geom.matrix.Matrix44]}}
```

Oh yeah, that's a map. With a lot of stuff in it. Actually it's everything that's needed to compile and run the GPU program we've described so far. And what we want to do is to add a matrix in the `:uinforms` map under the key `:model`, specify that it's a uniform in our shader spec and finally write its use in the vertex shader.

So let's attach the output of `spin` to the map given by `combine-model-shader-and-camera` in `core.cljs`. Also, pass on `t` from the anonymous function we give `anim/animate` to `draw-frame!`, you'll have to reload the page to redefine the `defonce`.

```clojure
(defn draw-frame! [t]
  (doto gl-ctx
    (gl/clear-color-and-depth-buffer 0 0 0 1 1)
    (gl/draw-with-shader (assoc-in (combine-model-shader-and-camera triangle shader-spec camera)
                                   [:uniforms :model] (spin t)))))
```
And here is one interesting function: `assoc-in`. It takes a map as its first argument, inserts its third argument into the path given by the second argument. Take a look at the map three code-boxes up, under the keyword `:uniforms` it will insert a new keyword `model` with the value of what `(spin t)` returns. Cool huh?

Either way, now the data's in place. But then we've got to declare the data in the shader-spec so that geom can manage the upload of the data. So modify your definition of `shader-spec` so that the `:uniforms` map now looks like:

```clojure
:uniforms {:view  :mat4
           :proj  :mat4
           :model :mat4}
```

And finally add model into your vertex shader `:vs` in your `shader-spec`.

```clojure
:vs "void main() {
   gl_Position = proj * view * model * vec4(position, 1.0);
}"
```

And what do we got here if not a spinning triangle?




Beyond this tutorial
====================

There are a whole bunch of [examples available in the geom README.md](https://github.com/thi-ng/geom/tree/develop#example-usage). You can also take a look at [this template](https://github.com/Rovanion/webgl-figwheel-template) which has a structure very close to this tutorial, see if you can continue writing on your tutorial code until it has textures and other useful things.

You can also try to find the bug in the program you've just created where the shaders are rebuilt for each and every frame, something which eventually causes Firefox to crash due to being out of memory as it [doesn't seem to garbage collect its shader cache](https://bugzilla.mozilla.org/show_bug.cgi?id=1319426).
