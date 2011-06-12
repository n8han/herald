/*
  lazy val changelog = changelogAction
  def changelogPath = outputPath / "CHANGELOG.md"
  def changelogAction = task { generateChangelog(changelogPath) } describedAs ("Produce combined release notes" )

  def generateChangelog(output: Path) = {
    def cmpName(f: Path) = f.base.replace("""\.markdown$""", "").replaceAll("""\.""", "")
    val outputFile = output.asFile
    val inOrder = (notesFiles --- aboutNotesPath).getFiles.toList.map(Path.fromFile(_)).
                  sort((p1, p2) => cmpName(p1).compareTo(cmpName(p2)) < 0).reverse.foreach { p =>
      val fileVersion = p.base.replace("""\.markdown$""", "")
      FileUtilities.readString(p.asFile, log) match {
        case Right(text) => FileUtilities.append(outputFile, "\nVersion " + fileVersion + ":\n\n" + text, log)
        case Left(error) => throw new RuntimeException(error)
      }
    }
    log.info("Generated " + output)
    None
  }

*/
