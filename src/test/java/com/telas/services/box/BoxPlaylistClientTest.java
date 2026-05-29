package com.telas.services.box;

import com.telas.dtos.request.BoxPlaylistPushRequestDto;
import com.telas.dtos.request.UpdateBoxMonitorsAdRequestDto;
import com.telas.dtos.response.BoxPlayerSettingsResponseDto;
import com.telas.services.BoxCarouselSettingsService;
import com.telas.shared.utils.HttpClientUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoxPlaylistClientTest {

    @Mock
    private HttpClientUtil httpClient;

    @Mock
    private BoxCarouselSettingsService boxCarouselSettingsService;

    @InjectMocks
    private BoxPlaylistClient boxPlaylistClient;

    private static final BoxPlayerSettingsResponseDto PLAYER_SETTINGS =
            new BoxPlayerSettingsResponseDto(5000, 2000, 15);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(boxPlaylistClient, "apiKey", "test-api-key");
        lenient().when(boxCarouselSettingsService.getSettings()).thenReturn(PLAYER_SETTINGS);
    }

    @Test
    void resolveUpdateAdsUrl_appendsPathWhenBaseUrlHasTrailingSlash() {
        assertEquals("http://10.0.0.1:8081/update-ads", boxPlaylistClient.resolveUpdateAdsUrl("http://10.0.0.1:8081/"));
    }

    @Test
    void resolveUpdateAdsUrl_appendsPathWhenBaseUrlHasNoTrailingSlash() {
        assertEquals("http://10.0.0.1:8081/update-ads", boxPlaylistClient.resolveUpdateAdsUrl("http://10.0.0.1:8081"));
    }

    @Test
    void pushEmptyPlaylist_returnsTrueOnSuccess() {
        assertTrue(boxPlaylistClient.pushEmptyPlaylist("http://10.0.0.1:8081/", UUID.randomUUID()));

        ArgumentCaptor<BoxPlaylistPushRequestDto> bodyCaptor = ArgumentCaptor.forClass(BoxPlaylistPushRequestDto.class);
        verify(httpClient).makePostRequest(
                eq("http://10.0.0.1:8081/update-ads"),
                bodyCaptor.capture(),
                eq(Void.class),
                eq(null),
                any());
        assertTrue(bodyCaptor.getValue().getAds().isEmpty());
        assertNotNull(bodyCaptor.getValue().getPlayerSettings());
    }

    @Test
    void pushEmptyPlaylist_returnsFalseOnFailure() {
        doThrow(new RuntimeException("network")).when(httpClient).makePostRequest(any(), any(), any(), any(), any());
        assertFalse(boxPlaylistClient.pushEmptyPlaylist("http://10.0.0.1:8081/", UUID.randomUUID()));
    }

    @Test
    void pushPlaylistUpdates_groupsByBaseUrl() {
        UpdateBoxMonitorsAdRequestDto first = new UpdateBoxMonitorsAdRequestDto();
        first.setBaseUrl("http://10.0.0.1:8081/");
        UpdateBoxMonitorsAdRequestDto second = new UpdateBoxMonitorsAdRequestDto();
        second.setBaseUrl("http://10.0.0.2:8081/");

        Set<String> result = boxPlaylistClient.pushPlaylistUpdates(List.of(first, second));

        assertEquals(Set.of("http://10.0.0.1:8081/", "http://10.0.0.2:8081/"), result);
    }

    @Test
    void pushPlaylistUpdates_skipsBlankBaseUrl() {
        UpdateBoxMonitorsAdRequestDto invalid = new UpdateBoxMonitorsAdRequestDto();
        invalid.setBaseUrl("");

        Set<String> result = boxPlaylistClient.pushPlaylistUpdates(List.of(invalid));

        assertTrue(result.isEmpty());
        verifyNoInteractions(httpClient);
    }

    @Test
    void pushPlaylistUpdates_postsGroupedItemsWithPlayerSettings() {
        UpdateBoxMonitorsAdRequestDto dto = new UpdateBoxMonitorsAdRequestDto();
        dto.setBaseUrl("http://10.0.0.1:8081/");

        boxPlaylistClient.pushPlaylistUpdates(List.of(dto));

        ArgumentCaptor<BoxPlaylistPushRequestDto> bodyCaptor = ArgumentCaptor.forClass(BoxPlaylistPushRequestDto.class);
        verify(httpClient).makePostRequest(
                eq("http://10.0.0.1:8081/update-ads"),
                bodyCaptor.capture(),
                eq(Void.class),
                eq(null),
                any());
        assertEquals(1, bodyCaptor.getValue().getAds().size());
        assertEquals(PLAYER_SETTINGS, bodyCaptor.getValue().getPlayerSettings());
    }
}
