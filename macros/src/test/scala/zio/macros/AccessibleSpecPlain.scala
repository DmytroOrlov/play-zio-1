package zio.macros

import zio._
import zio.stream._
import zio.test.Assertion._
import zio.test._

object AccessibleSpecPlain extends DefaultRunnableSpec {

  def spec = suite("AccessibleSpecPlain")(
    suite("Accessible macro")(
      testM("compiles when applied to object with empty Service") {
        assertM(typeCheck {
          """
            @accessible
            trait Module
          """
        })(isRight(anything))
      },
      testM("fails when applied to object without a Service") {
        assertM(typeCheck {
          """
            @accessible
            object Module
          """
        })(isLeft(anything))
      },
/*
      testM("fails when applied to trait") {
        assertM(typeCheck {
          """
            @accessible
            trait Module
          """
        })(isLeft(anything))
      },
*/
      testM("fails when applied to class") {
        assertM(typeCheck {
          """
            @accessible
            class Module
          """
        })(isLeft(anything))
      },
      testM("generates accessor for values") {
        assertM(typeCheck {
          """
            @accessible
            trait Module {
                val foo: ZIO[Any, Nothing, Unit]
            }

            object Check {
              val foo: ZIO[Has[Module], Nothing, Unit] =
                Module.>.foo
            }
          """
        })(isRight(anything))
      },
      testM("add accessor to object") {
        assertM(typeCheck {
          """
            @accessible
            trait Module {
                val foo: ZIO[Any, Nothing, Unit]
            }

            object Module {
                val make = "making"
            }

            object Check {
              val foo: ZIO[Has[Module], Nothing, Unit] =
                Module.>.foo
            }
          """
        })(isRight(anything))
      },
      testM("generates accessor for functions") {
        assertM(typeCheck {
          """
            @accessible
            trait Module {
                def foo(i: Int): ZIO[Any, Nothing, Unit]
            }

            object Check {
              def foo(i: Int): ZIO[Has[Module], Nothing, Unit] =
                Module.>.foo(i)
            }
          """
        })(isRight(anything))
      },
      testM("generates accessor for varargs functions") {
        assertM(typeCheck {
          """
            @accessible
            trait Module {
                def varargsFoo(a: Int, b: Int*): ZIO[Any, Nothing, Unit]
            }

            object Check {
              def varargsFoo(a: Int, b: Int*): ZIO[Has[Module], Nothing, Unit] =
                Module.>.varargsFoo(a, b: _*)
            }
          """
        })(isRight(anything))
      },
      testM("generates accessors for all capabilities") {
        assertM(typeCheck {
          """
            @accessible
            trait Module {
                val static                                 : ZIO[Any, Nothing, String]
                def zeroArgs                               : ZIO[Any, Nothing, Int]
                def zeroArgsWithParens()                   : ZIO[Any, Nothing, Long]
                def singleArg(arg1: Int)                   : ZIO[Any, Nothing, String]
                def multiArgs(arg1: Int, arg2: Long)       : ZIO[Any, Nothing, String]
                def multiParamLists(arg1: Int)(arg2: Long) : ZIO[Any, Nothing, String]
                def typedVarargs[T](arg1: Int, arg2: T*)   : ZIO[Any, Nothing, T]
                def command(arg1: Int)                     : ZIO[Any, Nothing, Unit]
                def overloaded(arg1: Int)                  : ZIO[Any, Nothing, String]
                def overloaded(arg1: Long)                 : ZIO[Any, Nothing, String]
                def function(arg1: Int)                    : String
                def sink(arg1: Int)                        : ZSink[Any, Nothing, Nothing, Int, List[Int]]
                def stream(arg1: Int)                      : ZStream[Any, Nothing, Int]
            }

            object Check {
              val static                                 : ZIO[Has[Module], Nothing, String] = Module.>.static
              def zeroArgs                               : ZIO[Has[Module], Nothing, Int]    = Module.>.zeroArgs
              def zeroArgsWithParens()                   : ZIO[Has[Module], Nothing, Long]   = Module.>.zeroArgsWithParens()
              def singleArg(arg1: Int)                   : ZIO[Has[Module], Nothing, String] = Module.>.singleArg(arg1)
              def multiArgs(arg1: Int, arg2: Long)       : ZIO[Has[Module], Nothing, String] = Module.>.multiArgs(arg1, arg2)
              def multiParamLists(arg1: Int)(arg2: Long) : ZIO[Has[Module], Nothing, String] = Module.>.multiParamLists(arg1)(arg2)
              def typedVarargs[T](arg1: Int, arg2: T*)   : ZIO[Has[Module], Nothing, T]      = Module.>.typedVarargs[T](arg1, arg2: _*)
              def command(arg1: Int)                     : ZIO[Has[Module], Nothing, Unit]   = Module.>.command(arg1)
              def overloaded(arg1: Int)                  : ZIO[Has[Module], Nothing, String] = Module.>.overloaded(arg1)
              def overloaded(arg1: Long)                 : ZIO[Has[Module], Nothing, String] = Module.>.overloaded(arg1)
              def function(arg1: Int)                    : ZIO[Has[Module], Throwable, String] = Module.>.function(arg1)
              def sink(arg1: Int)                        : ZIO[Has[Module], Nothing, ZSink[Any, Nothing, Nothing, Int, List[Int]]] = Module.>.sink(arg1)
              def stream(arg1: Int)                      : ZIO[Has[Module], Nothing, ZStream[Any, Nothing, Int]] = Module.>.stream(arg1)
            }
          """
        })(isRight(anything))
      }
    )
  )
}
