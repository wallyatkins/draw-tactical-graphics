
import io.javalin.Javalin;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class Main {
  private static int PORT = 8080;

  // Initialize and start the web server
  public static void main(String[] args) {
    initEnvironment();
    Javalin app = Javalin.create()
      .port(PORT)
      .enableStaticFiles("/public")
      .get("/ping", (req, res) -> res.body("pong"))
      .post("/drawing", (req, res) -> {
        appendToFile(req.body());
        res.status(200);
      });
  }

  // Initialize environment variables
  private static void initEnvironment() {
    ProcessBuilder processBuilder = new ProcessBuilder();
    if (processBuilder.environment().get("PORT") != null) {
      PORT = Integer.parseInt(processBuilder.environment().get("PORT"));
    }
  }

  // Append the drawing JSON to the temp NDJSON file
  private static void appendToFile(String json) {
    BufferedWriter bw = null;
    try {
      bw = new BufferedWriter(new FileWriter(System.getenv("TEMP_FILE_PATH"), true));
    	bw.write(json);
    	bw.newLine();
    	bw.flush();
    } catch (IOException ex) {
    	ex.printStackTrace();
    } finally {
      if (bw != null) try {
    	   bw.close();
    	} catch (IOException e) {
    	  // just ignore it
      }
    }
  }

}
