package controllers

import javax.inject._

import akka.util.ByteString
import play.api.http.HttpEntity
import play.api.libs.json.Json
import play.api.libs.ws.{InMemoryBody, WSClient, WSRequest}
import play.api.mvc._
import play.api.{Environment, Logger}

import scala.annotation.tailrec
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.io.Source

case class AgentRequest(command: String, arguments: Map[String, String])

case class AgentResponse(command: String, arguments: Map[String, String])

/**
  * Responsible for handling communication / translation with the local card reader and proxying requests (optionally) to the real IWT server for layout / GWT RPC.
  */
@Singleton
class AgentController @Inject()(implicit ws: WSClient, app: Environment, configuration: play.api.Configuration, scrConnector: ScrConnector) extends Controller {
  implicit val reqFormat = Json.format[AgentRequest]
  implicit val prepF = Json.format[PrepareSale]
  implicit val compF = Json.format[ConfirmSale]
  implicit val responseFormat = Json.format[AgentResponse]

  def fudgeStuff() = Action {
    val asStream = getClass.getResourceAsStream("/examplepost.xml")
    val demBytes = Source.fromInputStream(asStream).map(_.toByte).toArray
    val theThing = ws.url("https://qa.paymentexpress.com/scr.aspx").withHeaders(
      "User-Agent"->"SKScrApp",
      "Content-Type"->"text/plain",
      "Content-Length"-> demBytes.length.toString).
    withBody(InMemoryBody(ByteString(demBytes)))
    val resp = Await.result(theThing.execute(),Duration("10s"))
    println(resp.body)
    Ok(resp.body)
  }

  def handler = Action(parse.json) { implicit request =>
    val asRequest: AgentRequest = request.body.validate[AgentRequest].get
    println(asRequest)


    asRequest.command match {
      case "reset" => //TODO
      case "prep" => prepareTransaction
//      case "fakeSwipe" => readingDone = true; completeDone = false; TODO
//      case "fakePay" => paymentDone = true; readingDone = false; TODO
//      case "fakeComplete" => completeDone = true; paymentDone = false
    }

    /* asRequest.command match {
      case "reset" => readingDone = false; paymentDone = false; completeDone = false;
      case "fakeSwipe" => readingDone = true; completeDone = false;
      case "fakePay" => paymentDone = true; readingDone = false;
      case "fakeComplete" => completeDone = true; paymentDone = false
    }
    println(s" reading $readingDone payment $paymentDone complete $completeDone")
    Ok(Json.toJson(AgentResponse(asRequest.command, asRequest.arguments)))*/
    Ok("")
  }

  def proxy(path: String) = Action.async { request =>
    Logger.info(s"Proxying to $path")
    //We need HTTP rather than HTTPS here as we making the remote call via Java which will dislike the certificate because it is self signed. Against a real HTTPS certificate (from IWT) this won't happen
    val target: String = configuration.getString("proxy.target").fold(throw new RuntimeException("No proxy configured"))(x => x + s"/$path".replaceAll("//", "/"))
    val emptyBody: WSRequest = ws.url(target)
      .withMethod(request.method)
      .withHeaders(request.headers.toSimpleMap.toList: _*)
    val withBodyOption = request.body.asRaw.flatMap(_.asBytes()).fold(emptyBody)(x => emptyBody.withBody(InMemoryBody(x)))

    withBodyOption.execute().map { response =>
      Result(ResponseHeader(response.status, response.allHeaders.mapValues(_.head)), HttpEntity.Strict(response.bodyAsBytes, response.header("Content-Type")))
    }
  }

  var currentState = CardReaderState(None)

  def prepareTransaction = Action(parse.json) { implicit request =>
    val asRequest = request.body.validate[PrepareSale].get
    //This will block in the call ... maybe?
    val command: Response = scrConnector.sendCommand(AuthAction(asRequest.saleKey, asRequest.amountInCents.toString, "merchantHere???"))
    handleResponse(command)
  }

  def completeTransaction = Action(parse.json) { implicit request =>
    val asRequest = request.body.validate[ConfirmSale].get
    //This will block in the call ... maybe?
    val command: Response = scrConnector.sendCommand(CompAction(asRequest.saleKey, asRequest.amountInCents.toString))
    handleResponse(command)
  }

  @tailrec
  final def handleResponse(command: Response): Result = {
    command match {
      case app: ApprovableResponse => if (app.approved) Ok("") else InternalServerError("")
      case display: NonTxResponse => {
        println(s"received a non tx response of $display")
        handleResponse(scrConnector.readFurther)
      }
    }
  }

  scrConnector.startServer()
  scrConnector.startClient()

  //  def swiped() = Action { Ok(Json.toJson(readingDone)) }
  //
  //  def paymentProcessed() = Action{Ok(Json.toJson(paymentDone))}
  //
  //  def transactionCompleted() = Action { Ok(Json.toJson(completeDone)) }
  //
  //  def noContent() = Action { NoContent }
}

case class PrepareSale(saleKey: String, amountInCents: String)
case class ConfirmSale(saleKey: String, amountInCents: String)

case class CardReaderState(saleKey: Option[String], approved: Boolean = false, complete: Boolean = false)
