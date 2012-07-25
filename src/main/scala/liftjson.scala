package herald

import net.liftweb.json._
import JsonDSL._

package object as {
  val JValue = dispatch.as.String.andThen(parse)
}
