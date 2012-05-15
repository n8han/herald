package herald

import com.ning.http.client.Response

import net.liftweb.json._
import JsonDSL._

object LiftJson {
  val As = dispatch.As.string.andThen(JsonParser.parse)
}
