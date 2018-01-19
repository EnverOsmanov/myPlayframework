import sbt.librarymanagement.{CrossVersion, ModuleID}
import sbt.{Defaults, stringToOrganization}

object Dependencies {

  private object version {
    val akka = "2.5.6"
    val slf4j = "1.7.25"
  }

  private def slf4j(name: String) = "org.slf4j" % name % version.slf4j

  private val slf4jModuleIds = Seq(slf4j("slf4j-api"), slf4j("jul-to-slf4j"), slf4j("jcl-over-slf4j"))

  val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"

  val jettyAlpnAgent = "org.mortbay.jetty.alpn" % "jetty-alpn-agent" % "2.0.6"

  val streamsDependencies = Seq(

  )


  private def playFileWatch: ModuleID = "com.lightbend.play" %% "play-file-watch" % "1.1.6"


  def runSupportDependencies: Seq[ModuleID] = Seq(playFileWatch)

  def sbtDependencies(sbtVersion: String, scalaVersion: String): Seq[ModuleID] = {
    def sbtDep(moduleId: ModuleID) = sbtPluginDep(moduleId, sbtVersion, scalaVersion)

    Seq(
      "com.typesafe" % "config" % "1.3.1",
      sbtDep("com.typesafe.sbt" % "sbt-native-packager" % "1.3.2")
    )
  }

  def runtime: Seq[ModuleID] = Seq(
    "javax.inject" % "javax.inject" % "1",
    "com.typesafe.akka" %% "akka-actor" % version.akka
  ) ++ slf4jModuleIds

  private def sbtPluginDep(moduleID: ModuleID, sbtVersion: String, scalaVersion: String) = {
    Defaults.sbtPluginExtra(moduleID, CrossVersion.binarySbtVersion(sbtVersion), CrossVersion.binaryScalaVersion(scalaVersion))
  }

}
