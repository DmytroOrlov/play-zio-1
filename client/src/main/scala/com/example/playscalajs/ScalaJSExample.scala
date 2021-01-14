package com.example.playscalajs

import com.example.playscalajs.shared.SharedMessages
import org.scalajs.dom
import org.scalajs.dom.Node
import zio._

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

@js.native
@JSGlobal("THREE.Scene")
class Scene extends js.Object {
  def add(obj: js.Object): Unit = js.native
}

@js.native
@JSGlobal("THREE.PerspectiveCamera")
class PerspectiveCamera(a: Double, b: Double, c: Double, d: Double) extends Camera {
  var position: Vector3 = js.native
}

@js.native
@JSGlobal("THREE.WebGLRenderer")
class WebGLRenderer(params: js.Dynamic) extends js.Object {
  def setSize(width: Double, height: Double, updateStyle: Boolean = js.native): Unit = js.native

  def render(scene: Scene, camera: Camera, renderTarget: RenderTarget = js.native, forceClear: Boolean = js.native): Unit = js.native

  var domElement: Node = js.native
}

@js.native
@JSGlobal("THREE.Camera")
class Camera extends js.Object

trait RenderTarget extends js.Object

@js.native
@JSGlobal("THREE.Vector3")
class Vector3(var x: Double, var y: Double, var z: Double) extends js.Object

@js.native
@JSGlobal("THREE.Mesh")
class Mesh(geometry: Geometry, material: Material) extends js.Object {
  var rotation: Euler = js.native
}

class Geometry extends js.Object

class Material extends js.Object

@js.native
@JSGlobal("THREE.BoxGeometry")
class BoxGeometry(
    width: Double,
    height: Double,
    depth: Double,
    widthSegments: Double = js.native,
    heightSegments: Double = js.native,
    depthSegments: Double = js.native,
) extends Geometry

@js.native
@JSGlobal("THREE.MeshBasicMaterial")
class MeshBasicMaterial(params: js.Dynamic) extends Material

@js.native
@JSGlobal("THREE.Euler")
class Euler extends js.Object {
  var x: Double = js.native
  var y: Double = js.native
}

object ScalaJSExample extends App {
  def run(args: List[String]) = {
    IO {
      val scene = new Scene()
      val camera = new PerspectiveCamera(75, dom.window.innerWidth / dom.window.innerHeight, 0.1, 1000)
      val renderer = new WebGLRenderer(js.Dynamic.literal())
      renderer.setSize(dom.window.innerWidth, dom.window.innerHeight)
      dom.document.body.appendChild(renderer.domElement)
      camera.position.z = 5

      def createCube(side: Double): Mesh = {
        val geometry = new BoxGeometry(side, side, side)
        val material = new MeshBasicMaterial(js.Dynamic.literal(color = 0x00ff00, wireframe = true))
        val cube = new Mesh(geometry, material)
        cube
      }

      val cube = createCube(3)
      scene.add(cube)

      def renderLoop(timestamp: Double): Unit = {
        dom.window.requestAnimationFrame(renderLoop _)
        cube.rotation.x += .03
        cube.rotation.y += .1
        renderer.render(scene, camera)
      }

      renderLoop(System.currentTimeMillis())
      dom.document.getElementById("scalajsShoutOut").textContent = SharedMessages.itWorks
    }.exitCode
  }
}
