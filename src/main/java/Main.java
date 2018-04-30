//import java.util.logging.Level;
//import java.util.logging.Logger;

//import org.glassfish.jersey.logging.LoggingFeature;

import java.io.IOException;

import io.javalin.Javalin;

import org.kohsuke.github.GitHub;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHContent;

public class Main {
  private static int PORT = 8080;

  public static void main(String[] args) {
    initEnvironment();
    //Logger logger = Logger.getLogger(Main.class.getName());
    //LoggingFeature feature = new LoggingFeature(logger, Level.INFO, null, null);
    Javalin app = Javalin.create()
      .port(PORT)
      .enableStaticFiles("/public")
      .get("/test", (req, res) -> res.body("tests"))
      .post("/drawing", (req, res) -> res.body("tests"));
  }

  private static void initEnvironment() {
    ProcessBuilder processBuilder = new ProcessBuilder();
    if (processBuilder.environment().get("PORT") != null) {
      PORT = Integer.parseInt(processBuilder.environment().get("PORT"));
    }
  }

  private static String addDrawing() {
    try {
      GitHub github = GitHub.connect("USER", "TOKEN");
      GHRepository ghRepo = github.getRepository("REPOSITORY");
      GHContent content = ghRepo.getFileContent("/PATH/FILE.txt");
    } catch (IOException ex) {
      ex.printStackTrace();
    }
    return "";
  }
}
