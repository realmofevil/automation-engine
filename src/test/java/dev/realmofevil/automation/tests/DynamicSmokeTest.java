
package dev.realmofevil.automation.tests;

import dev.realmofevil.automation.engine.http.RouteInvoker;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DynamicSmokeTest {

    @TestFactory
    List<DynamicTest> smoke() {
        return List.of(
            DynamicTest.dynamicTest("HTTP 200 check", () -> {
                var res = RouteInvoker.get("https://httpbin.org/get");
                assertEquals(200, res.statusCode());
            })
        );
    }
}
