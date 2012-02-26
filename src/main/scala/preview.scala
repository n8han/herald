package herald

import unfiltered.request._
import unfiltered.response._

object Preview {
  def apply(body: => Either[String,Seq[xml.Node]],
            title: => Either[String,String]) {
    unfiltered.netty.Http.anylocal.plan(unfiltered.netty.cycle.Planify {
      case GET(Path(Seg(Nil))) =>
        previewContent(body, title).fold(
          err => InternalServerError ~> ResponseString(err),
          html => Html(html)
        )
    }).run { server =>
      unfiltered.util.Browser.open(
        "http://127.0.0.1:%d/".format(server.port)
      )
      println("\nPreviewing notes. Press CTRL+C to stop.")
    }
  }

  def previewContent(bodyContent: Either[String, Seq[xml.Node]],
                     title: Either[String, String]) = 
    for {
      body <- bodyContent.right
      t <- title.right
    } yield
      <html>
      <head>
        <title> { title } </title>
        <style> {"""
          div.about * { font-style: italic }
          div.about em { font-style: normal }
        """} </style>
      </head>
      <body>
        <h2><a href="#">{ title }</a></h2>
        { body }
      </body>
      </html>
}
