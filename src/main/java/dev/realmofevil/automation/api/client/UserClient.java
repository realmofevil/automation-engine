//TODO: Legacy class, to be removed
package dev.realmofevil.automation.api.client;

import dev.realmofevil.automation.engine.http.ApiClient;

public class UserClient {

    private final ApiClient api;
    private final String baseUrl;

    public UserClient(ApiClient api, String baseUrl) {
        this.api = api;
        this.baseUrl = baseUrl;
    }

    public int getUserStatus(String id) throws Exception {
        return api.get(baseUrl + "/users/" + id).statusCode();
    }

    public int createUser(String payload) throws Exception {
        return api.post(baseUrl + "/users", payload).statusCode();
    }
}
