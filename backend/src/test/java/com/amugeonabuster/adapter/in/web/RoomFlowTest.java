package com.amugeonabuster.adapter.in.web;

import com.amugeonabuster.application.port.out.RecommendRestaurantsPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "app.alerts.enabled=false",
        "spring.jpa.show-sql=false",
        "spring.datasource.url=jdbc:h2:mem:roomflow;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@ActiveProfiles("local")
class RoomFlowTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockBean RecommendRestaurantsPort restaurants;

    @Test
    void twoBrowsersCompleteVotingAndPersistAllVotes() throws Exception {
        when(restaurants.recommend(anyString(), anyString())).thenReturn(List.of());
        var host = new MockHttpSession();
        var guest = new MockHttpSession();
        JsonNode created = postJson("/api/rooms", host,
                "{\"hostNickname\":\"호스트\",\"location\":\"강남역\",\"customMenus\":[\"치킨\",\"초밥, 우동\"]}", 201);
        String room = created.path("roomId").asText();
        String hostId = created.path("hostId").asText();
        JsonNode joined = postJson("/api/rooms/" + room + "/members", guest, "{\"guestNickname\":\"참여자\"}", 200);
        String guestId = joined.path("members").get(1).path("id").asText();
        postJson("/api/rooms/" + room + "/start", host, "{\"hostId\":\"" + hostId + "\"}", 200);
        for (String menu : List.of("치킨", "초밥, 우동")) {
            for (int i = 0; i < 2; i++) {
                postJson("/api/rooms/" + room + "/swipes", i == 0 ? host : guest,
                        json.writeValueAsString(java.util.Map.of("memberId", i == 0 ? hostId : guestId,
                                "menuName", menu, "isLike", menu.equals("치킨"))), 200);
            }
        }
        var result = mvc.perform(get("/api/rooms/" + room)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.winningMenu").value("치킨"))
                .andExpect(jsonPath("$.completedMembersCount").value(2)).andReturn();
        assertThat(json.readTree(result.getResponse().getContentAsString()).path("defaultMenus").size()).isEqualTo(2);
    }

    private JsonNode postJson(String path, MockHttpSession session, String body, int statusCode) throws Exception {
        var result = mvc.perform(post(path).session(session).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is(statusCode)).andReturn();
        return json.readTree(result.getResponse().getContentAsString());
    }
}
