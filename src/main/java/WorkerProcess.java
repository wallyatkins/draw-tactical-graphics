import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONObject;

import org.kohsuke.github.GitHub;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHContent;

public class WorkerProcess {

  private static String tempFilePath;
  private static HashMap<String, String> drawings = new HashMap<String, String>();

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

      // 1. Check the temp NDJSON file to see if there is content
      // 2. Sort the NDJSON entries into different piles
      // 3. Clear the temp NDJSON file for new entries
      // 4. Push updates to the proper NDJSON files on GitHub
      // 5. Clear the piles

      if (!fileIsEmpty(tempFilePath)) {
        try {
          List<JSONObject> ndjson = parseNDJSON(new FileInputStream(tempFilePath));
        } catch (IOException ex) {
          ex.printStackTrace();
        }
      }

      Iterator it = drawings.entrySet().iterator();
      while (it.hasNext()) {
        Map.Entry pair = (Map.Entry)it.next();
        System.out.println(pair.getKey() + " = " + pair.getValue());
        it.remove();
      }

    }
  }

  // Returns a string builder from an input stream
  private static StringBuilder getStringFromInputStream(InputStream is, String data) {

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

    sb.append(data);
		return sb;
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
      new PrintWriter(file).close();
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

    try {
      GitHub github = GitHub.connectUsingPassword(System.getenv("GITHUB_USERNAME"), System.getenv("GITHUB_PASSWORD"));
      GHRepository ghRepo = github.getRepository(System.getenv("GITHUB_REPOSITORY"));
      GHContent content = ghRepo.getFileContent("raw/" + name + ".ndjson");
      //System.out.println(getStringFromInputStream(content.read()));
    } catch (IOException ex) {
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
