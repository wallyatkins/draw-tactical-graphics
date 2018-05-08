import io.javalin.Javalin;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.List;

import org.kohsuke.github.GitHub;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHTreeBuilder;
import org.kohsuke.github.GHCommitBuilder;

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
        appendDataSet(req.queryParam("name"), req.body());
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
    /* System.getenv("TEMP_FILE_PATH");
    System.out.println(json);
    BufferedWriter bw = null;
    try {
      bw = new BufferedWriter(new FileWriter(System.getenv("TEMP_FILE_PATH"), true));
    	bw.write(json);
    	bw.newLine();
    	bw.flush();
      System.out.println("Wrote to " + System.getenv("TEMP_FILE_PATH") + ": " + json);
    } catch (IOException ex) {
    	ex.printStackTrace();
    } finally {
      if (bw != null) try {
    	   bw.close();
    	} catch (IOException e) {
    	  e.printStackTrace();
      }
    } */
  }

  private static void appendDataSet(String name, String json) {
    System.out.println("Query Param: " + name);
    try {
      GitHub github = GitHub.connectUsingPassword(System.getenv("GITHUB_USERNAME"), System.getenv("GITHUB_PASSWORD"));
      GHRepository ghRepo = github.getRepository(System.getenv("GITHUB_REPOSITORY"));
      GHRef masterRef = ghRepo.getRef("heads/master");
      String masterTreeSha = ghRepo
              .getTreeRecursive("master", 1)
              .getSha();
      GHContent content = null;
      List<GHContent> contents = ghRepo.getDirectoryContent("/raw");
      for (GHContent c:contents) {
        if (c.getName().contains(name)) {
          content = c;
        }
      }
      String text = getStringFromInputStream(content.read());
      text += json + System.lineSeparator();
      String treeSha = ghRepo.createTree()
              .baseTree(masterTreeSha)
              .textEntry(content.getPath(), text, false)
              .create()
              .getSha();
      String commitSha = ghRepo.createCommit()
              .message("adding more drawings - " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
              .tree(treeSha)
              .parent(masterRef.getObject().getSha())
              .create()
              .getSHA1();
      masterRef.updateTo(commitSha);
    } catch (IOException ex) {
      System.out.println("An error occurred.");
      ex.printStackTrace();
    }
  }

  // Returns a string builder from an input stream
  private static String getStringFromInputStream(InputStream is) {

    BufferedReader br = null;
    StringBuilder sb = new StringBuilder();

    String line;
    try {
      br = new BufferedReader(new InputStreamReader(is));
      while ((line = br.readLine()) != null) {
        sb.append(line);
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    return sb.toString();
  }

}
