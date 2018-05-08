import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONObject;

import org.kohsuke.github.GitHub;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHTreeBuilder;
import org.kohsuke.github.GHCommitBuilder;

public class WorkerProcess {

  private static String tempFilePath;
  private static Map<String, String> drawings = new HashMap<String, String>();

  // Start a worker to check for and add new drawings to the dataset
  public static void main(String[] args) {

    if (System.getenv("TEMP_FILE_PATH") != null) {
      tempFilePath = System.getenv("TEMP_FILE_PATH");
    } else {
      tempFilePath = "/tmp/drawings.ndjson";
    }

    while(true) {
    	try {
        Thread.sleep(60000); // sleep for a minute
      } catch(InterruptedException e) {
        // something went wrong
      }
      System.out.println("Worker process woke up");

      // 4. Push updates to the proper NDJSON files on GitHub


      // 1. Check the temp NDJSON file to see if there is content
      if (!fileIsEmpty(tempFilePath)) {
        try {
          // 2. Sort the NDJSON entries into different piles
          List<JSONObject> ndjson = parseNDJSON(new FileInputStream(tempFilePath));
          for (JSONObject json : ndjson) {
            drawings.put(json.getString("type"), json.toString() + System.lineSeparator()); // append a new line character
          }
          // 3. Clear the temp NDJSON file for new entries
          clearFile(tempFilePath);
          // 4. Push updates to the proper NDJSON files on GitHub
          Iterator it = drawings.entrySet().iterator();
          while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            appendDataSet((String) pair.getKey(), (String) pair.getValue());
            it.remove();
          }
          // 5. Clear the piles
          clearDrawings();
        } catch (IOException ex) {
          ex.printStackTrace();
        }
      }
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

  // Checks if a file is empty
  private static boolean fileIsEmpty(String file) {
    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(file));
      if (br.readLine() == null) {
        return true;
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    } finally {
		  if (br != null) {
			  try {
				  br.close();
			  } catch (IOException e) {
			    e.printStackTrace();
        }
      }
    }
    return false;
  }

  // Truncates the given file's contents
  private static void clearFile(String file) {
    try {
      PrintWriter pw = new PrintWriter(file);
      pw.write("");
      pw.close();
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  // Clear out the drawings HashMap
  private static void clearDrawings() {
    drawings.clear();
  }

  // Append drawings to the NDJSON files on GitHub
  private static void appendDataSet(String name, String ndjson) {
    String fileName = "raw/" + name + ".ndjson";

    try {
      // Connect to the GitHub repository master branch
      GitHub github = GitHub.connectUsingPassword(System.getenv("GITHUB_USERNAME"), System.getenv("GITHUB_PASSWORD"));
      GHRepository ghRepo = github.getRepository(System.getenv("GITHUB_REPOSITORY"));
      GHRef masterRef = ghRepo.getRef("heads/master");

      // Retrieve the SHA of the latest commit on the master branch
      String masterTreeSha = ghRepo
              .getTreeRecursive("master", 1)
              .getSha();
      // System.out.println("Master branch SHA: " + masterTreeSha);

      GHContent content = null;
      List<GHContent> contents = ghRepo.getDirectoryContent("/raw");
      for (GHContent c:contents) {
        String contentName = c.getName();
        System.out.println("name === " + c.getName() + "; path === " + c.getPath());
        if (c.getName().contains(name)) {
          content = c;
        }
      }

      // Get the drawing file content, append and commit
      //System.out.println("Attempting to get file content: " + content.getPath());
      //GHContent content = ghRepo.getFileContent(fileName);
      //System.out.println("Attempting to get read content: " + content.getName());
      String text = getStringFromInputStream(content.read());
      //System.out.println("File content before: " + text);
      text += ndjson;
      //System.out.println("File content after: " + text);

      String treeSha = ghRepo.createTree()
              .baseTree(masterTreeSha)
              .textEntry(content.getPath(), text, false)
              .create()
              .getSha();
      //System.out.println("Updated tree SHA: " + treeSha);

      // Make a git commit
      String commitSha = ghRepo.createCommit()
              .message("adding more drawings - " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
              .tree(treeSha)
              .parent(masterRef.getObject().getSha())
              .create()
              .getSHA1();
      //System.out.println("Commit SHA: " + commitSha);

      // Update the master reference to refer to the new commit
      masterRef.updateTo(commitSha);
    } catch (IOException ex) {
      System.out.println("An error occurred.");
      ex.printStackTrace();
    }
  }

  // Parse the contents of an NDJSON file
  private static List<JSONObject> parseNDJSON(InputStream is) {
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		return br
				.lines()
				.filter(str -> !str.isEmpty())
				.map(JSONObject::new)
				.collect(Collectors.toList());
	}

}
