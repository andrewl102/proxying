/*
package controllers

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Flow, Sink, Source, Tcp }
import akka.util.ByteString
import scala.collection.immutable.IndexedSeq
import scala.util.{ Failure, Success }

object TcpEcho {

  /**
    * Use without parameters to start both client and
    * server.
    *
    * Use parameters `server 0.0.0.0 6001` to start server listening on port 6001.
    *
    * Use parameters `client 127.0.0.1 6001` to start client connecting to
    * server on 127.0.0.1:6001.
    *
    */
  def main(args: Array[String]): Unit = {
    if (args.isEmpty) {
      val system = ActorSystem("ClientAndServer")
      val (address, port) = ("127.0.0.1", 6000)
//      server(system, address, port)
      client(system, address, port)
    } else {
      val (address, port) =
        if (args.length == 3) (args(1), args(2).toInt)
        else ("127.0.0.1", 6000)
      if (args(0) == "server") {
        val system = ActorSystem("Server")
        server(system, address, port)
      } else if (args(0) == "client") {
        val system = ActorSystem("Client")
        client(system, address, port)
      }
    }
  }

  def server(system: ActorSystem, address: String, port: Int): Unit = {
    implicit val sys = system
    import system.dispatcher
    implicit val materializer = ActorMaterializer()

    val handler = Sink.foreach[Tcp.IncomingConnection] { conn =>
      println("Client connected from: " + conn.remoteAddress)
      conn handleWith Flow[ByteString]
    }

    val connections = Tcp().bind(address, port)
    val binding = connections.to(handler).run()

    binding.onComplete {
      case Success(b) =>
        println("Server started, listening on: " + b.localAddress)
      case Failure(e) =>
        println(s"Server could not bind to $address:$port: ${e.getMessage}")
        system.shutdown()
    }

  }

  def client(system: ActorSystem, address: String, port: Int): Unit = {
    implicit val sys = system
    import system.dispatcher
    implicit val materializer = ActorMaterializer()

    val testInput: IndexedSeq[ByteString] = ('a' to 'z').map(ByteString(_))

    //    Source(te)

    val writeSource: Source[ByteString, NotUsed] = Source(testInput).via(Tcp().outgoingConnection(address, port))

    //    val result = writeSource.runForeach()

    val result = writeSource.
      runFold(ByteString.empty) { (acc, in) ⇒ acc ++ in }

    result.onComplete {
      case Success(result2) =>
        println(s"Result: " + result2.utf8String)
      //        println("Shutting down client")
      //        system.shutdown()
      case Failure(e) =>
        println("Failure: " + e.getMessage)
        system.shutdown()
    }
    Thread.sleep(3000L)

    val result2 = writeSource.runFold(ByteString.empty) { (acc, in) ⇒ acc ++ in }

    result2.onComplete { case Success(rx) =>
      println(s"ResultX: " + rx.utf8String)
    }
  }
}*/
