package core.http;
import domain.request.ApiRequest;
import domain.response.ApiResponse;
public interface ApiClient {
    ApiResponse execute(ApiRequest request);
}