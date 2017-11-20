sys.props.get("plugin.version") match {
  case Some(x) =>
    addSbtPlugin("com.typesafe.yalp" % "sbt-plugin" % x)

  case _ => sys.error("""|The system property 'plugin.version' is not defined.
                         |Specify this property using the scriptedLaunchOpts -D.""".stripMargin + sys.props.keys.filter(_.contains("version")))
}


unmanagedSourceDirectories in Compile += baseDirectory.value.getParentFile / "project" / s"scala-sbt-${sbtBinaryVersion.value}"