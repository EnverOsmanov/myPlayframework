object FileWatchServiceInitializer {

  lazy val initialFileWatchService = play.dev.filewatch.FileWatchService.polling(500)

}