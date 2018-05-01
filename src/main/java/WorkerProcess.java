import java.io.IOException;

import org.kohsuke.github.GitHub;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHContent;

public class WorkerProcess {

  public static void main(String[] args) {

    while(true) {
    	try {
        Thread.sleep(5000);
      } catch(InterruptedException e) {
        // something went wrong
      }
      System.out.println("Worker process woke up");
      try {
        GitHub github = GitHub.connect("USER", "TOKEN");
        GHRepository ghRepo = github.getRepository("REPOSITORY");
        GHContent content = ghRepo.getFileContent("/PATH/FILE.txt");
      } catch (IOException ex) {
        ex.printStackTrace();
      }
    }
  }

}
