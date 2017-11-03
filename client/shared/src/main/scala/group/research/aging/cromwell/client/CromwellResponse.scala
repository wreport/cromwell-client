package group.research.aging.cromwell.client

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZonedDateTime}

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

@JsonCodec case class Outputs(outputs: Map[String,  CallOutput], id: String) extends WorkflowResponse


trait WithDateTime {
  import cats.syntax.either._
  // import cats.syntax.either._
  // import java.time.Instant
  implicit val encodeInstant: Encoder[ZonedDateTime] = Encoder.encodeString.contramap[ZonedDateTime](_.toString)
  implicit val decodeInstant: Decoder[ZonedDateTime] = Decoder.decodeString.emap { str =>
    Either.catchNonFatal(ZonedDateTime.parse(str)).leftMap(t => "ZonedDateTime")
  }
}

object QueryResult extends WithDateTime

@JsonCodec case class QueryResult(id: String, status: String, start: ZonedDateTime, end: ZonedDateTime) extends WorkflowResponse
{
  lazy val duration: FiniteDuration = FiniteDuration(java.time.Duration.between(start.toInstant, end.toInstant).toMillis, MILLISECONDS)
}

@JsonCodec case class Status(id: String, status: String) extends WorkflowResponse

@JsonCodec case class QueryResults(results: List[QueryResult]) extends CromwellResponse

@JsonCodec case class Logs(calls: Map[String, List[LogCall]], id: String) extends WorkflowResponse

@JsonCodec case class LogCall(stderr: String, stdout: String, attempt: Int, shardIndex: Int) extends CromwellResponse

@JsonCodec case class Backends(supportedBackends: List[String], defaultBackend: String) extends CromwellResponse

@JsonCodec case class SubmittedFiles(inputs: String, workflow: String, options: String) extends CromwellResponse

@JsonCodec case class WorkflowFailure(message: String, causedBy: List[WorkflowFailure] = Nil) extends CromwellResponse



object Metadata extends WithDateTime

@JsonCodec case class Metadata(
                               workflowName: String,
                               workflowRoot: String,
                               id: String,
                               submission: String,
                               status: String,
                               start: ZonedDateTime,//String,
                               end: ZonedDateTime, //String, //ISO_INSTANT
                               inputs: Inputs,
                               failures: List[WorkflowFailure],
                               submittedFiles: SubmittedFiles,
                              ) extends WorkflowResponse
{

  //protected def parse(text: String): LocalDate = LocalDate.parse(text, DateTimeFormatter.ISO_INSTANT)
}


/**
  *
  *
  *   "workflowName": "wf",
  "submittedFiles": {
    "inputs": "{\"wf.hello.pattern\":\"^[a-z]+$\",\"wf.hello.in\":\"/home/antonkulaga/Documents/test.txt\"}",
    "workflow": "task hello {\n  String pattern\n  File in\n\n  command {\n    echo 'hello ${pattern} world ${in}!'\n  }\n\n  output {\n    Array[String] matches = read_lines(stdout())\n  }\n}\n\nworkflow wf {\n  call hello\n}",
    "options": "{\n\n}"
  },
  "calls": {

  },
  "outputs": {

  },
  "workflowRoot": "/home/antonkulaga/Soft/cromwell-executions/wf/f9ed8341-4a16-4fd2-a5b6-71946d0e325c",
  "id": "f9ed8341-4a16-4fd2-a5b6-71946d0e325c",
  "inputs": {
    "wf.hello.in": "/home/antonkulaga/Documents/test.txt",
    "wf.hello.pattern": "^[a-z]+$"
  },
  "submission": "2017-02-25T03:45:03.205+02:00",
  "status": "Failed",
  "failures": [{
    "message": "/home/antonkulaga/Soft/cromwell-executions/wf/f9ed8341-4a16-4fd2-a5b6-71946d0e325c"
  }],
  "end": "2017-02-25T03:45:04.402+02:00",
  "start": "2017-02-25T03:45:03.374+02:00"
  *
  *
  */