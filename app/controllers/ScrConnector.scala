package controllers

import javax.inject.Singleton

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Tcp.OutgoingConnection
import akka.stream.scaladsl.{Flow, Sink, Source, Tcp}
import akka.util.ByteString

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

object ScrConnector extends ScrConnector {
  def main(args: Array[String]): Unit = {
    startServer()
    startClient()
    val authAction = AuthAction("1234567890123456", "100", "MERCHANT REFERENCE 12345678")
//    sendMessage(authAction.asByteString)
//    sendMessage(authAction.asByteString)
//    sendMessage(authAction.asByteString)

      sendCommand(authAction)
      sendCommand(CompAction("theCmdSeq","100"))
//      sendCommand(null)
//      sendCommand(null)
//      sendCommand(null)


//    println("Here")
//    sendCommand(null)
    //    sendMessage("abc123")
    //    sendMessage("aaa")
    //    sendMessage("cdef")
  }
}

@Singleton
class ScrConnector {

/*  def sendCommand(action:ScrAction): Response = {
    //    sendMessage(action.asByteString)
//    sendMessage(ByteString("GET /index.html HTTP/1.1\r\n"))
    sendMessage(ByteString("GET / HTTP/1.0\n\n"))
    //    sendMessage(ByteString("GET /index.html HTTP/1.1"))
  }

  def sendMessage(byteString: ByteString): Response = {
    println(s"sending $byteString")
//    val result = Source(Vector(byteString)).via(tcpConnection.get).runFold(ByteString.empty) { (acc, in) ⇒ acc ++ in }
//    val result1: ByteString = Await.result(result, Duration("20s"))
//    var responseString: String = result1.utf8String
    //We handle all messages synchronously with a 20 second timeout
    //Check and strip the CR character from the response. Perhaps we should keep blocking until it is available
    //    if(responseString.endsWith("\r")) ResponseParser.parse(responseString.substring(0,responseString.length))
    //    else throw new IllegalArgumentException
//    println(responseString)
//    println(result1)
//    null
    val socket: Socket = new Socket("127.0.0.1",9000)
    socket.getOutputStream.write(byteString.toArray)
    socket.getOutputStream.flush()
    val buffer = new Array[Byte](40960)
    val read: Int = socket.getInputStream.read(buffer)
    val byteBuffer: ByteBuffer = ByteBuffer.wrap(buffer,0,read)
    println(new String(byteBuffer.array(),"UTF-8"))
    null

  }*/

  def sendCommand(action:ScrAction): Response = {
        sendMessage(action.asByteString)
//    sendMessage(ByteString("GET / HTTP/1.0\n\n"))
//        sendMessage(ByteString("GET /index.html HTTP/1.1"))
  }

  def sendMessage(byteString: ByteString): Response = {
    println(s"sending $byteString")
    val result = Source(Vector(byteString)).via(tcpConnection.get).runFold(ByteString.empty) { (acc, in) ⇒ acc ++ in }
    val result1: ByteString = Await.result(result, Duration("40s"))
    val responseString: String = result1.utf8String
    //We handle all messages synchronously with a 40 second timeout
    //Check and strip the CR character from the response. Perhaps we should keep blocking until it is available
    //    if(responseString.endsWith("\r")) ResponseParser.parse(responseString.substring(0,responseString.length))
    //    else throw new IllegalArgumentException
    println(responseString)
    println(result1)
    ResponseParser.parse(responseString)
  }

  def readFurther: Response = {
    println("Read further")
    val result: Future[ByteString] = Source.empty.via(tcpConnection.get).runFold(ByteString.empty) { (a1: ByteString, a2: ByteString) => a1.++(a2) }
    val result1: ByteString = Await.result(result, Duration("40s"))
    println(result1)
    ResponseParser.parse(result1.utf8String)
  }

/*  @throws(classOf[IOException])
  def doIt(clientSocket: Socket) {
    val is: InputStream = clientSocket.getInputStream
    val pw: PrintWriter = new PrintWriter(clientSocket.getOutputStream)
    val out: ByteArrayOutputStream = new ByteArrayOutputStream(4096)
    val pw2: PrintWriter = new PrintWriter(out)
    pw.println("GET / HTTP/1.0")
    pw.println
    pw.flush
    pw2.println("GET / HTTP/1.0")
    pw2.println
    pw2.flush
    val array: Array[Byte] = ByteBuffer.wrap(out.toByteArray, 0, 16).array
    val string: ByteString = ByteString(array)
    println(s"The BS was $string")
    val buffer: Array[Byte] = new Array[Byte](1024)
    var read: Int = 0
    while ((({
      read = is.read(buffer); read
    })) != -1) {
      val output: String = new String(buffer, 0, read)
//      System.out.print(output)
//      System.out.flush
    }

    clientSocket.close
  }*/

  implicit val system = ActorSystem("scrConnector")

  import system.dispatcher

  implicit val materializer = ActorMaterializer()

  private var tcpConnection: Option[Flow[ByteString, ByteString, Future[OutgoingConnection]]] = None



  def startServer() = server("127.0.0.1", 6000)

  def startClient() = tcpConnection = Some(Tcp().outgoingConnection("127.0.0.1", 6000))
//  def startClient() = tcpConnection = Some(Tcp().outgoingConnection("127.0.0.1", 9000))

