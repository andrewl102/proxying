package controllers

import javax.inject._

import play.api.http.HttpEntity
import play.api.libs.json.Json
import play.api.libs.ws.{InMemoryBody, WSClient, WSRequest}
import play.api.mvc._
import play.api.{Environment, Logger}

import scala.concurrent.ExecutionContext.Implicits.global

case class AgentRequest(command:String, arguments:Map[String,String])
case class AgentResponse(command:String, arguments:Map[String,String])
import controllers.FakeHomeController._

/**
  * Responsible for handling communication / translation with the local card reader and proxying requests (optionally) to the real IWT server for layout / GWT RPC.
  */
@Singleton
class AgentController @Inject()(implicit ws: WSClient, app: Environment, configuration: play.api.Configuration) extends Controller {
  implicit val reqFormat = Json.format[AgentRequest]
  implicit val responseFormat = Json.format[AgentResponse]

  def handler = Action(parse.json) { implicit request =>
    val asRequest: AgentRequest = request.body.validate[AgentRequest].get
    println(asRequest)
    asRequest.command match {
      case "reset" => readingDone = false; paymentDone = false; completeDone = false;
      case "fakeSwipe" => readingDone = true; completeDone = false;
      case "fakePay" => paymentDone = true; readingDone = false;
      case "fakeComplete" => completeDone = true; paymentDone = false
    }
    println(s" reading $readingDone payment $paymentDone complete $completeDone")
    Ok(Json.toJson(AgentResponse(asRequest.command, asRequest.arguments)))
  }

  def proxy(path: String) = Action.async { request =>
      Logger.info(s"Proxying to $path")
      //We need HTTP rather than HTTPS here as we making the remote call via Java which will dislike the certificate because it is self signed. Against a real HTTPS certificate (from IWT) this won't happen
      val target: String = configuration.getString("proxy.target").fold(throw new RuntimeException("No proxy configured"))(x => x + s"/$path".replaceAll("//","/"))
      val emptyBody: WSRequest = ws.url(target)
        .withMethod(request.method)
        .withHeaders(request.headers.toSimpleMap.toList: _*)
      val withBodyOption = request.body.asRaw.flatMap(_.asBytes()).fold(emptyBody)(x => emptyBody.withBody(InMemoryBody(x)))

      withBodyOption.execute().map { response =>
        Result(ResponseHeader(response.status, response.allHeaders.mapValues(_.head)), HttpEntity.Strict(response.bodyAsBytes, response.header("Content-Type")))
      }
  }

  def swiped() = Action { Ok(Json.toJson(readingDone)) }

  def paymentProcessed() = Action{Ok(Json.toJson(paymentDone))}

  def transactionCompleted() = Action { Ok(Json.toJson(completeDone)) }

  def noContent() = Action { NoContent }
}

object FakeHomeController {
  var readingDone = false
  var paymentDone = false
  var completeDone = false
}
