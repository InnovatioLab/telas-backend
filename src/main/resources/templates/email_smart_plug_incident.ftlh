<#import "email_monitoring_layout.ftlh" as mon>
<@mon.monitoringEmail title="Smart plug alert" emphasis="critical">
    <tr>
        <td style="padding: 14px 0; border-bottom: 1px solid #eceff1;">
            <p class="field-label">Monitor / target</p>
            <p class="field-value">${monitorAddress!""}</p>
        </td>
    </tr>
    <tr>
        <td style="padding: 14px 0; border-bottom: 1px solid #eceff1;">
            <p class="field-label">Box IP</p>
            <p class="field-value">${boxIp!""}</p>
        </td>
    </tr>
    <tr>
        <td style="padding: 14px 0; border-bottom: 1px solid #eceff1;">
            <p class="field-label">Incident</p>
            <p class="field-value">${incidentType!""}<#if (severity!"")?has_content> (severity ${severity!""})</#if></p>
        </td>
    </tr>
    <tr>
        <td style="padding: 14px 0; border-bottom: 1px solid #eceff1;">
            <p class="field-label">Relay / power</p>
            <p class="field-value">Relay on: ${relayOn!""} — Power (W): ${powerWatts!""}</p>
        </td>
    </tr>
    <tr>
        <td style="padding: 14px 0; border-bottom: 1px solid #eceff1;">
            <p class="field-label">Details</p>
            <p class="field-value">${hypothesis!""}</p>
        </td>
    </tr>
    <tr>
        <td style="padding: 14px 0;">
            <p class="field-label">Notification time</p>
            <p class="field-value">${notifiedAt!""}</p>
        </td>
    </tr>
</@mon.monitoringEmail>