  def server(address: String, port: Int): Unit = {
    val handler = Sink.foreach[Tcp.IncomingConnection] { conn =>
      println("Client connected from: " + conn.remoteAddress)
      val badFlow = Flow.fromFunction(new Function[ByteString, ByteString] {
        override def apply(v1: ByteString): ByteString = {
          val theAction: ScrAction = ActionParser.parse(v1)
          theAction match {
            case a:AuthAction => AuthResponse(a.txnRef,"00",a.amount,s"Pong => ${a.txnRef}").asByteString
            case c:CompAction => CompResponse(c.cmdSeq,"00","Pong => NFI").asByteString
//            case c:PokeAction => CompResponse(c.cmdSeq,"00","Pong => NFI").asByteString
          }
        }
      })

      conn handleWith badFlow
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

  def client(system: ActorSystem, address: String, port: Int): Unit = tcpConnection = Some(Tcp(system).outgoingConnection(address, port))

}

object ActionParser {
  def parse(input:ByteString):ScrAction = {
    val asString: String = input.utf8String
    println(s"Parsing action input of $asString")
    if(asString.endsWith("\r")) {
      asString.substring(0,asString.length-1).split("~").toList match {
        case "TXN" ::  "AUTH" :: txnRef :: amount :: merRef :: Nil => AuthAction(txnRef,amount,merRef)
        case "TXN" ::  "COMP" :: cmdSeq :: amount :: Nil => CompAction(cmdSeq,amount)
//        case "TXN" ::  "POKE" :: sale :: Nil => CompAction(cmdSeq,amount)
        case _ => {
          new Throwable().printStackTrace();
          throw new IllegalArgumentException(s"Couldn't parse action of $asString")

        }
      }
    } else {
      new Throwable().printStackTrace();
      throw new IllegalArgumentException(s"Not terminated by CR : $asString")
    }
  }
}


sealed trait ScrCom extends Product {
  def prefix:List[String]
  def asList: List[String] = prefix ++ this.productIterator.map {
    case o: Option[String] => o.getOrElse("")
    case x: Any => x.toString
  }.toList

  def asByteString: ByteString = ByteString(asList.mkString("~").toCharArray.map(_.toByte) ++ Array('\r'.toByte))
}


sealed trait ScrAction extends ScrCom

//Unused : after billing id in order: SlotId, OneSwipeFlag,  ( OemDataFormat,OemData not in example)
//Don't forget to CR
case class AuthAction(txnRef: String, amount: String, merchantReference: String, billingId: Option[String] = None) extends ScrAction {
  //  def asList:List[String]=List("TXN","AUTH",txnRef,amount,merchantReference.getOrElse(""),billingId.getOrElse(""),"","","","")
  override def prefix: List[String] = List("TXN","AUTH")

}

//Fake action just for testing swipe card and payment
case class PokeAction(sale:String) extends ScrAction {
  //  def asList:List[String]=List("TXN","AUTH",txnRef,amount,merchantReference.getOrElse(""),billingId.getOrElse(""),"","","","")
  override def prefix: List[String] = List("TXN","POKE")

}

case class CompAction(cmdSeq: String, amount: String, billingId: Option[String] = None, oemDataFormat: Option[String]=None, oemData: Option[String]=None) extends ScrAction {
  override def prefix: List[String] = List("TXN","COMP")
}

sealed trait Response extends ScrCom {
}

sealed trait ApprovableResponse extends Response {
  def approved: Boolean = responseCode == "00"
  def responseCode: String
}

case class CompResponse(cmdSeq: String, responseCode: String, txnRef: String) extends ApprovableResponse {
  override def prefix: List[String] = List("txn","comp")
}

case class AuthResponse(txnRef: String, responseCode: String, amount: String, dpsTxnRef: String) extends ApprovableResponse {
  override def prefix: List[String] = List("txn","auth")
}

case class NonTxResponse(text:String="") extends Response {
  override def prefix: List[String] = Nil
}

object CompResponse {
  def fromString(input: String) = fromList(input.split("~").toList)

  def fromList(input: List[String]) = input match {
    case List(cmdSeq, re, txnRef) => CompResponse(cmdSeq, re, txnRef)
    case _ => throw new IllegalStateException(s"String $input was not in expected format for AuthResponse")
  }
}


object AuthResponse {
  def fromString(input: String) = fromList(input.split("~").toList)

  def fromList(input: List[String]) = input match {
    case List(tx, re, amount, dpsRef) => AuthResponse(tx, re, amount, dpsRef)
    case _ =>       new Throwable().printStackTrace();
      throw new IllegalStateException(s"String $input was not in expected format for AuthResponse")
  }
}

object ResponseParser {
  def parse(asString: String): Response =  {
    println(s"Parsing response $asString")
    if(asString.endsWith("\r")) {
      asString.substring(0,asString.length-1).split("~").toList match {
        case "txn" :: rest => parseTxn(rest)
        case "msg" :: rest => new NonTxResponse(asString)
        case "dsp" :: rest => new NonTxResponse(asString)
        case "DSP" :: rest => new NonTxResponse(asString)
        case _ =>       new Throwable().printStackTrace();
          throw new IllegalArgumentException(s"Unable to recognize input of $asString")
      }
    } else {
      new Throwable().printStackTrace();
      throw new IllegalArgumentException(s"Response not terminated by CR : ${asString}")
    }

  }

  def parseTxn(input: List[String]): Response = {
    input match {
      case "comp" :: rest => CompResponse.fromList(rest)
      case "auth" :: rest => AuthResponse.fromList(rest)
      case _ =>       new Throwable().printStackTrace();
        throw new IllegalArgumentException(s"Unable to recognize txn input of $input")
    }
  }
}

object runner extends App {


  /*private val parse: Response = ResponseParser.parse("txn~auth~1~00~1000~0000000f0000008c~")
  parse match {
    case a:ApprovableResponse => println(a.approved,a.responseCode)
    case a:NonTxResponse =>
  }
  println(parse)*/


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