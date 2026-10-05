package com.proactiveos.integrations.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.proactiveos.events.entity.LifeEventType;
import com.proactiveos.integrations.entity.ExternalProviderId;
import org.junit.jupiter.api.Test;

class MockExternalDataProviderTest {

    private final MockExternalDataProvider provider = new MockExternalDataProvider();

    @Test
    void returnsStableDevelopmentRecordsAndSupportedTypes() {
        var first = provider.fetchRecords(1L);
        var second = provider.fetchRecords(99L);

        assertThat(provider.providerId()).isEqualTo(ExternalProviderId.MOCK);
        assertThat(provider.developmentOnly()).isTrue();
        assertThat(first).hasSize(5).isEqualTo(second);
        assertThat(provider.supportedEventTypes()).containsExactlyInAnyOrder(
                LifeEventType.STEPS, LifeEventType.WORKOUT, LifeEventType.SLEEP);
        assertThat(first).extracting("externalRecordId").doesNotHaveDuplicates();
        assertThat(first).filteredOn(record -> record.type() == LifeEventType.STEPS)
                .singleElement()
                .satisfies(record -> assertThat(record.count()).isEqualTo(8247));
        assertThat(first).filteredOn(record -> record.type() == LifeEventType.WORKOUT)
                .allSatisfy(record -> {
                    assertThat(record.activityType()).isEqualTo("Leg workout");
                    assertThat(record.durationMinutes()).isEqualTo(40);
                });
        assertThat(first).filteredOn(record -> record.type() == LifeEventType.SLEEP)
                .singleElement()
                .satisfies(record -> assertThat(record.durationMinutes()).isEqualTo(480));
    }
}