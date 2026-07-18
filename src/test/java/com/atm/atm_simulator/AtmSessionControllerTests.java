package com.atm.atm_simulator;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
class AtmSessionControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void loginWithdrawAndReadTransactions() throws Exception {
        String token = login("1234567", "1234");

        mockMvc.perform(get("/api/atm/balance")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(100000));

        mockMvc.perform(post("/api/atm/withdraw")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":20000}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(80000));

        mockMvc.perform(get("/api/atm/transactions")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type").value("WITHDRAW"))
                .andExpect(jsonPath("$[0].amount").value(20000))
                .andExpect(jsonPath("$[0].balanceAfter").value(80000));
    }

    @Test
    void transferTopUpAndChangePinFlowWorks() throws Exception {
        String token = login("7654321", "5678");

        mockMvc.perform(post("/api/atm/transfer")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetAccountNumber":"1234567",
                                  "targetBankName":"Nihon Bank",
                                  "note":"Rent",
                                  "amount":10000
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(490000));

        mockMvc.perform(post("/api/atm/topup")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "target":"Suica",
                                  "amount":5000
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(485000));

        mockMvc.perform(post("/api/atm/change-pin")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPin":"5678",
                                  "newPin":"2468"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("PIN changed successfully"));

        mockMvc.perform(get("/api/atm/transactions")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].type").value("PIN_CHANGE"))
                .andExpect(jsonPath("$[1].type").value("TOP_UP"))
                .andExpect(jsonPath("$[2].type").value("TRANSFER"));

        mockMvc.perform(post("/api/atm/logout")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        login("7654321", "2468");
    }

    @Test
    void wrongPinLoginReturns401() throws Exception {
        mockMvc.perform(post("/api/atm/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountNumber":"1234567","pin":"9999"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void withdrawExceedingBalanceReturns400() throws Exception {
        String token = login("9999999", "0000");

        mockMvc.perform(post("/api/atm/withdraw")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":2000000}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Insufficient funds"));
    }

    @Test
    void invalidSessionReturns401() throws Exception {
        mockMvc.perform(get("/api/atm/balance")
                        .header("Authorization", bearer("00000000-0000-0000-0000-000000000000")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePinWithWrongCurrentPinReturns401() throws Exception {
        String token = login("9999999", "0000");

        mockMvc.perform(post("/api/atm/change-pin")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPin":"9999","newPin":"1111"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void concurrentWithdrawalsDoNotOverdraft() throws Exception {
        int threads = 20;

        String[] tokens = new String[threads];
        for (int i = 0; i < threads; i++) {
            tokens[i] = login("9999999", "0000");
        }

        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            final String token = tokens[i];
            executor.submit(() -> {
                try {
                    ready.countDown();
                    start.await();
                    int status = mockMvc.perform(post("/api/atm/withdraw")
                                    .header("Authorization", bearer(token))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
                                            {"amount":100000}
                                            """))
                            .andReturn().getResponse().getStatus();
                    if (status == 200) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception ignored) {}
            });
        }

        ready.await();
        start.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS));

        assertEquals(10, successCount.get());

        String verifyToken = login("9999999", "0000");
        mockMvc.perform(get("/api/atm/balance")
                        .header("Authorization", bearer(verifyToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(0));
    }

    private String login(String accountNumber, String pin) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/atm/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountNumber":"%s",
                                  "pin":"%s"
                                }
                                """.formatted(accountNumber, pin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
