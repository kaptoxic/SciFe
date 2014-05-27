import sbt._
import Keys._

object SciFeBuild extends Build {
  lazy val root =
    Project("SciFe", file("."))
      .configs( BenchConfig )
      .settings( inConfig(BenchConfig)(Defaults.testTasks): _*)
      .settings(
        fork in Test := true,
        javaOptions in Test += "-Xmx2048m",
        unmanagedSourceDirectories in Test <+= sourceDirectory ( _ / "bench" ),

        commands ++= Seq(benchCommand, benchBadgeCommand),
        
        parallelExecution in BenchConfig := false,
        fork in BenchConfig := false,
        testFrameworks in BenchConfig += new TestFramework("org.scalameter.ScalaMeterFramework"),
        includeFilter in BenchConfig := AllPassFilter,
        testOptions in BenchConfig := Seq(Tests.Filter(benchFilter)),
        testOptions in BenchConfig := Seq(),
        scalacOptions in BenchConfig ++= Seq("-deprecation", "-unchecked", "-feature", "-Xdisable-assertions"),
        scalacOptions in BenchConfig ++= Seq("-Xelide-below", "OFF") 
      )

  val benchRegEx = """(.*\.suite\.[^\.]*Suite*)"""
      
  def benchFilter(name: String): Boolean = {
    name matches benchRegEx
  }
  
  lazy val BenchConfig = config("benchmark") extend(Test)
    
  def benchCommand = Command.single("bench") { (state, arg) =>
    val extracted: Extracted = Project.extract(state)
    import extracted._

    arg match {
      case "full" =>
        val fullState =
          append(Seq(testOptions in BenchConfig += Tests.Filter(_ endsWith "Full")), state)
        Project.evaluateTask(test in BenchConfig, fullState)
        state
      case "minimal" =>
        val minState =          
          append(Seq(testOptions in BenchConfig += Tests.Filter(_ endsWith "Minimal")), state)
        Project.evaluateTask(test in BenchConfig, minState)
        state
      case _ =>
        state.fail
    }
  }
  
  import java.util._
  import java.text._
  
  val badgesUrl = "http://img.shields.io/badge/"
  val pattern = "benchmark-%s-green.svg"
  val downloadCommand = "wget -O ./tmp/status.svg %s%s"
  val suffixPattern = "benchmark-%s-green.svg"
    
  def benchBadgeCommand = Command.command("bench-badge") { state =>
    val currentTime = Calendar.getInstance().getTime()
    val dateFormat = new SimpleDateFormat("""dd%2'F'MM%2'F'yy""")
    
    val dateString = dateFormat format currentTime
    val suffix = suffixPattern format dateString
    
    val commandResult =
      Process(downloadCommand.format(badgesUrl, suffix)).lines
    
    println(commandResult)
    
    state
  }
  
}
