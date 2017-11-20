import sbt.librarymanagement.ModuleID
import sbt.stringToOrganization

object Dependencies {

  private object version {
    val akka = "2.5.6"
    val slf4j = "1.7.25"
  }

  private val slf4j =
    Seq("slf4j-api", "jul-to-slf4j", "jcl-over-slf4j")
      .map("org.slf4j" % _ % version.slf4j)

  val jettyAlpnAgent = "org.mortbay.jetty.alpn" % "jetty-alpn-agent" % "2.0.6"

  val streamsDependencies = Seq(

  )


  private def playFileWatch: ModuleID = "com.lightbend.play" %% "play-file-watch" % "1.1.2"


  def runSupportDependencies: Seq[ModuleID] = Seq(playFileWatch)

  def sbtDependencies: Seq[ModuleID] = {

    Seq(
      "com.typesafe" % "config" % "1.3.1"
    )
  }

  def runtime: Seq[ModuleID] = Seq(
    "javax.inject" % "javax.inject" % "1",
    "com.typesafe.akka" %% "akka-actor" % version.akka
  ) ++ slf4j

}
