package controllers

import javax.inject._

import play.api.Play.current
import play.api.http.HttpEntity
import play.api.libs.json.Json
import play.api.libs.ws.WS
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class AgentRequest(command:String, arguments:Map[String,String])
case class AgentResponse(command:String, arguments:Map[String,String])
import FakeHomeController._

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  * Second one uses -Xms512M -Xmx1024M -Xss1M -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=256M -Dhttps.port=9444 -Dhttps.keyStore={home}/conf/remote.keystore -Dhttp.port=9001
  */
@Singleton
class HomeController @Inject() extends Controller {

  /**
    * Create an Action to render an HTML page with a welcome message.
    * The configuration in the `routes` file means that this method
    * will be called when the application receives a `GET` request with
    * a path of `/`.
    */
  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

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
    if(path.contains("favicon.ico")) {
      Future.successful(NoContent)
    }
    else if (request.host.contains("localhost") & System.getProperty("http.port","") != "9001") {
      println(s"Proxying to $path")
      //      val s: String = "https://remoteserver:9444/"
      //We need HTTP rather than HTTPS here as we making the remote call via Java which will dislike the certificate because it is self signed. Against a real HTTPS certificate (from IWT) this won't happen
      val s: String = "http://remoteserver:9001"
      val z: Future[Result] = WS.url(s + path)
        .withMethod(request.method)
        .withHeaders(request.headers.toSimpleMap.toList: _*)
        //      .withBody[String](strBody)
        .execute()
        .map { response => {
          Result(ResponseHeader(response.status, response.allHeaders.mapValues(_.head)), HttpEntity.Strict(response.bodyAsBytes, response.header("Content-Type")))
        }
        }
      z
    } else {
      Future.successful(Ok("Not proxying as it would loop infinitely"))
    }
  }

  def someProxyExample() = Action.async { request =>
    if(request.host.contains("localhost") & System.getProperty("http.port","") != "9001") {
      proxy(request.path).apply(request)
    } else {
      println("Remote response")
      Future.successful(Ok("<html><head>Some example</head><body></body>"))
    }
  }

  def swiped() = Action { Ok(Json.toJson(readingDone)) }

  def paymentProcessed() = Action{Ok(Json.toJson(paymentDone))}

  def transactionCompleted() = Action { Ok(Json.toJson(completeDone)) }

}

object FakeHomeController {
  var readingDone = false
  var paymentDone = false
  var completeDone = false
}
