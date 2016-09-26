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
(defonce red (atom 0))

(def shader-spec
  {:vs "void main() {
          gl_Position = proj * view * model * vec4(position, 1.0);
       }"
   :fs "void main() {
           gl_FragColor = vec4(0, 0.5, 1.0, 1.0);
       }"
   :uniforms {:model      [:mat4 mat/M44]
              :view       :mat4
              :proj       :mat4}
   :attribs  {:position   :vec3}})


(def model (geom/as-mesh
            (tri/triangle3 [[1 0 0] [0 0 0] [0 1 0]])
            {:mesh (glmesh/gl-mesh 3)}))


(defn combine-model-shader-and-camera
  [model spec camera]
  (cam/apply
   (gl/make-buffers-in-spec
    (assoc (gl/as-gl-buffer-spec model {}) :shader
           (shaders/make-shader-from-spec gl-ctx spec))
    gl-ctx glc/static-draw) camera))


(anim/animate
 (fn [t]
   (doto gl-ctx
     (gl/clear-color-and-depth-buffer (swap! red #(mod (+ % 0.001) 1)) 0 0 1 1)
     (gl/draw-with-shader (combine-model-shader-and-camera model shader-spec camera))) true))

(defn on-js-reload [])
