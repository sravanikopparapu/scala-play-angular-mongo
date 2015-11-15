package controllers

import java.io.File
import javax.inject.Singleton

import com.typesafe.scalalogging.slf4j.LazyLogging
import play.api.Play
import play.api.Play.current
import play.api.mvc._


@Singleton
class Application extends Controller with LazyLogging {

  def index = Action {
    logger.info("Serving index page...")

    val javascripts = {
      if (Play.isDev) {
        // Load all .js and .coffeescript files within app/assets
        Option(Play.getFile("app/assets")).
          filter(_.exists).
          map(findScripts).
          getOrElse(Nil)
      } else {
        // Concated and minified by UglifyJS
        "concat.min.js" :: Nil
      }
    }

    Ok(views.html.index(javascripts))
  }

  private def findScripts(base: File): Seq[String] = {
    val baseUri = base.toURI
    directoryFlatMap(base, scriptMapper).
      map(f => baseUri.relativize(f.toURI).getPath)
  }

  private def scriptMapper(file: File): Option[File] = {
    val name = file.getName
    if (name.endsWith(".js")) Some(file)
    else if (name.endsWith(".coffee")) Some(new File(file.getParent, name.dropRight(6) + "js"))
    else None
  }

  private def directoryFlatMap[A](in: File, fileFun: File => Option[A]): Seq[A] = {
    in.listFiles.flatMap {
      case f if f.isDirectory => directoryFlatMap(f, fileFun)
      case f if f.isFile => fileFun(f)
    }
  }

}
