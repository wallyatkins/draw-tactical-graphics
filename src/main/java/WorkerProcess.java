import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.kohsuke.github.GitHub;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHContent;

public class WorkerProcess {

  public static void main(String[] args) {

    while(true) {
    	try {
        Thread.sleep(60000);
      } catch(InterruptedException e) {
        // something went wrong
      }
      System.out.println("Worker process woke up");

      // 1. Check the temp NDJSON file to see if there is content
      // 2. Sort the NDJSON entries into different piles
      // 3. Clear the temp NDJSON file for new entries
      // 4. Push updates to the proper NDJSON files to GitHub

      try {
        GitHub github = GitHub.connectUsingPassword(System.getenv("GITHUB_USERNAME"), System.getenv("GITHUB_PASSWORD"));
        GHRepository ghRepo = github.getRepository("wallyatkins/draw-tactical-graphics");
        GHContent content = ghRepo.getFileContent("Procfile");
        System.out.println(getStringFromInputStream(content.read()));
      } catch (IOException ex) {
        ex.printStackTrace();
      }
    }
  }

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
