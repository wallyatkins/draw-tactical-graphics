import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.json.JSONObject;

public class NdJson {
	
	// from: https://github.com/abhijithtn/ndjson-java/blob/master/src/main/java/org/jmeshtru/ndjson/NDJSON.java
	public List<JSONObject> parse(InputStream inputStream) {
		Objects.requireNonNull(inputStream, "InputStream cannot be null");
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
		return bufferedReader
				.lines()
				.filter(str -> !str.isEmpty())
				.map(JSONObject::new)
				.collect(Collectors.toList());
	}

	public String generate(List<JSONObject> ndJsonList, ByteArrayOutputStream outputStream) throws IOException {
		Objects.requireNonNull(ndJsonList);
		Objects.requireNonNull(outputStream);

		// String ndjson = "";
		BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
		ndJsonList.stream()
				.map(JSONObject::toString)
				.map(str -> str.replace("\n", "").replace("\r", "").trim())
				.forEachOrdered(jsonString -> writeWithNewLine(bufferedWriter, jsonString));
		bufferedWriter.flush();
		//bufferedWriter.write(ndjson);
		return outputStream.toString();
	}

	private void writeWithNewLine(BufferedWriter writer, String jsonString) {
		try {
			writer.write(jsonString);
			writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
