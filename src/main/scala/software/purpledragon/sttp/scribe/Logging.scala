package software.purpledragon.sttp.scribe

import org.slf4j.{Logger, LoggerFactory}

private[scribe] trait Logging {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass.getName)
}
