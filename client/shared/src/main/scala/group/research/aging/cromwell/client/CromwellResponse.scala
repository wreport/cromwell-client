package group.research.aging.cromwell.client

//import java.time.ZonedDateTime

import io.circe._
import io.circe.generic.JsonCodec

import scala.concurrent.duration._


trait CromwellResponse

@JsonCodec case class Stats(workflows: Int, jobs: Int) extends CromwellResponse

@JsonCodec case class Version(cromwell: String) extends CromwellResponse

trait WorkflowResponse extends CromwellResponse
{
  def id: String
}

case class CallOutput(value: Json) extends CromwellResponse

object CallOutput {

  implicit val encode: Encoder[CallOutput] = new Encoder[CallOutput] {
    final def apply(a: CallOutput): Json = a.value
  }

  implicit val decode: Decoder[CallOutput] = new Decoder[CallOutput] {
    final def apply(c: HCursor): Decoder.Result[CallOutput] = c.focus match{
      case None => Left(DecodingFailure("Cannot extract call output!", c.history))
      case Some(json) => Right(CallOutput(json))
    }
  }

}

case class Inputs(values: Map[String, String]) extends CromwellResponse

object Inputs {
  import io.circe.syntax._
  implicit val encode: Encoder[Inputs] = new Encoder[Inputs] {
    final def apply(a: Inputs): Json = a.values.asJson
  }

  implicit val decode: Decoder[Inputs] = new Decoder[Inputs] {
    final def apply(c: HCursor): Decoder.Result[Inputs] = c.focus match{
      case None => Left(DecodingFailure("Cannot extract call output!", c.history))
      case Some(json) => Right(Inputs(json.asObject.map(o=>o.toMap.mapValues(v=>v.toString())).get))
    }
  }

}


object QueryResults {
  lazy val empty = QueryResults(Nil)
}

@JsonCodec case class QueryResults(results: List[QueryResult]) extends CromwellResponse

@JsonCodec case class QueryResult(id: String, status: String, start: Option[String] = None, end: Option[String] = None) extends WorkflowResponse


//implicit val config: Configuration = Configuration.default.withSnakeCaseKeys
// config: io.circe.generic.extras.Configuration = Configuration(io.circe.generic.extras.Configuration$$$Lambda$2037/501381773@195cef0e,false,None)

object Metadata

@JsonCodec case class Metadata(
                                id: String,
                                submission: String,
                                status: String,
                                start: Option[String],
                                end: Option[String],
                                inputs: Inputs,
                                failures: Option[List[WorkflowFailure]] = None,
                                submittedFiles: SubmittedFiles,
                                workflowName: Option[String] = None,
                                workflowRoot: Option[String] = None,
                                calls: Option[Map[String, List[LogCall]]] = None
                              ) extends WorkflowResponse
{

  lazy val startDate: String = start.map(s=>s.substring(0, Math.max(0, s.indexOf("T")))).getOrElse("")
  lazy val endDate: String = end.fold("")(e=>e.substring(0, Math.max(0, e.indexOf("T"))))

  lazy val startTime: String = start.map(s=>s.substring(s.indexOf("T")+1, s.lastIndexOf("."))).getOrElse("")
  lazy val endTime: String = end.fold("")(e=> e.substring(e.indexOf("T")+1, e.lastIndexOf(".")))

  lazy val dates: String = if(endDate==startDate || endDate=="") startDate else s"${startDate}-${endDate}"

  //protected def parse(text: String): LocalDate = LocalDate.parse(text, DateTimeFormatter.ISO_INSTANT)
}

@JsonCodec case class Outputs(outputs: Map[String,  CallOutput], id: String) extends WorkflowResponse

@JsonCodec case class StatusInfo(id: String, status: String) extends WorkflowResponse

@JsonCodec case class Logs(calls: Option[Map[String, List[LogCall]]], id: String) extends WorkflowResponse

@JsonCodec case class LogCall(stderr: String, stdout: String, attempt: Int, shardIndex: Int) extends CromwellResponse

@JsonCodec case class Backends(supportedBackends: List[String], defaultBackend: String) extends CromwellResponse

@JsonCodec case class SubmittedFiles(inputs: String, workflow: String, options: String) extends CromwellResponse

@JsonCodec case class WorkflowFailure(message: String, causedBy: List[WorkflowFailure] = Nil) extends CromwellResponse