package controllers

import com.example.playscalajs.shared.SharedMessages
import play.api.mvc.BaseController

trait RootController extends BaseController {

  def index = Action {
    Ok(views.html.index(SharedMessages.itWorks))
  }

}
