import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.jersey.logging.LoggingFeature;

import io.javalin.Javalin;

public class Main {
  private static int PORT = 8080;

  public static void main(String[] args) {
    initEnvironment();
    Logger logger = Logger.getLogger(Main.class.getName());
    LoggingFeature feature = new LoggingFeature(logger, Level.INFO, null, null);
    Javalin app = Javalin.create()
      .port(PORT)
      .enableStaticFiles("/public")
      .get("/test", (req, res) -> res.body("tests"));
  }

  private static void initEnvironment() {
    ProcessBuilder processBuilder = new ProcessBuilder();
    if (processBuilder.environment().get("PORT") != null) {
      PORT = Integer.parseInt(processBuilder.environment().get("PORT"));
    }
  }
}
