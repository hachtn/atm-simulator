package com.atm.atm_simulator;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
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
