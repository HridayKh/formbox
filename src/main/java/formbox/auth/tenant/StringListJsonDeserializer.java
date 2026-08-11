package formbox.auth.tenant;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StringListJsonDeserializer extends JsonDeserializer<List<String>> {
	@Override
	public List<String> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
		if (p.currentToken() == JsonToken.START_OBJECT) {
			p.skipChildren();
			return new ArrayList<>();
		}
		if (p.currentToken() == JsonToken.VALUE_NULL) {
			return new ArrayList<>();
		}
		if (p.currentToken() == JsonToken.START_ARRAY) {
			List<String> list = new ArrayList<>();
			while (p.nextToken() != JsonToken.END_ARRAY) {
				list.add(p.getText());
			}
			return list;
		}
		return new ArrayList<>();
	}
}
