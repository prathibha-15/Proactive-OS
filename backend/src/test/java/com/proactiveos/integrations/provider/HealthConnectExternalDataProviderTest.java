package com.proactiveos.integrations.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.proactiveos.events.entity.LifeEventType;
import com.proactiveos.integrations.entity.ExternalProviderId;
import org.junit.jupiter.api.Test;

class HealthConnectExternalDataProviderTest {

    private final HealthConnectExternalDataProvider provider = new HealthConnectExternalDataProvider();

    @Test
    void identifiesAsUploadOnlyHealthConnectForStepsAndSleep() {
        assertThat(provider.providerId()).isEqualTo(ExternalProviderId.HEALTH_CONNECT);
        assertThat(provider.developmentOnly()).isFalse();
        assertThat(provider.clientUploadRequired()).isTrue();
        assertThat(provider.supportedEventTypes()).containsExactlyInAnyOrder(LifeEventType.STEPS, LifeEventType.SLEEP);
        assertThat(provider.displayName()).contains("Android companion required");
    }

    @Test
    void refusesServerSideFetchingBecauseHealthConnectIsClientUploaded() {
        assertThatThrownBy(() -> provider.fetchRecords(12L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("uploaded by the Android companion");
    }
}