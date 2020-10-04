package alpha.medix.sleepy.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Optional;

public class JsonUtils {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public static <T> Optional<T> fromJson(String json, Class<T> clazz) {
    try {
      return Optional.of(OBJECT_MAPPER.readValue(json, clazz));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  public static String toJson(Object object) throws IOException {
    try {
      return OBJECT_MAPPER.writeValueAsString(object);
    } catch (JsonProcessingException e) {
      throw new IOException(e);
    }
  }

}
