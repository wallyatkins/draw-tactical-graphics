public class Main {
  private static int PORT = 8080;

  public static void main(String[] args) {
    initEnvironment();
    Logger logger = Logger.getLogger(Main.class.getName());
    LoggingFeature feature = new LoggingFeature(logger, Level.INFO, null, null);
    Javalin app = Javalin.create().port(PORT).enableStaticFiles("/public");
  }
  
  private static void initEnvironment() {
    ProcessBuilder processBuilder = new ProcessBuilder();
    if (processBuilder.environment().get("PORT") != null) {
      PORT = Integer.parseInt(processBuilder.environment().get("PORT"));
    }
  }
}
