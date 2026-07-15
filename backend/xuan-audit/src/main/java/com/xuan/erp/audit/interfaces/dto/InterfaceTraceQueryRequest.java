package com.xuan.erp.audit.interfaces.dto;

import com.xuan.erp.audit.application.query.InterfaceTraceQuery;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public record InterfaceTraceQueryRequest(
        String serviceName,
        String endpointName,
        String startTime,
        String endTime,
        Integer limit
) {

    private static final DateTimeFormatter INPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter SKYWALKING_MINUTE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HHmm");
    private static final ZoneId FRONTEND_ZONE = ZoneId.of("Asia/Shanghai");
    private static final ZoneId SKYWALKING_QUERY_ZONE = ZoneId.of("UTC");
    private static final List<DateTimeFormatter> SUPPORTED_INPUT_FORMATTERS = List.of(
            INPUT_FORMATTER,
            SKYWALKING_MINUTE_FORMATTER
    );

    public InterfaceTraceQuery toQuery() {
        LocalDateTime now = LocalDateTime.now();
        String normalizedEnd = endTime == null || endTime.isBlank()
                ? formatForSkyWalking(now)
                : formatForSkyWalking(endTime);
        String normalizedStart = startTime == null || startTime.isBlank()
                ? formatForSkyWalking(now.minusHours(1))
                : formatForSkyWalking(startTime);
        return new InterfaceTraceQuery(
                serviceName,
                endpointName,
                normalizedStart,
                normalizedEnd,
                limit == null || limit <= 0 ? 20 : limit
        );
    }

    private String formatForSkyWalking(String value) {
        for (DateTimeFormatter formatter : SUPPORTED_INPUT_FORMATTERS) {
            try {
                return formatForSkyWalking(LocalDateTime.parse(value, formatter));
            } catch (DateTimeParseException ignored) {
                // Try the next supported frontend/OAP datetime format.
            }
        }
        return value;
    }

    private String formatForSkyWalking(LocalDateTime value) {
        return SKYWALKING_MINUTE_FORMATTER.format(value
                .atZone(FRONTEND_ZONE)
                .withZoneSameInstant(SKYWALKING_QUERY_ZONE));
    }
}
