package herald

import dispatch._
import java.net.URI
import java.io.{File,FileInputStream}
import com.tristanhunt.knockoff.DefaultDiscounter._
import scala.xml.{NodeSeq,Node}

object Herald {
  def posterousCredentialsPath = 
    file(new File(System.getProperty("user.home")), ".posterous")

  lazy val posterousProperties =
    posterousCredentialsPath.right.map { file =>
      val props = new java.util.Properties
      val is = new FileInputStream(file)
      props.load(is)
      is.close()
      props
    }

  def posterousProperty(name: String) =
    posterousProperties.right.flatMap { p =>
      Option(p.getProperty(name)).toRight {
        "Required property %s not found in file %s".format(
          name, posterousCredentialsPath
        )
      }
    }

  def posterousEmail = posterousProperty("email")
  def posterousPassword = posterousProperty("password")

  def file(parent: File, child: String) =
    Some(new File(parent, child)).filter { _.exists }.toRight {
      "Required path %s/%s does not exist".format(parent.toString, child)
    }

  def dir(parent: File, child: String) =
    file(parent, child).right.flatMap { f =>
      if (f.isDirectory) Right(f)
      else Left("Path %s must be a directory".format(f))
    }

  def notesFile(name: String) =
    notesDirectory.right.flatMap { d =>
      file(d, "%s.%s".format(name, notesExtension))
    }

  /** Posterous site id, defaults to implicit.ly */
  def siteId = 6903194 // 1031779
  def site = "herald-test.posterous.com" // "implicit.ly"

  def base = new File(".").getCanonicalFile

  /** Title defaults to name and version */
  def title = version.right.map { v => "%s %s".format(name, v) }
  def name = base.getName

  /** Path to release notes and text about project. */
  def notesDirectory = dir(base, "notes")

  implicit def seqOrdering[A](implicit cmp: Ordering[A]) =
    new Ordering[Seq[A]] {
      @annotation.tailrec
      def compare (lx: Seq[A], ly: Seq[A]) = {
        if (lx.isEmpty && ly.isEmpty) 0
        else if (lx.isEmpty) -1
        else if (ly.isEmpty) 1
        else {
          val cur = cmp.compare(lx.head, ly.head)
          if (cur == 0) compare(lx.tail, ly.tail)
          else cur
        }
      }
    }

  def version: Either[String,String] =
    notesDirectory.right.flatMap { notes =>
      val Note = ("(.*)[.]" + notesExtension).r
      val versions = notes.listFiles.map { _.getName }.collect {
        case Note(version) if version != aboutName => version
      }
      if (versions.isEmpty)
        Left("no version notes found in " + notes)
      else Right(
        versions.maxBy {
          _.split("\\D").filterNot { _.isEmpty }.map { _.toInt }.toSeq
        }
      )
    }

  def versionFile = for {
    notes <- notesDirectory.right
    v <- version.right
    f <- notesFile(v).right
  } yield f


  def aboutName = "about"
  /** Project info named about.markdown. */
  def aboutFile = notesDirectory.right.flatMap { n =>
    notesFile(aboutName)
  }
  def notesExtension = "markdown"

  /** The content to be posted, transformed into xml. Default impl
   *  is the version notes followed by the "about" boilerplate in a
   *  div of class "about" */
  def bodyContent =
    for {
      versionNotes <- versionFile.right
      about <- aboutFile.right
    } yield
      mdToXml(versionNotes) ++
        <div class="about"> { mdToXml(about) } </div>

  /** @return node sequence from str or Nil if str is null or empty. */
  private def mdToXml(str: String) = str match {
    case null | "" => Nil
    case _ => toXML(knockoff(str))
  }   

  /** @return node sequence from file or Nil if file is not found. */
  private def mdToXml(md: File) =
    if (md.exists)
      toXML(knockoff(scala.io.Source.fromFile(md).mkString))
    else
      Nil

  def run(args: Array[String]) = {
    val either = args match {
      case Array("--publish") =>
        val nested = for {
          body <- bodyContent.right
          title <- title.right
          email <- posterousEmail.right
          pass <- posterousPassword.right
        } yield {
          Publish.duplicate(email, pass, site, title)().toLeft {
            Publish(body, email, pass, siteId, title, name)().right.map {
              url =>
                unfiltered.util.Browser.open(url)
                "Published to " + url
            }
          }
        }
        nested.joinRight.joinRight
      case Array("--version") =>
        Right("herald is ready to go")
      case _ =>
        for {
          _ <- bodyContent.right
          _ <- title.right
        } yield {
          Preview(bodyContent, title)
          "Stopped preview."
        }
    }
    Http.shutdown()
    either.fold(
      err => { System.err.println(err); 1 },
      msg => { println(msg); 0 }
    )
  }
  def main(args: Array[String]) {
    System.exit(run(args))
  }
}

class Herald extends xsbti.AppMain {
  def run(config: xsbti.AppConfiguration) = {
    Exit(Herald.run(config.arguments))
  }
  case class Exit(val code: Int) extends xsbti.Exit
}
