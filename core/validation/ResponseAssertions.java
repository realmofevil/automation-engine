package core.validation;

import domain.response.ApiResponse;
import org.junit.jupiter.api.Assertions;

public final class ResponseAssertions {

    private final ApiResponse response;

    private ResponseAssertions(ApiResponse response) {
        this.response = response;
    }

    public static ResponseAssertions assertThat(ApiResponse response) {
        return new ResponseAssertions(response);
    }

    public ResponseAssertions hasStatus(int expected) {
        Assertions.assertEquals(expected, response.statusCode());
        return this;
    }

    public ResponseAssertions bodyContains(String value) {
        Assertions.assertTrue(response.body().contains(value));
        return this;
    }
}