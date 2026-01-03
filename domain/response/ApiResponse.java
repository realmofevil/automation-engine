package domain.response;

import java.util.List;
import java.util.Map;

public record ApiResponse(int statusCode, Map<String, List<String>> headers, String body) {}