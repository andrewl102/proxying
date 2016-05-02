package controllers

import javax.inject.Singleton

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Tcp.OutgoingConnection
import akka.stream.scaladsl.{Flow, Sink, Source, Tcp}
import akka.util.ByteString

import scala.collection.immutable.IndexedSeq
import scala.concurrent.Future
import scala.util.{Failure, Success}

@Singleton
object ScrConnector {
  implicit val system = ActorSystem("scrConnector")
  import system.dispatcher
  implicit val materializer = ActorMaterializer()

  private var tcpConnection:Option[Flow[ByteString, ByteString, Future[OutgoingConnection]]] = None

  def main(args:Array[String]):Unit = {
    startServer()
    startClient()
    val authAction = AuthAction("1234567890123456","100",Some("MERCHANT REFERENCE 12345678"))
    sendMessage(authAction.asByteString)
//    sendMessage("abc123")
//    sendMessage("aaa")
//    sendMessage("cdef")
  }

  def startServer() = server("127.0.0.1", 6000)
  def startClient() = tcpConnection = Some(Tcp().outgoingConnection("127.0.0.1", 6000))

  def server(address: String, port: Int): Unit = {
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

  def client(system: ActorSystem, address: String, port: Int): Unit =  tcpConnection = Some(Tcp(system).outgoingConnection(address, port))

  def sendMessage(byteString: ByteString): Unit ={
    val result = Source(Vector(byteString)).via(tcpConnection.get).runFold(ByteString.empty) { (acc, in) ⇒ acc ++ in }
    result.onComplete {
      case Success(result2) =>
        println(s"Result: " + result2.utf8String)
      case Failure(e) =>
        println("Failure: " + e.getMessage)
        system.shutdown()
    }
  }
}

//Unused : after billing id in order: SlotId, OneSwipeFlag,  ( OemDataFormat,OemData not in example)
//Don't forget to CR
case class AuthAction(txnRef:String,amount:String,merchantReference:Option[String]=None,billingId:Option[String]=None) extends ScrAction {
  //  def asList:List[String]=List("TXN","AUTH",txnRef,amount,merchantReference.getOrElse(""),billingId.getOrElse(""),"","","","")
  def asList:List[String]=List("TXN","AUTH",txnRef,amount,merchantReference.getOrElse(""),billingId.getOrElse(""),"","")
}

case class CompAction(cmdSeq:String,amount:String,billingId:Option[String]=None,oemDataFormat:Option[String],oemData:Option[String]) extends ScrAction {
  def asList:List[String]=List("TXN","COMP",cmdSeq,amount,""/*SlotId*/,oemDataFormat.getOrElse(""),oemData.getOrElse(""))
}

sealed trait ScrAction {
  def asList:List[String]
  def asByteString:ByteString = ByteString(asList.mkString("~").toCharArray.map(_.toByte) ++ Array('\r'.toByte))
}

sealed trait Response
sealed trait ApprovableResponse extends Response {
  def approved:Boolean = responseCode == "00"
  def responseCode:String
}

case class CompResponse(cmdSeq:String,responseCode:String,txnRef:String) extends ApprovableResponse
case class AuthResponse(txnRef:String,responseCode:String,amount:String, dpsTxnRef:String) extends ApprovableResponse

class NonTxResponse extends Response

object CompResponse {
  def fromString(input:String) = fromList(input.split("~").toList)
  def fromList(input:List[String]) = input match {
    case List(cmdSeq,re,txnRef) => CompResponse(cmdSeq,re,txnRef)
    case _ => throw new IllegalStateException(s"String $input was not in expected format for AuthResponse")
  }
}


object AuthResponse {
  def fromString(input:String) = fromList(input.split("~").toList)
  def fromList(input:List[String]) = input match {
    case List(tx,re,amount,dpsRef) => AuthResponse(tx,re,amount,dpsRef)
    case _ => throw new IllegalStateException(s"String $input was not in expected format for AuthResponse")
  }
}

object ResponseParser {
  def parse(input:String):Response = input.split("~").toList match {
    case "txn" :: rest => parseTxn(rest)
    case "msg" :: rest => new NonTxResponse()
    case _ => throw new IllegalArgumentException(s"Unable to recognize input of $input")
  }
  def parseTxn(input:List[String]):Response = {
    input match {
      case "comp" :: rest => CompResponse.fromList(rest)
      case "auth" :: rest => AuthResponse.fromList(rest)
      case _ => throw new IllegalArgumentException(s"Unable to recognize txn input of $input")
    }
  }
}

object runner extends App {
  private val parse: Response = ResponseParser.parse("txn~auth~1~00~1000~0000000f0000008c~")
  parse match {
    case a:ApprovableResponse => println(a.approved,a.responseCode)
    case a:NonTxResponse =>
  }
  println(parse)


  //TXN~AUTH~1234567890123456~100~MERCHANT REFERENCE 12345678~~~
  //TXN~AUTH~1234567890123456~100~MERCHANT REFERENCE 12345678~~~
  //  println(AuthAction("1234567890123456","100",Some("MERCHANT REFERENCE 12345678")).asList.mkString("~"))
  //  println(AuthResponse.fromString("txn~auth~1~00~1000~0000000f0000008c~").approved)
  //  println(AuthResponse.fromString("txn~auth~1~76~1000~0000000f0000008c~").approved) //Lower space or upper?
  //  println(CompResponse.fromString("txn~comp~1~00~1234~").approved)

}

//TransactionAction(List("AUTH","1234567890123456","100","MERC REF 123")

/*case class TransactionAction(args:Seq[CTuple]=List(CTuple("AUTH","1234567890123456"))) {
}*/