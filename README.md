<!-- markdown-toc start - Don't edit this section. Run M-x markdown-toc-generate-toc again -->
**Table of Contents**

- [WebGL ClojureScript Tutorial](#webgl-clojurescript-tutorial)
    - [Introduction](#introduction)
    - [File Hiarchy](#file-hiarchy)
    - [An introduction to figwheel](#an-introduction-to-figwheel)
    - [Getting started](#getting-started)
        - [The canvas](#the-canvas)
        - [Thi.ng Geom - the helper library](#thing-geom---the-helper-library)

<!-- markdown-toc end -->
WebGL ClojureScript Tutorial
============================

This document will guide you through the basics of setting up a WebGL application in ClojureScript using thi.ng/geom and Figwheel.




Introduction
------------

Hi, and welcome!

This guide assumes basic knowledge OpenGL and [GLSL]() and rudimentary understanding of the [Clojure syntax](http://www.tryclj.com/) . It's aimed at those who want to leveredge the zero iteration time development environment provided by [figwheel](https://www.youtube.com/watch?v=KZjFVdU8VLI) to make 3D applications.

In order to follow this guide you'll want to have [Leiningen](http://leiningen.org/) and [Git](https://git-scm.com/) along with your favourite text editor ([Emacs](https://www.gnu.org/software/emacs/) in case you haven't decided).

With that done, we're off to the races!



File Hiarchy
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

The most important files here are `index.html`, `style.css` and `core.cljs`; they're the sum of our code for now. The HTML and CSS are going to be delivered as they are by the web server; core.cljs on the other hand is has to be compiled before it can be delivered to the web browser.

In other words it's getting time to start our compiling conductor/web server/magic machine Figwheel! But before we do anything crazy we want to initiate a new git repository so that we'll always be able to land safely in a previous commit.

    git init
    git add src/* resources/public/index.html resources/public/css/style.css README.md project.clj dev/user.clj
		git commit -v

The file project.clj contains the Leiningen defenition of our project, it tells Leiningen where our source code is and how to compile it; but perhaps most importantly it contains the list of dependencies we're building on. We'll have to edit this file every time we want to build on the shoulders of giants.



An introduction to figwheel
---------------------------

[Figwheel](https://www.youtube.com/watch?v=j-kj2qwJa_E) by Bruce Hauman allows for code to dynamically be injected into your running web application without destroying it's state, provided you follow a few simple rules about how you handle your state.

As a fast and hard rule: Muteable state should be declared like:

```clojure
    (defonce app-state (atom 0))
```

and to be read with [`@`](https://clojuredocs.org/clojure.core/deref) and modified with [`!swap`](https://clojuredocs.org/clojure.core/swap!) like:

```clojure
    (swap! app-state function-which-mutates-state argument-to-function-if-any)
```

But since an interactive programming session says more than a thousand words, go ahead and start up figwheel!

    lein figwheel

Once done chopping its fruits your browser will have opened a new tab displaying your application. If you open your developer console you'll see that there's a message printed there for you. If we modify string literal given as the first argument to `println` and save the file we'll see the modified string printed to the console.

This within itself isn't all that interesting, we'll need a more complex program to illustrate the true prowess of Figwheel. So go ahead and delete the `println` to give room for our WebGL program.

If you want to play a bit more with Figwheel before continuing I recommend trying out the [flappy bird demo by its author](https://github.com/bhauman/flappy-bird-demo/blob/master/project.clj).



Getting started
---------------

In order to get started with WebGL there's one very important aspect missing in our `index.html` - a canvas.

### The canvas

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


### Thi.ng Geom - the helper library

In order to abstract ourselves away from calling `glVertexAttribPointer` and its cousins, but not above `GLSL` like [Three.js](https://github.com/cassiel/threejs-figwheel) would, I've chosen Karsten Schmidts library [thi.ng/geom](https://github.com/thi-ng/geom). We need to declare this dependency in our `project.clj`. Your dependencies section should approximate:

```clojure
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.89"]
                 [org.clojure/core.async "0.2.385"
                  :exclusions [org.clojure/tools.reader]]
                 [thi.ng/geom "0.0.1178-SNAPSHOT"]]
```

To download the new dependency and restart figwheel, press Ctrl-c in the terminal you ran `lein figwheel` and then start it again.


#### Namespaces

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

We can now access things defined in `thi.ng.geom.gl.core` through the namespace qualifier `gl`. So lets do just that to create a [GL context](https://www.opengl.org/wiki/OpenGL_Context), our entry point to the OpenGL state machine.


#### Let there be darkness

Beneath `(enable-console-print!)` in `core.cljs`, add:

```clojure
(defonce gl-ctx (gl/gl-context "main"))
```

This way we define, once and only once, the symbol `gl-ctx`. Why not start mutating the state machine by clearing its buffers? The below program should result in you having a block box on your screen.

```clojure
(ns webgl-clojurescript-tutorial.core
  (:require [thi.ng.geom.gl.core :as gl]))

(enable-console-print!)

(defonce gl-ctx (gl/gl-context "main"))

(doto gl-ctx
  (gl/clear-color-and-depth-buffer 0 0 0 1 1))

```
If we take a peek at the defenition of `gl/clear-color-and-depth-buffer`, in Emacs with CIDER by pressing `M-.`, that the arguments are `red green blue alpha depth`. Play with the arguments a little and you'll probably understand.

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

That gets unwieldier and harder to grasp, but it's good to know that you have a fallback.

In case you went with the bloated version of the code, revert by calling `git commit reset --hard` from your bash terminal.
