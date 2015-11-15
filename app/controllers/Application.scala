package controllers

import javax.inject.Singleton

import com.typesafe.scalalogging.slf4j.LazyLogging
import play.api.mvc._


@Singleton
class Application extends Controller with LazyLogging {

  def index = Action {
    Ok(views.html.index())
  }
}
